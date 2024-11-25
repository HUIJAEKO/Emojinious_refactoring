package com.example.blogpractice.game.service.phase;

import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.service.manage.GameSessionManager;
import com.example.blogpractice.websocket.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CheckingPhaseService {
    private final MessageUtil messageUtil;
    private final GameSessionManager gameSessionManager;

    public void startCheckingPhase(GameSession gameSession){
        System.out.println("GameService.startCheckingPhase");
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Checking Phase");

        Map<String, String> images = gameSession.getGeneratedImages();
        gameSession.getPlayers().forEach(player ->
                messageUtil.sendToPlayer(gameSession.getSessionId(), player.getSocketId(), "image", images.get(player.getId())));
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
    }
}
