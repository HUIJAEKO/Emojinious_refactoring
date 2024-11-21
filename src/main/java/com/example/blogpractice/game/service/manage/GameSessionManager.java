package com.example.blogpractice.game.service.manage;

import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameSessionManager {
    private final RedisUtil redisUtil;

    public void updateGameSession(GameSession gameSession) {
        redisUtil.set("game:session:" + gameSession.getSessionId(), gameSession);
    }
}
