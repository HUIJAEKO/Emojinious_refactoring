package com.example.blogpractice.game.model;

import com.example.blogpractice.game.constant.GamePhase;
import com.example.blogpractice.game.constant.GameState;
import com.example.blogpractice.player.domain.Player;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serializable;
import java.util.*;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameSession implements Serializable {
    private String sessionId;
    private List<Player> players;
    private GameSettings settings;
    private GameState state;
    private int currentTurn;
    private GamePhase currentPhase;
    private Map<String, String> currentPrompts;

    private Map<String, Map<String, Float>> playerScores; // <나 <상대방, 내가 얻은 점수>>

    private List<Map<String, String>> currentGuesses;
    private Map<String, String> currentKeywords;
    private Map<String, String> generatedImages;
    private long phaseStartTime;
    private long phaseEndTime;
    private int currentGuessRound;
    private Map<String, Set<String>> guessedPlayers;
    private long currentRoundStartTime;
    private int currentRoundSubmittedGuesses;

    public GameSession(String sessionId) {
        this.sessionId = sessionId;
        this.players = new ArrayList<>();
        this.settings = new GameSettings();
        this.state = GameState.WAITING;
        this.currentTurn = 0;
        this.currentPhase = GamePhase.WAITING;
        this.currentPrompts = new HashMap<>();
        this.playerScores = new HashMap<>();
        this.currentGuesses = new ArrayList<>();
        this.currentKeywords = new HashMap<>();
        this.generatedImages = new HashMap<>();
        this.currentGuessRound = 0;
        this.guessedPlayers = new HashMap<>();
        this.currentRoundStartTime = 0;
    }

    public void addPlayer(Player player) {
        if (players.size() >= 5) {
            throw new IllegalStateException("Maximum number of players reached");
        }
        players.add(player);
    }

    public Player getPlayerById(String playerId) {
        return players.stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public long getRemainingTime() {
        if (currentPhase == GamePhase.GUESSING && currentGuessRound > 0) {
            long totalGuessingTime = settings.getGuessTimeLimit() * 1000L;
            long elapsedTimeInCurrentRound = System.currentTimeMillis() - currentRoundStartTime;

            return Math.max(0, totalGuessingTime - elapsedTimeInCurrentRound);
        } else {
            return Math.max(0, phaseEndTime - System.currentTimeMillis());
        }
    }
}