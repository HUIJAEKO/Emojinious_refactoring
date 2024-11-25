package com.example.blogpractice.websocket.util;

import com.example.blogpractice.game.constant.GamePhase;
import com.example.blogpractice.game.dto.GameStateDto;
import com.example.blogpractice.game.dto.TurnResultDto;
import com.example.blogpractice.websocket.message.ChatMessage;
import com.example.blogpractice.websocket.message.PlayerMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MessageUtil {
    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastGameState(String sessionId, GameStateDto gameStateDto) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId, gameStateDto);
    }

    public void broadcastChatMessage(String sessionId, ChatMessage message) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId + "/chat", message);
    }

    public void broadcastPhaseStartMessage(String sessionId, GamePhase phase, String message) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId + "/phase",
                Map.of("phase", phase, "message", message));
    }

    public void sendToPlayer(String sessionId, String playerId, String type, Object data) {
        PlayerMessage message = new PlayerMessage(type, data);
        messagingTemplate.convertAndSendToUser(playerId, "/queue/game/" + sessionId, message, createHeaders(playerId));
    }

    public void updateSubmissionProgress(String sessionId, String type, int submitted, int total) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId + "/progress",
                Map.of("type", type, "submitted", submitted, "total", total));
    }

    public void broadcastGameResult(String sessionId, TurnResultDto scores) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId + "/result", scores);
    }

    private MessageHeaders createHeaders(String playerId) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor
                .create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(playerId);
        headerAccessor.setLeaveMutable(true);
        return headerAccessor.getMessageHeaders();
    }
}
