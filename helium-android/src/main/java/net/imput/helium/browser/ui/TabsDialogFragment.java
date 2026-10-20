package net.imput.helium.browser.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.List;
import net.imput.helium.R;
import net.imput.helium.browser.tab.Tab;
import net.imput.helium.browser.tab.TabManager;

/**
 * TabsDialogFragment: Bottom sheet interface for switching, adding, and closing browser tabs.
 */
public class TabsDialogFragment extends BottomSheetDialogFragment {

    public interface OnTabActionListener {
        void onTabSelected(@NonNull Tab tab);
        void onNewTabRequested(boolean incognito);
        void onAllTabsClosed();
    }

    private TabManager mTabManager;
    private OnTabActionListener mActionListener;
    private TabsAdapter mAdapter;
    private TextView mTvHeader;

    public static TabsDialogFragment newInstance() {
        return new TabsDialogFragment();
    }

    public void setOnTabActionListener(@Nullable OnTabActionListener listener) {
        mActionListener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnTabActionListener) {
            mActionListener = (OnTabActionListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_tabs_switcher, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTabManager = TabManager.getInstance();

        mTvHeader = view.findViewById(R.id.tv_tabs_header);
        ImageView btnNewTab = view.findViewById(R.id.btn_new_tab);
        Button btnNewIncognito = view.findViewById(R.id.btn_new_incognito_tab);
        Button btnCloseAll = view.findViewById(R.id.btn_close_all_tabs);
        RecyclerView rvTabs = view.findViewById(R.id.rv_tabs);

        updateHeader();

        rvTabs.setLayoutManager(new LinearLayoutManager(getContext()));
        mAdapter = new TabsAdapter();
        rvTabs.setAdapter(mAdapter);

        btnNewTab.setOnClickListener(v -> {
            if (mActionListener != null) {
                mActionListener.onNewTabRequested(false);
            }
            dismiss();
        });

        btnNewIncognito.setOnClickListener(v -> {
            if (mActionListener != null) {
                mActionListener.onNewTabRequested(true);
            }
            dismiss();
        });

        btnCloseAll.setOnClickListener(v -> {
            mTabManager.closeAllTabs();
            if (mActionListener != null) {
                mActionListener.onAllTabsClosed();
            }
            dismiss();
        });
    }

    private void updateHeader() {
        if (mTvHeader != null && mTabManager != null) {
            mTvHeader.setText("Open Tabs (" + mTabManager.getTabCount() + ")");
        }
    }

    private class TabsAdapter extends RecyclerView.Adapter<TabsAdapter.TabViewHolder> {

        @NonNull
        @Override
        public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab_card, parent, false);
            return new TabViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
            List<Tab> tabs = mTabManager.getTabs();
            if (position < 0 || position >= tabs.size()) return;

            Tab tab = tabs.get(position);
            Tab activeTab = mTabManager.getActiveTab();
            boolean isActive = (activeTab != null && activeTab.getId().equals(tab.getId()));

            holder.tvTitle.setText(tab.getTitle());
            holder.tvUrl.setText(tab.isIncognito() ? "Private Tab" : tab.getUrl());

            if (tab.getFavicon() != null) {
                holder.ivIcon.setImageBitmap(tab.getFavicon());
            } else {
                holder.ivIcon.setImageResource(tab.isIncognito() ? R.drawable.ic_shield_check : R.drawable.ic_helium_logo);
            }

            // Pin indicator (PRD M06)
            holder.ivPin.setVisibility(tab.isPinned() ? View.VISIBLE : View.GONE);

            // Tab Group badge (PRD M06)
            if (tab.getGroupId() != null) {
                net.imput.helium.browser.tab.TabGroup group = mTabManager.getGroupById(tab.getGroupId());
                if (group != null) {
                    holder.tvGroupBadge.setText(group.getName());
                    holder.tvGroupBadge.setVisibility(View.VISIBLE);
                } else {
                    holder.tvGroupBadge.setVisibility(View.GONE);
                }
            } else {
                holder.tvGroupBadge.setVisibility(View.GONE);
            }

            holder.itemView.setSelected(isActive);
            holder.itemView.setAlpha(isActive ? 1.0f : 0.85f);

            holder.itemView.setOnClickListener(v -> {
                mTabManager.selectTab(tab.getId());
                if (mActionListener != null) {
                    mActionListener.onTabSelected(tab);
                }
                dismiss();
            });

            // Long-click to toggle pin status
            holder.itemView.setOnLongClickListener(v -> {
                mTabManager.setTabPinned(tab.getId(), !tab.isPinned());
                notifyItemChanged(position);
                return true;
            });

            holder.btnClose.setOnClickListener(v -> {
                mTabManager.closeTab(tab.getId());
                notifyDataSetChanged();
                updateHeader();
                if (mTabManager.getTabCount() == 0) {
                    if (mActionListener != null) {
                        mActionListener.onAllTabsClosed();
                    }
                    dismiss();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mTabManager.getTabCount();
        }

        class TabViewHolder extends RecyclerView.ViewHolder {
            final ImageView ivIcon;
            final TextView tvTitle;
            final TextView tvUrl;
            final ImageView ivPin;
            final TextView tvGroupBadge;
            final ImageView btnClose;

            TabViewHolder(@NonNull View itemView) {
                super(itemView);
                ivIcon = itemView.findViewById(R.id.iv_tab_icon);
                tvTitle = itemView.findViewById(R.id.tv_tab_title);
                tvUrl = itemView.findViewById(R.id.tv_tab_url);
                ivPin = itemView.findViewById(R.id.iv_pin_indicator);
                tvGroupBadge = itemView.findViewById(R.id.tv_tab_group_badge);
                btnClose = itemView.findViewById(R.id.btn_close_tab);
            }
        }
    }
}
