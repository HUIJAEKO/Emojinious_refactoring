package com.example.blogpractice.websocket.controller;

import com.example.blogpractice.game.dto.GameStateDto;
import com.example.blogpractice.game.service.manage.GameService;
import com.example.blogpractice.websocket.message.ChatMessage;
import com.example.blogpractice.websocket.message.ConnectMessage;
import com.example.blogpractice.player.domain.Player;
import com.example.blogpractice.player.service.PlayerService;
import com.example.blogpractice.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebsocketController {

    private final GameService gameService;
    private final PlayerService playerService;
    private final JwtUtil jwtUtil;

    @MessageMapping("/connect")
    @SendToUser("/queue/connect-ack")
    public String handleConnect(SimpMessageHeaderAccessor headerAccessor,
                                @Payload ConnectMessage message) {
        System.out.println("WebSocketController.handleConnect");
        String playerId = message.getPlayerId();
        String token = message.getToken();

        try {
            Claims claims = jwtUtil.validateToken(token);
            if (claims.getSubject().equals(playerId)) {
                Player player = playerService.getPlayerById(playerId);

                if (player != null) {
                    gameService.handleExistingConnection(player.getSessionId(), playerId);

                    player.setSocketId(headerAccessor.getSessionId());
                    playerService.savePlayer(player);
                    headerAccessor.getSessionAttributes().put("player", player);
                    headerAccessor.getSessionAttributes().put("playerId", playerId);
                    headerAccessor.getSessionAttributes().put("sessionId", claims.get("sessionId", String.class));
                    headerAccessor.getSessionAttributes().put("nickname", player.getNickname());
                    headerAccessor.getSessionAttributes().put("characterId", player.getCharacterId());
                    headerAccessor.getSessionAttributes().put("isHost", player.isHost());

                    return "Connected successfully";
                } else {
                    return "Player not found";
                }
            }
        } catch (Exception e) {
            return "Invalid token";
        }
        return "Connection failed";
    }

    @MessageMapping("/game/{sessionId}/join")
    @SendTo("/topic/game/{sessionId}")
    public GameStateDto joinGame(@DestinationVariable String sessionId,
                                 SimpMessageHeaderAccessor headerAccessor) {
        System.out.println("WebSocketController.joinGame");
        String playerId = (String) headerAccessor.getSessionAttributes().get("playerId");
        String nickname = (String) headerAccessor.getSessionAttributes().get("nickname");

        if (gameService.isPlayerAlreadyJoined(sessionId, playerId)) {
            return gameService.getGameState(sessionId);
        }

        return gameService.joinGame(sessionId, playerId, nickname);
    }

    @MessageMapping("/game/{sessionId}/chat")
    public void sendChatMessage(@DestinationVariable String sessionId,
                                @Payload ChatMessage message,
                                SimpMessageHeaderAccessor headerAccessor) {
        String playerId = (String) headerAccessor.getSessionAttributes().get("playerId");
        String nickname = (String) headerAccessor.getSessionAttributes().get("nickname");
        message.setPlayerId(playerId);
        message.setSender(nickname);
        gameService.broadcastChatMessage(sessionId, message);
    }
}
