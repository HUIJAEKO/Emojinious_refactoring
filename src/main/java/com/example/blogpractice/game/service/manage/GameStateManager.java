package com.example.blogpractice.game.service.manage;

import com.example.blogpractice.game.dto.GameSettingDto;
import com.example.blogpractice.game.dto.GameStateDto;
import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.model.GameSettings;
import com.example.blogpractice.player.domain.Player;
import com.example.blogpractice.player.dto.PlayerDto;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class GameStateManager {

    public GameStateDto createGameStateDto(GameSession gameSession) {
        GameStateDto dto = new GameStateDto();
        dto.setSessionId(gameSession.getSessionId());
        dto.setPlayers(gameSession.getPlayers().stream()
                .map(this::convertToPlayerDto)
                .collect(Collectors.toList()));
        dto.setSettings(convertToGameSettingsDto(gameSession.getSettings()));
        dto.setState(gameSession.getState());
        dto.setCurrentTurn(gameSession.getCurrentTurn());
        dto.setCurrentPhase(gameSession.getCurrentPhase().ordinal());
        dto.setRemainingTime(gameSession.getRemainingTime());

        return dto;
    }

    private PlayerDto convertToPlayerDto(Player player) {
        PlayerDto dto = new PlayerDto();
        dto.setId(player.getId());
        dto.setNickname(player.getNickname());
        dto.setCharacterId(player.getCharacterId());
        dto.setHost(player.isHost());
        dto.setScore(player.getScore());
        return dto;
    }

    private GameSettingDto convertToGameSettingsDto(GameSettings settings) {
        GameSettingDto dto = new GameSettingDto();
        dto.setPromptTimeLimit(settings.getPromptTimeLimit());
        dto.setGuessTimeLimit(settings.getGuessTimeLimit());
        dto.setDifficulty(settings.getDifficulty());
        dto.setTurns(settings.getTurns());
        dto.setTheme(settings.getTheme());
        return dto;
    }
}
