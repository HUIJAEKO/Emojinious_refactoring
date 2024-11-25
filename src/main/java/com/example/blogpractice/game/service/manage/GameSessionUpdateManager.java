package com.example.blogpractice.game.service.manage;

import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameSessionUpdateManager {

    private final RedisUtil redisUtil;
    private static final String GAME_SESSION_KEY = "game:session:";

    public void updateGameSession(GameSession gameSession) {
        redisUtil.set(GAME_SESSION_KEY + gameSession.getSessionId(), gameSession);
    }

    public GameSession getGameSession(String sessionId) {
        return redisUtil.get(GAME_SESSION_KEY + sessionId, GameSession.class);
    }
}
