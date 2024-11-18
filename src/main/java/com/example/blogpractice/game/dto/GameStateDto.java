package com.example.blogpractice.game.dto;

import com.example.blogpractice.game.constant.GameState;
import com.example.blogpractice.player.dto.PlayerDto;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GameStateDto {
    private String sessionId;
    private List<PlayerDto> players;
    private GameSettingDto settings;
    private GameState state;
    private int currentTurn;
    private int currentPhase;
    private Map<String, String> currentPrompts;
    private Map<String, Map<String, String>> currentGuesses;
    private long remainingTime;
}
