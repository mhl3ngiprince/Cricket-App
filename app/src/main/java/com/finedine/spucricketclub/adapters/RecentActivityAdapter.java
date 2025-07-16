package com.finedine.spucricketclub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.spucricketclub.R;
import com.finedine.spucricketclub.models.ActivityItem;

import java.util.List;

/**
 * Adapter for displaying recent activity items in the dashboard
 */
public class RecentActivityAdapter extends RecyclerView.Adapter<RecentActivityAdapter.ActivityViewHolder> {

    private List<ActivityItem> activityItems;

    public RecentActivityAdapter(List<ActivityItem> activityItems) {
        this.activityItems = activityItems;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recent_activity, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        ActivityItem item = activityItems.get(position);

        holder.activityTitle.setText(item.getTitle());
        holder.activityDescription.setText(item.getDescription());
        holder.activityTime.setText(item.getTimeAgo());

        try {
            holder.activityIcon.setImageResource(item.getIconResId());
        } catch (Exception e) {
            // Set default icon if resource not found
            holder.activityIcon.setImageResource(android.R.drawable.ic_menu_info_details);
        }
    }

    @Override
    public int getItemCount() {
        return activityItems.size();
    }

    public void updateItems(List<ActivityItem> items) {
        this.activityItems = items;
        notifyDataSetChanged();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        ImageView activityIcon;
        TextView activityTitle;
        TextView activityDescription;
        TextView activityTime;

        public ActivityViewHolder(@NonNull View itemView) {
            super(itemView);
            activityIcon = itemView.findViewById(R.id.activity_icon);
            activityTitle = itemView.findViewById(R.id.activity_title);
            activityDescription = itemView.findViewById(R.id.activity_description);
            activityTime = itemView.findViewById(R.id.activity_time);
        }
    }
}