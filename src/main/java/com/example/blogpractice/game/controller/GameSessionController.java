package com.example.blogpractice.game.controller;

import com.example.blogpractice.game.dto.GameSettingDto;
import com.example.blogpractice.game.dto.GameStateDto;
import com.example.blogpractice.game.service.manage.GameService;
import com.example.blogpractice.game.service.manage.GameSessionService;
import com.example.blogpractice.security.JwtUtil;
import com.example.blogpractice.websocket.util.MessageUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class GameSessionController {
    private final GameSessionService gameSessionService;
    private final GameService gameService;
    private final JwtUtil jwtUtil;
    private final MessageUtil messageUtil;

    @PutMapping("/{sessionId}/settings")
    public ResponseEntity<?> updateGameSettings(
            @PathVariable String sessionId,
            @RequestBody GameSettingDto settings,
            @RequestHeader("Authorization") String token) {
        System.out.println("update settings");
        Claims claims = jwtUtil.validateToken(token.replace("Bearer ", ""));
        String playerId = claims.getSubject();
        boolean isHost = claims.get("isHost", Boolean.class);
        String tokenSessionId = claims.get("sessionId", String.class);

        if (!isHost || !sessionId.equals(tokenSessionId)) {
            return ResponseEntity.status(403).body("Not authorized to change settings");
        }

        try {
            gameSessionService.updateGameSettings(sessionId, playerId, settings);
            GameStateDto updatedGameState = gameService.getGameState(sessionId);
            messageUtil.broadcastGameState(sessionId, updatedGameState);
            return ResponseEntity.ok().body("Settings updated successfully");

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("An error occurred while updating settings");
        }
    }
}
