package com.example.blogpractice.websocket.util;

import com.example.blogpractice.game.constant.GamePhase;
import com.example.blogpractice.game.dto.GameStateDto;
import com.example.blogpractice.websocket.message.ChatMessage;
import lombok.RequiredArgsConstructor;
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
}
