package com.example.blogpractice.game.service.phase;

import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.service.manage.GameSessionManager;
import com.example.blogpractice.websocket.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DescriptionPhaseService {
    private final MessageUtil messageUtil;
    private final GameSessionManager gameSessionManager;
    private final PhaseService phaseService;

    void startDescriptionPhase(GameSession gameSession) {
        System.out.println("PhaseService.startDescriptionPhase");
        gameSession.getPlayers().forEach(player ->
                messageUtil.sendToPlayer(gameSession.getSessionId(), player.getSocketId(), "keyword", gameSession.getCurrentKeywords().get(player.getId())));
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Description Phase");
        phaseService.updateSubmissionProgress(gameSession, "prompt");
    }
}
