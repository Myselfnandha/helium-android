package net.imput.helium.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import net.imput.helium.R;
import net.imput.helium.adblock.AdBlockEngine;
import net.imput.helium.browser.HeliumMainActivity;
import net.imput.helium.browser.tab.Tab;
import net.imput.helium.browser.tab.TabManager;
import net.imput.helium.search.SearchSuggestion;

/**
 * QuickSearchOverlayActivity: Transparent overlay host displaying an instant focused
 * search input card and a centered, draggable and resizable floating mini-browser.
 */
public class QuickSearchOverlayActivity extends AppCompatActivity {

    private FrameLayout mFlOverlayRoot;

    // Compact Search Card Views
    private LinearLayout mCardCompactSearch;
    private EditText mEtSearch;
    private ImageView mBtnClearSearch;
    private ImageView mBtnVoiceSearch;
    private View mDividerSuggestions;
    private RecyclerView mRvSuggestions;

    // Floating Mini-Browser Views
    private LinearLayout mCardMiniBrowser;
    private LinearLayout mLayoutDragHeader;
    private ImageView mBtnMiniBack;
    private ImageView mBtnMiniReload;
    private TextView mTvMiniOmnibox;
    private ImageView mBtnMiniExpand;
    private ImageView mBtnMiniMinimize;
    private ImageView mBtnMiniClose;
    private ProgressBar mProgressMiniBrowser;
    private FrameLayout mContainerMiniWebView;
    private ImageView mBtnResizeHandle;

    private QuickSearchController mSearchController;
    private SuggestionsAdapter mSuggestionsAdapter;
    private AdBlockEngine mAdBlockEngine;

    private Tab mActiveTab;
    private WebView mWebView;
    private String mCurrentUrl;
    private String mLastQuery;

