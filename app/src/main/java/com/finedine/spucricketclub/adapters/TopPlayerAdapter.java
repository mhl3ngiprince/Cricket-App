package com.finedine.spucricketclub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.spucricketclub.R;
import com.finedine.spucricketclub.models.PlayerStats;

import java.util.List;

/**
 * Adapter for displaying top players in a horizontal RecyclerView
 */
public class TopPlayerAdapter extends RecyclerView.Adapter<TopPlayerAdapter.TopPlayerViewHolder> {

    private List<PlayerStats> players;

    public TopPlayerAdapter(List<PlayerStats> players) {
        this.players = players;
    }

    @NonNull
    @Override
    public TopPlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_top_player, parent, false);
        return new TopPlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TopPlayerViewHolder holder, int position) {
        PlayerStats player = players.get(position);

        holder.playerName.setText(player.getName());
        holder.playerRole.setText(formatRole(player.getRole()));
        holder.playerStat.setText(player.getStatLine());

        if (player.getPlayerImage() != null) {
            holder.playerImage.setImageBitmap(player.getPlayerImage());
        } else {
            // Set default image
            holder.playerImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }
    }

    private String formatRole(String role) {
        // Convert ENUM_STYLE to "Title Case"
        if (role == null || role.isEmpty()) return "";

        // Replace underscores with spaces and title case each word
        String[] words = role.split("_");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(word.charAt(0))
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }

        return formatted.toString().trim();
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class TopPlayerViewHolder extends RecyclerView.ViewHolder {
        ImageView playerImage;
        TextView playerName;
        TextView playerRole;
        TextView playerStat;

        public TopPlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            playerImage = itemView.findViewById(R.id.player_image);
            playerName = itemView.findViewById(R.id.player_name);
            playerRole = itemView.findViewById(R.id.player_role);
            playerStat = itemView.findViewById(R.id.player_stat);
        }
    }
}