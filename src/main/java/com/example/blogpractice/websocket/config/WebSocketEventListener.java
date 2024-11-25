package com.example.blogpractice.websocket.config;

import com.example.blogpractice.game.service.manage.GameSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
    GameSessionManager gameSessionManager;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null) {
            String sessionId = (String) sessionAttributes.get("sessionId");
            String playerId = (String) sessionAttributes.get("playerId");

            if (sessionId != null && playerId != null) {
                gameSessionManager.handlePlayerConnect(sessionId, playerId);
                System.out.println("Player connected - sessionId: {}, playerId: {}" + sessionId + playerId);
            } else {
                System.out.println("Session attributes are incomplete - sessionId: {}, playerId: {}" + sessionId + playerId);
            }
        } else {
            System.out.println("Session attributes are null for the connected event");
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();

        if (sessionAttributes != null) {
            String sessionId = (String) sessionAttributes.get("sessionId");
            String playerId = (String) sessionAttributes.get("playerId");

            if (sessionId != null && playerId != null) {
                gameSessionManager.handlePlayerDisconnect(sessionId, playerId);
                log.info("Player disconnected - sessionId: {}, playerId: {}", sessionId, playerId);
            } else {
                log.warn("Session attributes are incomplete for disconnect - sessionId: {}, playerId: {}", sessionId, playerId);
            }
        } else {
            log.warn("Session attributes are null for the disconnect event");
        }
    }
}