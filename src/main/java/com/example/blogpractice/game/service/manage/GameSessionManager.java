package com.example.blogpractice.game.service.manage;

import com.example.blogpractice.game.dto.GameSettingDto;
import com.example.blogpractice.game.dto.GameStateDto;
import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.model.GameSettings;
import com.example.blogpractice.game.service.phase.PhaseService;
import com.example.blogpractice.player.domain.Player;
import com.example.blogpractice.player.dto.PlayerDto;
import com.example.blogpractice.player.service.PlayerService;
import com.example.blogpractice.websocket.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameSessionManager {
    private final MessageUtil messageUtil;
    private final PlayerService playerService;
    private final GameService gameService;
    private final PhaseService phaseService;
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

    public void updateSubmissionProgress(GameSession gameSession, String type) {
        int submitted;
        if (type.equals("prompt")) {
            submitted = gameSession.getCurrentPrompts().size();
        } else { // guess
            submitted = gameSession.getCurrentRoundSubmittedGuesses();
        }
        int total = gameSession.getPlayers().size();
        messageUtil.updateSubmissionProgress(gameSession.getSessionId(), type, submitted, total);
    }

    public void handlePlayerConnect(String sessionId, String playerId) {
        System.out.println("GameService.handlePlayerConnect");
        GameSession gameSession = gameService.getGameSession(sessionId);
        Player player = playerService.getPlayerById(playerId);
        if (player != null && !gameSession.getPlayers().contains(player)) {
            gameSession.addPlayer(player);
            gameService.updateGameSession(gameSession);
            messageUtil.broadcastGameState(gameSession.getSessionId(), createGameStateDto(gameSession));
        }
    }

    public void handlePlayerDisconnect(String sessionId, String playerId) {
        GameSession gameSession = gameService.getGameSession(sessionId);
        gameSession.removePlayer(playerId);
        gameService.updateGameSession(gameSession);
        if (gameSession.getPlayers().isEmpty()) {
            phaseService.endGame(gameSession);
        } else {
            gameService.updateGameSession(gameSession);
            messageUtil.broadcastGameState(gameSession.getSessionId(), createGameStateDto(gameSession));
        }
    }
}
