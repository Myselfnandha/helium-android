package net.imput.helium.browser.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import net.imput.helium.R;
import net.imput.helium.search.SearchSuggestion;

/**
 * SuggestionsAdapter: Renders real-time omnibox suggestions (Bangs, Bookmarks, History, Search).
 */
public class SuggestionsAdapter extends RecyclerView.Adapter<SuggestionsAdapter.ViewHolder> {

    public interface OnSuggestionClickListener {
        void onSuggestionClick(@NonNull SearchSuggestion suggestion);
    }

    private final List<SearchSuggestion> mItems = new ArrayList<>();
    private final OnSuggestionClickListener mListener;

    public SuggestionsAdapter(@NonNull OnSuggestionClickListener listener) {
        mListener = listener;
    }

    public void setSuggestions(@NonNull List<SearchSuggestion> suggestions) {
        mItems.clear();
        mItems.addAll(suggestions);
        notifyDataSetChanged();
    }

    public void clear() {
        mItems.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_omnibox_suggestion, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchSuggestion s = mItems.get(position);
        holder.tvTitle.setText(s.getTitle());
        holder.tvExtra.setText(s.getExtra());

        switch (s.getType()) {
            case BANG:
                holder.ivIcon.setImageResource(R.drawable.ic_extension_puzzle);
                break;
            case BOOKMARK:
                holder.ivIcon.setImageResource(R.drawable.ic_bookmark);
                break;
            case HISTORY:
                holder.ivIcon.setImageResource(R.drawable.ic_history);
                break;
            case SEARCH:
            default:
                holder.ivIcon.setImageResource(R.drawable.ic_search);
                break;
        }

        holder.itemView.setOnClickListener(v -> mListener.onSuggestionClick(s));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvExtra;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_suggestion_icon);
            tvTitle = itemView.findViewById(R.id.tv_suggestion_title);
            tvExtra = itemView.findViewById(R.id.tv_suggestion_extra);
        }
    }
}
