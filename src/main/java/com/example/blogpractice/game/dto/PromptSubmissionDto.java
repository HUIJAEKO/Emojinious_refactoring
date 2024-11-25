package com.example.blogpractice.game.dto;

import lombok.Data;

@Data
public class PromptSubmissionDto {
    private String prompt;

    public PromptSubmissionDto(){

    }

    public PromptSubmissionDto(String prompt){
        this.prompt = prompt;
    }
}
