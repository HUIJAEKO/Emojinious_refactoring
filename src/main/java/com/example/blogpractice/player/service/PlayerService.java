package com.example.blogpractice.player.service;

import com.example.blogpractice.player.domain.Player;
import com.example.blogpractice.security.JwtUtil;
import com.example.blogpractice.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerService {

    private final RedisUtil redisUtil;
    private final JwtUtil jwtUtil;

    private static final String PLAYER_KEY = "player";
    private static final String SESSION_PLAYER_KEY = "session:player:";
    private static final long PLAYER_EXPIRATION = 3 * 60 * 60;

    public Player createPlayer(String nickname, int characterId, String sessionId, boolean isHost) {
        String playerId = UUID.randomUUID().toString();
        Player player = new Player(playerId, nickname, characterId, isHost);
        player.setSessionId(sessionId);
        String token = jwtUtil.generateToken(playerId, sessionId, isHost);
        player.setToken(token);
        savePlayer(player);
        return player;
    }

    public void savePlayer(Player player) {
        String playerKey = PLAYER_KEY + player.getId();  // 수정된 부분
        String sessionPlayerKey = SESSION_PLAYER_KEY + player.getSessionId();

        redisUtil.setWithExpiration(playerKey, player, PLAYER_EXPIRATION, TimeUnit.SECONDS);
        redisUtil.setWithExpiration(sessionPlayerKey, player.getId(), PLAYER_EXPIRATION, TimeUnit.SECONDS);
    }

    public Player getPlayerById(String playerId) {
        return redisUtil.get(PLAYER_KEY + playerId, Player.class);
    }

    public String generateInviteLink(String sessionId) {
        return "http://localhost:8080/join?sessionId=" + sessionId;
    }

}
