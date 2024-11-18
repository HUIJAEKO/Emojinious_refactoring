package com.example.blogpractice.game.service.manage;

import com.example.blogpractice.game.dto.GameSettingDto;
import com.example.blogpractice.game.dto.GameStateDto;
import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.model.GameSettings;
import com.example.blogpractice.message.dto.ChatMessage;
import com.example.blogpractice.player.domain.Player;
import com.example.blogpractice.player.dto.PlayerDto;
import com.example.blogpractice.player.service.PlayerService;
import com.example.blogpractice.util.MessageUtil;
import com.example.blogpractice.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {
    private final PlayerService playerService;
    private final RedisUtil redisUtil;
    private final MessageUtil messageUtil;
    private final Map<String, Set<String>> activeConnections = new ConcurrentHashMap<>();
    private static final String GAME_SESSION_KEY = "game:session:";

    public void handleExistingConnection(String sessionId, String playerId) {
        Set<String> sessionPlayers = activeConnections.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet());
        if (sessionPlayers.contains(playerId)) {
            sessionPlayers.remove(playerId);
        }
    }

    public boolean isPlayerAlreadyJoined(String sessionId, String playerId) {
        GameSession gameSession = getGameSession(sessionId);
        System.out.println(gameSession != null && gameSession.getPlayerById(playerId) != null);
        return gameSession != null && gameSession.getPlayerById(playerId) != null;
    }

    public GameStateDto joinGame(String sessionId, String playerId, String nickname) {
        GameSession gameSession = getOrCreateGameSession(sessionId);
        Player player = playerService.getPlayerById(playerId);
        gameSession.addPlayer(player);
        updateGameSession(gameSession);
        messageUtil.broadcastGameState(gameSession.getSessionId(), createGameStateDto(gameSession));
        return createGameStateDto(gameSession);
    }

    public GameStateDto getGameState(String sessionId) {
        GameSession gameSession = getGameSession(sessionId);
        return createGameStateDto(gameSession);
    }

    private void updateGameSession(GameSession gameSession) {
        redisUtil.set(GAME_SESSION_KEY + gameSession.getSessionId(), gameSession);
    }

    public GameSession getGameSession(String sessionId) {
        return redisUtil.get(GAME_SESSION_KEY + sessionId, GameSession.class);
    }

    private GameSession getOrCreateGameSession(String sessionId) {
        GameSession gameSession = redisUtil.get(GAME_SESSION_KEY + sessionId, GameSession.class);
        if (gameSession == null) {
            gameSession = new GameSession(sessionId);
            updateGameSession(gameSession);
        }
        return gameSession;
    }

    private GameStateDto createGameStateDto(GameSession gameSession) {
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

    public void broadcastChatMessage(String sessionId, ChatMessage message) {
        messageUtil.broadcastChatMessage(sessionId, message);
    }

}
