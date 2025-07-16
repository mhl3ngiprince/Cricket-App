package com.finedine.spucricketclub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.spucricketclub.cricket.Player;

import java.util.List;

/**
 * Adapter for displaying players in a RecyclerView
 */
public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.PlayerViewHolder> {
    private List<Player> players;

    public PlayersAdapter(List<Player> players) {
        this.players = players;
    }

    @NonNull
    @Override
    public PlayerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new PlayerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PlayerViewHolder holder, int position) {
        Player player = players.get(position);
        holder.name.setText(player.getName());

        // Show stats based on player role
        String stats;
        if (player.isBowler()) {
            stats = String.format("Wickets: %d, Economy: %.2f",
                    player.getBowlingStats().getWickets(),
                    player.calculateEconomyRate());
        } else {
            stats = String.format("Runs: %d, SR: %.2f",
                    player.getBattingStats().getRuns(),
                    player.calculateStrikeRate());
        }
        holder.stats.setText(stats);
    }

    @Override
    public int getItemCount() {
        return players.size();
    }

    static class PlayerViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView stats;

        public PlayerViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(android.R.id.text1);
            stats = itemView.findViewById(android.R.id.text2);
        }
    }
}