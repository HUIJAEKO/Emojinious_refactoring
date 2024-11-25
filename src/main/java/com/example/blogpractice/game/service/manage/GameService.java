package com.example.blogpractice.game.service.manage;

import com.example.blogpractice.game.dto.GameSettingDto;
import com.example.blogpractice.game.dto.GameStateDto;
import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.model.GameSettings;
import com.example.blogpractice.game.service.phase.PhaseService;
import com.example.blogpractice.game.service.score.ScoreCalculator;
import com.example.blogpractice.websocket.message.ChatMessage;
import com.example.blogpractice.player.domain.Player;
import com.example.blogpractice.player.dto.PlayerDto;
import com.example.blogpractice.player.service.PlayerService;
import com.example.blogpractice.websocket.util.MessageUtil;
import com.example.blogpractice.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameService {
    private final PlayerService playerService;
    private final RedisUtil redisUtil;
    private final MessageUtil messageUtil;
    private final Map<String, Set<String>> activeConnections = new ConcurrentHashMap<>();
    private static final String GAME_SESSION_KEY = "game:session:";
    private final PhaseService phaseService;
    private final GameSessionManager gameSessionManager;
    private final ScoreCalculator scoreCalculator;

    public void handleExistingConnection(String sessionId, String playerId) {
        Set<String> sessionPlayers = activeConnections.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet());
        if (sessionPlayers.contains(playerId)) {
            sessionPlayers.remove(playerId);
        }
    }

    public boolean isPlayerAlreadyJoined(String sessionId, String playerId) {
        GameSession gameSession = getGameSession(sessionId);
        System.out.println(gameSession != null && gameSession.getPlayerById(playerId) != null);
        return gameSession != null && gameSession.getPlayerById(playerId) != null;
    }

    public GameStateDto joinGame(String sessionId, String playerId, String nickname) {
        GameSession gameSession = getOrCreateGameSession(sessionId);
        Player player = playerService.getPlayerById(playerId);
        gameSession.addPlayer(player);
        updateGameSession(gameSession);
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
        return gameSessionManager.createGameStateDto(gameSession);
    }

    public GameStateDto getGameState(String sessionId) {
        GameSession gameSession = getGameSession(sessionId);
        return gameSessionManager.createGameStateDto(gameSession);
    }

    public void updateGameSession(GameSession gameSession) {
        redisUtil.set(GAME_SESSION_KEY + gameSession.getSessionId(), gameSession);
    }

    public GameSession getGameSession(String sessionId) {
        return redisUtil.get(GAME_SESSION_KEY + sessionId, GameSession.class);
    }

    private GameSession getOrCreateGameSession(String sessionId) {
        GameSession gameSession = redisUtil.get(GAME_SESSION_KEY + sessionId, GameSession.class);
        if (gameSession == null) {
            gameSession = new GameSession(sessionId);
            updateGameSession(gameSession);
        }
        return gameSession;
    }

    public void broadcastChatMessage(String sessionId, ChatMessage message) {
        messageUtil.broadcastChatMessage(sessionId, message);
    }

    public GameStateDto startGame(String sessionId, String playerId) {
        GameSession gameSession = getGameSession(sessionId);
        if (!gameSession.isHost(playerId)) {
            throw new IllegalStateException("Only host can start the game");
        }
        gameSession.startGame();
        updateGameSession(gameSession);
        phaseService.startLoadingPhase(gameSession);
        return gameSessionManager.createGameStateDto(gameSession);
    }

    public GameStateDto submitPrompt(String sessionId, String playerId, String message) {
        System.out.println("GameService.submitPrompt");
        GameSession gameSession = getGameSession(sessionId);
        gameSession.submitPrompt(playerId, message);
        gameSessionManager.updateSubmissionProgress(gameSession, "prompt");
        updateGameSession(gameSession);
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
        System.out.println("gameSession = " + gameSession);
        return gameSessionManager.createGameStateDto(gameSession);
    }


    public GameStateDto submitGuess(String sessionId, String playerId, String guess) {
        GameSession gameSession = getGameSession(sessionId);
        gameSession.submitGuess(playerId, guess);
        gameSessionManager.updateSubmissionProgress(gameSession, "guess");
        CompletableFuture.runAsync(() -> calculateAndSaveScore(gameSession, playerId, guess));

        updateGameSession(gameSession);
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
        return gameSessionManager.createGameStateDto(gameSession);
    }

    private void calculateAndSaveScore(GameSession gameSession, String playerId, String guess) {
        String targetPlayerId = gameSession.getGuessTargetForPlayer(playerId);
        String targetKeyword = gameSession.getCurrentKeywords().get(targetPlayerId);
        float score = scoreCalculator.calculateSingleGuessScore(guess, targetKeyword);
        gameSession.addScore(playerId, targetPlayerId, score);
        updateGameSession(gameSession);
    }
}
