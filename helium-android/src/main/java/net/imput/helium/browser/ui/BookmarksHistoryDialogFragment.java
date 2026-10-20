package net.imput.helium.browser.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.List;
import net.imput.helium.R;
import net.imput.helium.database.BookmarkRecord;
import net.imput.helium.database.HeliumDbHelper;
import net.imput.helium.database.HistoryRecord;

/**
 * BookmarksHistoryDialogFragment: Bottom sheet managing saved bookmarks and browsing history.
 */
public class BookmarksHistoryDialogFragment extends BottomSheetDialogFragment {

    public interface OnUrlSelectedListener {
        void onUrlSelected(@NonNull String url);
    }

    public static final boolean TAB_BOOKMARKS = true;
    public static final boolean TAB_HISTORY = false;

    private HeliumDbHelper mDbHelper;
    private OnUrlSelectedListener mListener;
    private boolean mShowBookmarks = true;

    private TextView mTvTitle;
    private Button mBtnBookmarks;
    private Button mBtnHistory;
    private RecyclerView mRvItems;
    private ItemAdapter mAdapter;

    private final List<Object> mItems = new ArrayList<>();

    public static BookmarksHistoryDialogFragment newInstance(boolean startWithBookmarks) {
        BookmarksHistoryDialogFragment fragment = new BookmarksHistoryDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean("start_bookmarks", startWithBookmarks);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnUrlSelectedListener) {
            mListener = (OnUrlSelectedListener) context;
        }
    }

    public void setOnUrlSelectedListener(@Nullable OnUrlSelectedListener listener) {
        mListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_bookmarks_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getContext() != null) {
            mDbHelper = HeliumDbHelper.getInstance(getContext());
        }

        if (getArguments() != null) {
            mShowBookmarks = getArguments().getBoolean("start_bookmarks", true);
        }

        mTvTitle = view.findViewById(R.id.tv_dialog_title);
        mBtnBookmarks = view.findViewById(R.id.btn_mode_bookmarks);
        mBtnHistory = view.findViewById(R.id.btn_mode_history);
        Button btnClear = view.findViewById(R.id.btn_clear_data);
        mRvItems = view.findViewById(R.id.rv_items);

        mRvItems.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new ItemAdapter();
        mRvItems.setAdapter(mAdapter);

        mBtnBookmarks.setOnClickListener(v -> {
            mShowBookmarks = true;
            updateMode();
        });

        mBtnHistory.setOnClickListener(v -> {
            mShowBookmarks = false;
            updateMode();
        });

        btnClear.setOnClickListener(v -> {
            if (mDbHelper != null) {
                mDbHelper.clearAllBrowsingData(() -> {
                    mItems.clear();
                    mAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "Browsing data cleared", Toast.LENGTH_SHORT).show();
                });
            }
        });

        updateMode();
    }

    private void updateMode() {
        if (mShowBookmarks) {
            mTvTitle.setText("Bookmarks");
            mBtnBookmarks.setTextColor(getResources().getColor(R.color.helium_accent_cyan));
            mBtnHistory.setTextColor(getResources().getColor(R.color.helium_text_secondary));
            loadBookmarks();
        } else {
            mTvTitle.setText("History");
            mBtnHistory.setTextColor(getResources().getColor(R.color.helium_accent_cyan));
            mBtnBookmarks.setTextColor(getResources().getColor(R.color.helium_text_secondary));
            loadHistory();
        }
    }

    private void loadBookmarks() {
        if (mDbHelper == null) return;
        mDbHelper.getAllBookmarks(bookmarks -> {
            mItems.clear();
            mItems.addAll(bookmarks);
            mAdapter.notifyDataSetChanged();
        });
    }

    private void loadHistory() {
        if (mDbHelper == null) return;
        mDbHelper.getRecentHistory(100, history -> {
            mItems.clear();
            mItems.addAll(history);
            mAdapter.notifyDataSetChanged();
        });
    }

    private class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_bookmark, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Object obj = mItems.get(position);

            if (obj instanceof BookmarkRecord) {
                BookmarkRecord b = (BookmarkRecord) obj;
                holder.ivIcon.setImageResource(R.drawable.ic_bookmark);
                holder.tvTitle.setText(b.getTitle().isEmpty() ? b.getUrl() : b.getTitle());
                holder.tvUrl.setText(b.getUrl());

                holder.itemView.setOnClickListener(v -> {
                    if (mListener != null) {
                        mListener.onUrlSelected(b.getUrl());
                    }
                    dismiss();
                });

                holder.btnDelete.setOnClickListener(v -> {
                    mDbHelper.removeBookmark(b.getUrl(), deleted -> {
                        mItems.remove(b);
                        notifyDataSetChanged();
                    });
                });
            } else if (obj instanceof HistoryRecord) {
                HistoryRecord h = (HistoryRecord) obj;
                holder.ivIcon.setImageResource(R.drawable.ic_history);
                holder.tvTitle.setText(h.getTitle().isEmpty() ? h.getUrl() : h.getTitle());
                holder.tvUrl.setText(h.getUrl());

                holder.itemView.setOnClickListener(v -> {
                    if (mListener != null) {
                        mListener.onUrlSelected(h.getUrl());
                    }
                    dismiss();
                });

                holder.btnDelete.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return mItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivIcon;
            final TextView tvTitle;
            final TextView tvUrl;
            final ImageView btnDelete;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_item_icon);
                tvTitle = itemView.findViewById(R.id.tv_item_title);
                tvUrl = itemView.findViewById(R.id.tv_item_url);
                btnDelete = itemView.findViewById(R.id.btn_delete_item);
            }
        }
    }
}
