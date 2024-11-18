package com.example.blogpractice.player.controller;

import com.example.blogpractice.player.domain.Player;
import com.example.blogpractice.player.dto.PlayerCreateRequest;
import com.example.blogpractice.player.service.PlayerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {
    private final PlayerService playerService;

    @PostMapping("/host")
    public ResponseEntity<?> createHostPlayer(@Valid @RequestBody PlayerCreateRequest request) {
        String sessionId = UUID.randomUUID().toString();
        Player player = playerService.createPlayer(request.getNickname(), request.getCharacterId(), sessionId, true);
        String inviteLink = playerService.generateInviteLink(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("player", player);
        response.put("inviteLink", inviteLink);
        response.put("token", player.getToken());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/guest/{sessionId}")
    public ResponseEntity<?> createGuestPlayer(@PathVariable String sessionId,
                                               @Valid @RequestBody PlayerCreateRequest request) {
        Player player = playerService.createPlayer(request.getNickname(), request.getCharacterId(), sessionId, false);

        Map<String, Object> response = new HashMap<>();
        response.put("player", player);
        response.put("token", player.getToken());

        return ResponseEntity.ok(response);
    }

}
