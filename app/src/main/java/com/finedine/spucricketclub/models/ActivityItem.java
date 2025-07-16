package com.finedine.spucricketclub.models;

/**
 * Model class for recent activity items displayed in the dashboard
 */
public class ActivityItem {
    private int iconResId;
    private String title;
    private String description;
    private String timeAgo;

    public ActivityItem(int iconResId, String title, String description, String timeAgo) {
        this.iconResId = iconResId;
        this.title = title;
        this.description = description;
        this.timeAgo = timeAgo;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimeAgo() {
        return timeAgo;
    }

    public void setTimeAgo(String timeAgo) {
        this.timeAgo = timeAgo;
    }
}