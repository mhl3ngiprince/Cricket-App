package com.finedine.spucricketclub;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.finedine.spucricketclub.cricket.CricketMatchService;
import com.finedine.spucricketclub.cricket.Match;
import com.finedine.spucricketclub.cricket.Player;
import com.finedine.spucricketclub.cricket.Team;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing teams and players
 * Allows coach to enter player names for both teams and edit team names
 */
public class TeamManagementActivity extends AppCompatActivity {

    private EditText teamNameEditText;
    private EditText opponentNameEditText;
    private EditText playerNameEditText;
    private Spinner playerRoleSpinner;
    private Spinner teamSelectionSpinner;
    private ListView playersListView;
    private Button addPlayerButton;
    private Button saveTeamNamesButton;
    private TabLayout tabLayout;

    private CricketMatchService cricketMatchService;
    private Match currentMatch;
    private Team homeTeam;
    private Team awayTeam;
    private Team currentTeam;

    private List<String> playerNames = new ArrayList<>();
    private ArrayAdapter<String> playersAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_management);

        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Team Management");

        // Initialize UI components
        initializeUIComponents();

        // Get match service and create/get current match
        cricketMatchService = CricketMatchService.getInstance(this);
        if (cricketMatchService.getCurrentMatch() == null) {
            currentMatch = cricketMatchService.createMatch("SPU", "Opponent", 20);
        } else {
            currentMatch = cricketMatchService.getCurrentMatch();
        }

        // Get teams from match
        homeTeam = currentMatch.getTeamBatting();
        awayTeam = currentMatch.getTeamBowling();
        currentTeam = homeTeam; // Start with home team selected

        // Set initial team names
        teamNameEditText.setText(homeTeam.getTeamName());
        opponentNameEditText.setText(awayTeam.getTeamName());

        // Load players for current team
        loadPlayersForCurrentTeam();

        // Set up listeners
        setupListeners();
    }

    private void initializeUIComponents() {
        teamNameEditText = findViewById(R.id.edit_home_team_name);
        opponentNameEditText = findViewById(R.id.edit_away_team_name);
        playerNameEditText = findViewById(R.id.edit_player_name);
        playerRoleSpinner = findViewById(R.id.spinner_player_role);
        teamSelectionSpinner = findViewById(R.id.spinner_team_selection);
        playersListView = findViewById(R.id.list_players);
        addPlayerButton = findViewById(R.id.button_add_player);
        saveTeamNamesButton = findViewById(R.id.button_save_team_names);
        tabLayout = findViewById(R.id.tab_layout);

        // Set up team selection spinner
        ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new String[]{"Home Team", "Away Team"});
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        teamSelectionSpinner.setAdapter(teamAdapter);

        // Set up player role spinner
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"BATSMAN", "BOWLER", "ALL_ROUNDER", "WICKET_KEEPER"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        playerRoleSpinner.setAdapter(roleAdapter);

        // Set up players list adapter
        playersAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, playerNames);
        playersListView.setAdapter(playersAdapter);
    }

    private void setupListeners() {
        // Team selection spinner listener
        teamSelectionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Switch between home and away teams
                if (position == 0) {
                    currentTeam = homeTeam;
                } else {
                    currentTeam = awayTeam;
                }
                loadPlayersForCurrentTeam();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do
            }
        });

        // Add player button listener
        addPlayerButton.setOnClickListener(v -> addNewPlayer());

        // Save team names button listener
        saveTeamNamesButton.setOnClickListener(v -> saveTeamNames());

        // Players list item click listener for editing/deleting
        playersListView.setOnItemClickListener((parent, view, position, id) -> {
            String playerName = playerNames.get(position);
            showPlayerOptionsDialog(playerName);
        });
    }

    private void loadPlayersForCurrentTeam() {
        // Clear and reload player names
        playerNames.clear();
        for (Player player : currentTeam.getPlayers()) {
            playerNames.add(player.getName());
        }
        playersAdapter.notifyDataSetChanged();
    }

    private void addNewPlayer() {
        String name = playerNameEditText.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter player name", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if player name already exists
        for (Player player : currentTeam.getPlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                Toast.makeText(this, "Player already exists", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Get selected role
        Player.PlayerRole role;
        switch (playerRoleSpinner.getSelectedItemPosition()) {
            case 0:
                role = Player.PlayerRole.BATSMAN;
                break;
            case 1:
                role = Player.PlayerRole.BOWLER;
                break;
            case 2:
                role = Player.PlayerRole.ALL_ROUNDER;
                break;
            case 3:
                role = Player.PlayerRole.WICKET_KEEPER;
                break;
            default:
                role = Player.PlayerRole.BATSMAN;
        }

        // Create and add new player
        Player newPlayer = new Player(name, role);
        currentTeam.addPlayer(newPlayer);

        // Update the UI
        playerNames.add(name);
        playersAdapter.notifyDataSetChanged();

        // Clear the input field
        playerNameEditText.setText("");

        Toast.makeText(this, "Added player: " + name, Toast.LENGTH_SHORT).show();
    }

    private void saveTeamNames() {
        String homeTeamName = teamNameEditText.getText().toString().trim();
        String awayTeamName = opponentNameEditText.getText().toString().trim();

        if (homeTeamName.isEmpty() || awayTeamName.isEmpty()) {
            Toast.makeText(this, "Team names cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Update team names
        homeTeam.setTeamName(homeTeamName);
        awayTeam.setTeamName(awayTeamName);

        Toast.makeText(this, "Team names saved", Toast.LENGTH_SHORT).show();
    }

    private void showPlayerOptionsDialog(String playerName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(playerName);
        builder.setItems(new String[]{"Edit", "Delete"}, (dialog, which) -> {
            if (which == 0) {
                showEditPlayerDialog(playerName);
            } else if (which == 1) {
                deletePlayer(playerName);
            }
        });
        builder.create().show();
    }

    private void showEditPlayerDialog(String playerName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Player");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_player, null);
        builder.setView(dialogView);

        EditText nameEditText = dialogView.findViewById(R.id.edit_dialog_player_name);
        Spinner roleSpinner = dialogView.findViewById(R.id.spinner_dialog_player_role);

        // Set up role spinner
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"BATSMAN", "BOWLER", "ALL_ROUNDER", "WICKET_KEEPER"});
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        // Find the player
        Player player = currentTeam.findPlayerByName(playerName);
        if (player != null) {
            nameEditText.setText(player.getName());
            roleSpinner.setSelection(player.getRole().ordinal());
        }

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newName = nameEditText.getText().toString().trim();
            if (newName.isEmpty()) {
                Toast.makeText(this, "Player name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get selected role
            Player.PlayerRole role;
            switch (roleSpinner.getSelectedItemPosition()) {
                case 0:
                    role = Player.PlayerRole.BATSMAN;
                    break;
                case 1:
                    role = Player.PlayerRole.BOWLER;
                    break;
                case 2:
                    role = Player.PlayerRole.ALL_ROUNDER;
                    break;
                case 3:
                    role = Player.PlayerRole.WICKET_KEEPER;
                    break;
                default:
                    role = Player.PlayerRole.BATSMAN;
            }

            // Update player
            if (player != null) {
                player.setName(newName);
                player.setRole(role);

                // Update the list
                int index = playerNames.indexOf(playerName);
                if (index >= 0) {
                    playerNames.set(index, newName);
                    playersAdapter.notifyDataSetChanged();
                }

                Toast.makeText(this, "Player updated", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private void deletePlayer(String playerName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Player")
                .setMessage("Are you sure you want to delete " + playerName + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Find and remove player
                    Player player = currentTeam.findPlayerByName(playerName);
                    if (player != null) {
                        // Create a new list without the player to remove
                        List<Player> updatedPlayers = new ArrayList<>();
                        for (Player p : currentTeam.getPlayers()) {
                            if (!p.getId().equals(player.getId())) {
                                updatedPlayers.add(p);
                            }
                        }
                        currentTeam.setPlayers(updatedPlayers);

                        // Update the UI
                        playerNames.remove(playerName);
                        playersAdapter.notifyDataSetChanged();

                        Toast.makeText(this, "Player deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create().show();
    }
}