    // Drag and Resize State
    private float mDragStartX, mDragStartY;
    private float mDragStartTransX, mDragStartTransY;
    private float mResizeStartX, mResizeStartY;
    private int mResizeStartWidth, mResizeStartHeight;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_search_overlay);

        mSearchController = new QuickSearchController(this);
        mAdBlockEngine = AdBlockEngine.getInstance();

        initViews();
        setupOutsideTapDismissal();
        setupSearchInput();
        setupMiniBrowserControls();
        setupDragAndResize();

        // Restore unfinished draft if available
        String draft = SearchHistoryStore.getDraft(this);
        if (draft != null && !draft.isEmpty()) {
            mEtSearch.setText(draft);
            mEtSearch.setSelection(draft.length());
        }

        // Auto-focus search input and request keyboard
        mEtSearch.requestFocus();
        showKeyboard();
    }

    private void initViews() {
        mFlOverlayRoot = findViewById(R.id.fl_overlay_root);

        mCardCompactSearch = findViewById(R.id.card_compact_search);
        mEtSearch = findViewById(R.id.et_quick_search);
        mBtnClearSearch = findViewById(R.id.btn_clear_search);
        mBtnVoiceSearch = findViewById(R.id.btn_voice_search);
        mDividerSuggestions = findViewById(R.id.divider_suggestions);
        mRvSuggestions = findViewById(R.id.rv_search_suggestions);

        mCardMiniBrowser = findViewById(R.id.card_mini_browser);
        mLayoutDragHeader = findViewById(R.id.layout_drag_header);
        mBtnMiniBack = findViewById(R.id.btn_mini_back);
        mBtnMiniReload = findViewById(R.id.btn_mini_reload);
        mTvMiniOmnibox = findViewById(R.id.tv_mini_omnibox);
        mBtnMiniExpand = findViewById(R.id.btn_mini_expand);
        mBtnMiniMinimize = findViewById(R.id.btn_mini_minimize);
        mBtnMiniClose = findViewById(R.id.btn_mini_close);
        mProgressMiniBrowser = findViewById(R.id.progress_mini_browser);
        mContainerMiniWebView = findViewById(R.id.container_mini_webview);
        mBtnResizeHandle = findViewById(R.id.btn_resize_handle);

        mRvSuggestions.setLayoutManager(new LinearLayoutManager(this));
        mSuggestionsAdapter = new SuggestionsAdapter();
        mRvSuggestions.setAdapter(mSuggestionsAdapter);
    }

    private void setupOutsideTapDismissal() {
        mFlOverlayRoot.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                View activeCard = (mCardMiniBrowser.getVisibility() == View.VISIBLE)
                        ? mCardMiniBrowser : mCardCompactSearch;

                Rect rect = new Rect();
                activeCard.getGlobalVisibleRect(rect);
                if (!rect.contains((int) event.getX(), (int) event.getY())) {
                    saveDraftAndDismiss();
                    return true; // Consumes the first outside tap
                }
            }
            return false;
        });
    }

    private void setupSearchInput() {
        mBtnClearSearch.setOnClickListener(v -> mEtSearch.setText(""));

        mEtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = (s != null) ? s.toString() : "";
                mBtnClearSearch.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);

                mSearchController.onQueryChanged(text, (recentSearches, liveSuggestions, gen) -> {
                    runOnUiThread(() -> {
                        mSuggestionsAdapter.setData(recentSearches, liveSuggestions);
                        boolean hasItems = (recentSearches.size() + liveSuggestions.size()) > 0;
                        mDividerSuggestions.setVisibility(hasItems ? View.VISIBLE : View.GONE);
                        mRvSuggestions.setVisibility(hasItems ? View.VISIBLE : View.GONE);
                    });
                });
            }
        });

        mEtSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String text = mEtSearch.getText().toString().trim();
                if (!text.isEmpty()) {
                    openMiniBrowser(text);
                    return true;
                }
            }
            return false;
        });
    }

    private void openMiniBrowser(@NonNull String input) {
        mLastQuery = input;
        hideKeyboard();

        String resolvedUrl = QuickSearchController.resolveSubmissionUrl(this, input);
        mCurrentUrl = resolvedUrl;

        // Add to submitted search history & clear unfinished draft
        SearchHistoryStore.addQuery(this, input);
        SearchHistoryStore.clearDraft(this);

        // Transition from compact search to floating mini-browser
        mCardCompactSearch.setVisibility(View.GONE);
        mCardMiniBrowser.setVisibility(View.VISIBLE);
        mTvMiniOmnibox.setText(resolvedUrl);

        setupFloatingWebView(resolvedUrl);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupFloatingWebView(@NonNull String url) {
        if (mWebView != null) {
            mContainerMiniWebView.removeAllViews();
            mWebView.destroy();
            mWebView = null;
        }

        // Reuse native browser TabManager to manage tab lifecycle
        mActiveTab = TabManager.getInstance().createTab(url, false);

        mWebView = new WebView(this);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        String ua = settings.getUserAgentString();
        settings.setUserAgentString(ua + " Helium/1.0.0 (Android; MiniBrowser)");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String pageUrl, Bitmap favicon) {
                super.onPageStarted(view, pageUrl, favicon);
                mProgressMiniBrowser.setVisibility(View.VISIBLE);
                mCurrentUrl = pageUrl;
                mTvMiniOmnibox.setText(pageUrl);
            }

            @Override
            public void onPageFinished(WebView view, String pageUrl) {
                super.onPageFinished(view, pageUrl);
                mProgressMiniBrowser.setVisibility(View.GONE);
                mCurrentUrl = pageUrl;
                mTvMiniOmnibox.setText(pageUrl);
                SearchHistoryStore.saveTabRecovery(QuickSearchOverlayActivity.this, pageUrl, mLastQuery, mActiveTab.getId());
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    if (mAdBlockEngine.shouldBlock(request.getUrl().toString())) {
                        return AdBlockEngine.createEmptyBlockResponse();
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    mProgressMiniBrowser.setVisibility(View.VISIBLE);
                    mProgressMiniBrowser.setProgress(newProgress);
                } else {
                    mProgressMiniBrowser.setVisibility(View.GONE);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (title != null && !title.isEmpty()) {
                    mTvMiniOmnibox.setText(title);
                }
            }
        });

        mActiveTab.setWebView(mWebView);
        mContainerMiniWebView.addView(mWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        mWebView.loadUrl(url);
    }

    private void setupMiniBrowserControls() {
        mBtnMiniBack.setOnClickListener(v -> {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
            } else {
                minimizeToSearch();
            }
        });

        mBtnMiniReload.setOnClickListener(v -> {
            if (mWebView != null) mWebView.reload();
        });

        mBtnMiniExpand.setOnClickListener(v -> promoteToFullBrowser());
        mBtnMiniMinimize.setOnClickListener(v -> minimizeToSearch());
        mBtnMiniClose.setOnClickListener(v -> saveDraftAndDismiss());
    }

    /**
     * Promotes the existing Tab directly into HeliumMainActivity without creating duplicates.
     */
    private void promoteToFullBrowser() {
        if (mActiveTab != null && mWebView != null) {
            // Detach WebView so it can be re-attached in HeliumMainActivity
            mContainerMiniWebView.removeAllViews();

            Intent intent = new Intent(this, HeliumMainActivity.class);
            intent.putExtra("extra_tab_id", mActiveTab.getId());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        }
        finish();
    }

    private void minimizeToSearch() {
        mCardMiniBrowser.setVisibility(View.GONE);
        mCardCompactSearch.setVisibility(View.VISIBLE);
        mEtSearch.requestFocus();
        showKeyboard();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDragAndResize() {
        // Draggable top handle
        mLayoutDragHeader.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mDragStartX = event.getRawX();
                    mDragStartY = event.getRawY();
                    mDragStartTransX = mCardMiniBrowser.getTranslationX();
                    mDragStartTransY = mCardMiniBrowser.getTranslationY();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float deltaX = event.getRawX() - mDragStartX;
                    float deltaY = event.getRawY() - mDragStartY;
                    mCardMiniBrowser.setTranslationX(mDragStartTransX + deltaX);
                    mCardMiniBrowser.setTranslationY(mDragStartTransY + deltaY);
                    return true;
            }
            return false;
        });

        // Resizable corner handle
        mBtnResizeHandle.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mResizeStartX = event.getRawX();
                    mResizeStartY = event.getRawY();
                    mResizeStartWidth = mCardMiniBrowser.getWidth();
                    mResizeStartHeight = mCardMiniBrowser.getHeight();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int newWidth = (int) (mResizeStartWidth + (event.getRawX() - mResizeStartX));
                    int newHeight = (int) (mResizeStartHeight + (event.getRawY() - mResizeStartY));
                    // Clamp bounds
                    int minW = (int) (260 * getResources().getDisplayMetrics().density);
                    int minH = (int) (320 * getResources().getDisplayMetrics().density);
                    int maxW = getResources().getDisplayMetrics().widthPixels - 32;
                    int maxH = getResources().getDisplayMetrics().heightPixels - 64;

                    newWidth = Math.max(minW, Math.min(maxW, newWidth));
                    newHeight = Math.max(minH, Math.min(maxH, newHeight));

                    ViewGroup.LayoutParams lp = mCardMiniBrowser.getLayoutParams();
                    lp.width = newWidth;
                    lp.height = newHeight;
                    mCardMiniBrowser.setLayoutParams(lp);
                    return true;
            }
            return false;
        });
    }

    private void saveDraftAndDismiss() {
        hideKeyboard();
        String currentText = mEtSearch.getText().toString().trim();
        if (!currentText.isEmpty() && mCardCompactSearch.getVisibility() == View.VISIBLE) {
            SearchHistoryStore.saveDraft(this, currentText);
        }
        if (mCurrentUrl != null && mActiveTab != null) {
            SearchHistoryStore.saveTabRecovery(this, mCurrentUrl, mLastQuery, mActiveTab.getId());
        }
        finish();
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(mEtSearch, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onBackPressed() {
        if (mCardMiniBrowser.getVisibility() == View.VISIBLE) {
            if (mWebView != null && mWebView.canGoBack()) {
                mWebView.goBack();
            } else {
                minimizeToSearch();
            }
        } else {
            saveDraftAndDismiss();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSearchController.cancel();
        if (mWebView != null && mContainerMiniWebView != null) {
            mContainerMiniWebView.removeAllViews();
            mWebView.destroy();
            mWebView = null;
        }
    }

    // --- Suggestions RecyclerView Adapter ---

    private class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {

        private final List<Object> mItems = new ArrayList<>();

        public void setData(List<String> recents, List<SearchSuggestion> live) {
            mItems.clear();
            mItems.addAll(recents);
            mItems.addAll(live);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_quick_suggestion, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Object item = mItems.get(position);
            if (item instanceof String) {
                // Recent Search Item
                String query = (String) item;
                holder.tvTitle.setText(query);
                holder.tvSubtitle.setVisibility(View.GONE);
                holder.ivIcon.setImageResource(R.drawable.ic_history);
                holder.ivAction.setVisibility(View.VISIBLE);
                holder.ivAction.setOnClickListener(v -> {
                    SearchHistoryStore.removeQuery(QuickSearchOverlayActivity.this, query);
                    mItems.remove(position);
                    notifyItemRemoved(position);
                });
                holder.itemView.setOnClickListener(v -> openMiniBrowser(query));
            } else if (item instanceof SearchSuggestion) {
                // Live Suggestion Item
                SearchSuggestion s = (SearchSuggestion) item;
                holder.tvTitle.setText(s.getTitle());
                holder.tvSubtitle.setVisibility(s.getSubtitle().isEmpty() ? View.GONE : View.VISIBLE);
                holder.tvSubtitle.setText(s.getSubtitle());
                holder.ivAction.setVisibility(View.GONE);

                if (s.getType() == SearchSuggestion.Type.BANG) {
                    holder.ivIcon.setImageResource(R.drawable.ic_search);
                } else {
                    holder.ivIcon.setImageResource(R.drawable.ic_search);
                }
                holder.itemView.setOnClickListener(v -> openMiniBrowser(s.getUrl()));
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivIcon;
            final TextView tvTitle;
            final TextView tvSubtitle;
            final ImageView ivAction;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_suggestion_icon);
                tvTitle = itemView.findViewById(R.id.tv_suggestion_title);
                tvSubtitle = itemView.findViewById(R.id.tv_suggestion_subtitle);
                ivAction = itemView.findViewById(R.id.iv_suggestion_action);
            }
        }
    }
}
