package com.example.blogpractice.player.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Data
@RedisHash("player_session")
@NoArgsConstructor
@AllArgsConstructor
public class Player implements Serializable {
    private String id;
    private String nickname;
    private int characterId;
    private boolean isHost;
    private Integer score;
    private String token;
    private String sessionId;
    private String socketId;

    public Player(String id, String nickname, int characterId, boolean isHost){
        this.id = id;
        this.nickname = nickname;
        this.characterId = characterId;
        this.isHost = isHost;
        this.score = 0;
    }
}
