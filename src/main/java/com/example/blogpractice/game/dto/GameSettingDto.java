package com.example.blogpractice.game.dto;

import lombok.Data;

@Data
public class GameSettingDto {
    private Integer promptTimeLimit;
    private Integer guessTimeLimit;
    private String difficulty;
    private Integer turns;
    private String theme;
}
