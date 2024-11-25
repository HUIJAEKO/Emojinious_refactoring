package com.example.blogpractice.game.service.manage;

import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.service.phase.PhaseService;
import com.example.blogpractice.player.domain.Player;
import com.example.blogpractice.player.service.PlayerService;
import com.example.blogpractice.websocket.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameSessionManager {
    private final MessageUtil messageUtil;
    private final PlayerService playerService;
    private final PhaseService phaseService;
    private final GameStateManager gameStateManager;
    private final GameSessionUpdateManager gameSessionUpdateManager;

    public void updateSubmissionProgress(GameSession gameSession, String type) {
        int submitted;
        if (type.equals("prompt")) {
            submitted = gameSession.getCurrentPrompts().size();
        } else { // guess
            submitted = gameSession.getCurrentRoundSubmittedGuesses();
        }
        int total = gameSession.getPlayers().size();
        messageUtil.updateSubmissionProgress(gameSession.getSessionId(), type, submitted, total);
    }

    public void handlePlayerConnect(String sessionId, String playerId) {
        System.out.println("GameService.handlePlayerConnect");
        GameSession gameSession = gameSessionUpdateManager.getGameSession(sessionId);
        Player player = playerService.getPlayerById(playerId);
        if (player != null && !gameSession.getPlayers().contains(player)) {
            gameSession.addPlayer(player);
            gameSessionUpdateManager.updateGameSession(gameSession);
            messageUtil.broadcastGameState(gameSession.getSessionId(), gameStateManager.createGameStateDto(gameSession));
        }
    }

    public void handlePlayerDisconnect(String sessionId, String playerId) {
        GameSession gameSession = gameSessionUpdateManager.getGameSession(sessionId);
        gameSession.removePlayer(playerId);
        gameSessionUpdateManager.updateGameSession(gameSession);
        if (gameSession.getPlayers().isEmpty()) {
            phaseService.endGame(gameSession);
        } else {
            gameSessionUpdateManager.updateGameSession(gameSession);
            messageUtil.broadcastGameState(gameSession.getSessionId(), gameStateManager.createGameStateDto(gameSession));
        }
    }
}
