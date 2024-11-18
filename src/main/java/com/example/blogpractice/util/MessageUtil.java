package com.example.blogpractice.util;

import com.example.blogpractice.game.dto.GameStateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageUtil {
    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastGameState(String sessionId, GameStateDto gameStateDto) {
        messagingTemplate.convertAndSend("/topic/game/" + sessionId, gameStateDto);
    }
}
