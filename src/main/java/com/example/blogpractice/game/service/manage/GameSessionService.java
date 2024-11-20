package com.example.blogpractice.game.service.manage;

import com.example.blogpractice.game.dto.GameSettingDto;
import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.model.GameSettings;
import com.example.blogpractice.player.domain.Player;
import com.example.blogpractice.redis.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameSessionService {
    private final RedisUtil redisUtil;

    public void updateGameSettings(String sessionId, String playerId, GameSettingDto settings) {
        Player player = redisUtil.get("player" + playerId, Player.class);
        if (player == null) {
            throw new IllegalArgumentException("Player not found");
        }

        if (!player.getSessionId().equals(sessionId) || !player.isHost()) {
            throw new IllegalArgumentException("Invalid player or not authorized to change settings");
        }

        String gameSettingsKey = "game:settings:" + sessionId;
        GameSettings currentSettings = redisUtil.get(gameSettingsKey, GameSettings.class);
        if (currentSettings == null) {
            currentSettings = new GameSettings();
        }

        updateSettingsIfPresent(currentSettings, settings);

        redisUtil.set(gameSettingsKey, currentSettings);

        GameSession gameSession = redisUtil.get("game:session:" + sessionId, GameSession.class);
        if (gameSession != null) {
            gameSession.setSettings(currentSettings);
            redisUtil.set("game:session:" + sessionId, gameSession);
        }
    }

    private void updateSettingsIfPresent(GameSettings currentSettings, GameSettingDto newSettings) {
        if (newSettings.getPromptTimeLimit() != null) {
            currentSettings.setPromptTimeLimit(newSettings.getPromptTimeLimit());
        }
        if (newSettings.getGuessTimeLimit() != null) {
            currentSettings.setGuessTimeLimit(newSettings.getGuessTimeLimit());
        }
        if (newSettings.getDifficulty() != null) {
            currentSettings.setDifficulty(newSettings.getDifficulty());
        }
        if (newSettings.getTurns() != null) {
            currentSettings.setTurns(newSettings.getTurns());
        }
        if (newSettings.getTheme() != null) {
            currentSettings.setTheme(newSettings.getTheme());
        }
    }
}
