package com.example.blogpractice.websocket.message;

import lombok.Data;

@Data
public class ConnectMessage {
    private String playerId;
    private String token;
}
