package com.example.blogpractice.websocket.message;

import lombok.Data;

@Data
public class ChatMessage {
    private String playerId;
    private String sender;
    private String content;
    private long timestamp;
}
