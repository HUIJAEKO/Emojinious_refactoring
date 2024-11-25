package com.example.blogpractice.game.dto;

import com.example.blogpractice.player.dto.PlayerDto;
import lombok.Data;

import java.util.Map;

@Data
public class TurnResultDto {
    private Map<String, PlayerDto> turnResult;
}
