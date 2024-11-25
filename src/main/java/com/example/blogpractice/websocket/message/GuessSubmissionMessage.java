package com.example.blogpractice.websocket.message;

import lombok.Data;

@Data
public class GuessSubmissionMessage {
    private String guess;
}