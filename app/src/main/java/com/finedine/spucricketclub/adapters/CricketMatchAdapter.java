package com.finedine.spucricketclub.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finedine.spucricketclub.R;
import com.finedine.spucricketclub.cricket.Innings;
import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying cricket match data in a RecyclerView
 */
public class CricketMatchAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_MATCH_HEADER = 0;
    private static final int TYPE_BATSMAN = 1;
    private static final int TYPE_BOWLER = 2;

    private final Context context;
    private final List<Object> items = new ArrayList<>();
    private Match match;

    // Listener for item clicks
    public interface OnItemClickListener {
        void onBatsmanClick(Player batsman);

        void onBowlerClick(Player bowler);
    }

    private OnItemClickListener listener;

    public CricketMatchAdapter(Context context) {
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setMatch(Match match) {
        this.match = match;
        refreshData();
    }

    private void refreshData() {
        items.clear();

        if (match == null) {
            notifyDataSetChanged();
            return;
        }

        // Add match header
        items.add(match);

        // Add current batsmen
        Innings currentInnings = match.getCurrentInnings();
        if (currentInnings != null) {
            if (currentInnings.getStriker() != null) {
                items.add(currentInnings.getStriker());
            }
            if (currentInnings.getNonStriker() != null) {
                items.add(currentInnings.getNonStriker());
            }

            // Add current bowler
            if (currentInnings.getCurrentBowler() != null) {
                items.add(currentInnings.getCurrentBowler());
            }
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof Match) {
            return TYPE_MATCH_HEADER;
        } else if (item instanceof Player) {
            Player player = (Player) item;
            // Determine if player is batting or bowling based on which team they're on
            if (match != null && match.getCurrentInnings() != null) {
                if (match.getCurrentInnings().getBattingTeam().findPlayerByName(player.getName()) != null) {
                    return TYPE_BATSMAN;
                } else {
                    return TYPE_BOWLER;
                }
            }
        }
        return TYPE_BATSMAN; // Default
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_MATCH_HEADER:
                View headerView = inflater.inflate(R.layout.cricket_score_panel, parent, false);
                return new MatchHeaderViewHolder(headerView);
            case TYPE_BATSMAN:
                View batsmanView = inflater.inflate(R.layout.item_cricket_batsman, parent, false);
                return new BatsmanViewHolder(batsmanView);
            case TYPE_BOWLER:
                View bowlerView = inflater.inflate(R.layout.item_cricket_bowler, parent, false);
                return new BowlerViewHolder(bowlerView);
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);

        switch (holder.getItemViewType()) {
            case TYPE_MATCH_HEADER:
                bindMatchHeader((MatchHeaderViewHolder) holder, (Match) item);
                break;
            case TYPE_BATSMAN:
                bindBatsman((BatsmanViewHolder) holder, (Player) item);
                break;
            case TYPE_BOWLER:
                bindBowler((BowlerViewHolder) holder, (Player) item);
                break;
        }
    }

    private void bindMatchHeader(MatchHeaderViewHolder holder, Match match) {
        Innings innings = match.getCurrentInnings();
        if (innings == null) return;

        // Bind match summary data
        holder.textTeamBatting.setText(match.getTeamBatting().getTeamName());
        holder.textTeamBowling.setText(match.getTeamBowling().getTeamName());
        holder.textScore.setText(innings.getTotalScore() + "/" + innings.getWickets());

        // Format overs display
        int oversCompleted = (int) innings.getTotalOvers();
        int ballsInCurrentOver = Math.round((innings.getTotalOvers() - oversCompleted) * 10);
        holder.textOvers.setText(String.format("(%d.%d ov)", oversCompleted, ballsInCurrentOver));

        // Set target if applicable
        if (match.getTargetScore() > 0) {
            holder.textTarget.setText(String.format(
                    context.getString(R.string.target_label), match.getTargetScore()));
            holder.textTarget.setVisibility(View.VISIBLE);
        } else {
            holder.textTarget.setVisibility(View.GONE);
        }
    }

    private void bindBatsman(BatsmanViewHolder holder, Player batsman) {
        holder.textBatsmanName.setText(batsman.getName());
        holder.textBatsmanScore.setText(String.format(
                context.getString(R.string.batsman_score_format),
                batsman.getBattingStats().getRuns(),
                batsman.getBattingStats().getBallsFaced()));

        float strikeRate = batsman.calculateStrikeRate();
        holder.textBatsmanSR.setText(String.format(
                context.getString(R.string.batsman_sr_format), strikeRate));

        // Highlight current striker if applicable
        if (match != null && match.getCurrentInnings() != null &&
                batsman == match.getCurrentInnings().getStriker()) {
            holder.itemView.setBackgroundResource(R.color.spu_light_blue);
        } else {
            holder.itemView.setBackgroundResource(android.R.color.transparent);
        }

        // Set click listener
        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onBatsmanClick(batsman));
        }
    }

    private void bindBowler(BowlerViewHolder holder, Player bowler) {
        holder.textBowlerName.setText(bowler.getName());
        holder.textBowlerFigures.setText(String.format(
                context.getString(R.string.bowler_figures_format),
                bowler.getBowlingStats().getWickets(),
                bowler.getBowlingStats().getRunsConceded(),
                bowler.getBowlingStats().getOvers(),
                bowler.getBowlingStats().getBallsInCurrentOver()));

        // Set click listener
        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onBowlerClick(bowler));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * ViewHolder for match header
     */
    static class MatchHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView textTeamBatting;
        TextView textTeamBowling;
        TextView textScore;
        TextView textOvers;
        TextView textTarget;

        public MatchHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            textTeamBatting = itemView.findViewById(R.id.text_team_batting);
            textTeamBowling = itemView.findViewById(R.id.text_team_bowling);
            textScore = itemView.findViewById(R.id.text_score);
            textOvers = itemView.findViewById(R.id.text_overs);
            textTarget = itemView.findViewById(R.id.text_target);
        }
    }

    /**
     * ViewHolder for batsman items
     */
    static class BatsmanViewHolder extends RecyclerView.ViewHolder {
        TextView textBatsmanName;
        TextView textBatsmanScore;
        TextView textBatsmanSR;

        public BatsmanViewHolder(@NonNull View itemView) {
            super(itemView);
            textBatsmanName = itemView.findViewById(R.id.text_batsman_name);
            textBatsmanScore = itemView.findViewById(R.id.text_batsman_score);
            textBatsmanSR = itemView.findViewById(R.id.text_batsman_sr);
        }
    }

    /**
     * ViewHolder for bowler items
     */
    static class BowlerViewHolder extends RecyclerView.ViewHolder {
        TextView textBowlerName;
        TextView textBowlerFigures;

        public BowlerViewHolder(@NonNull View itemView) {
            super(itemView);
            textBowlerName = itemView.findViewById(R.id.text_bowler_name);
            textBowlerFigures = itemView.findViewById(R.id.text_bowler_figures);
        }
    }
}