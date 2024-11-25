package com.example.blogpractice.game.service.phase;

import com.example.blogpractice.game.dto.TurnResultDto;
import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.service.manage.GameSessionManager;
import com.example.blogpractice.game.service.score.ScoreCalculator;
import com.example.blogpractice.redis.util.RedisUtil;
import com.example.blogpractice.websocket.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhaseService {
    private final RedisUtil redisUtil;
    private final MessageUtil messageUtil;
    private final GameSessionManager gameSessionManager;
    private final DescriptionPhaseService descriptionPhaseService;
    private final GenerationPhaseService generationPhaseService;
    private final ScoreCalculator scoreCalculator;
    private final CheckingPhaseService checkingPhaseService;
    private static final String GAME_SESSION_KEY = "game:session:";

    void moveToNextPhase(GameSession gameSession) {
        gameSession.moveToNextPhase();
        updateGameSession(gameSession);
        switch (gameSession.getCurrentPhase()) {
            case DESCRIPTION:
                descriptionPhaseService.startDescriptionPhase(gameSession);
                break;
            case GENERATION:
                generationPhaseService.startGenerationPhase(gameSession);
                break;
            case CHECKING:
                checkingPhaseService.startCheckingPhase(gameSession);
                break;
            case GUESSING:
                startGuessingPhase(gameSession);
                break;
            case TURN_RESULT:
                startTurnResultPhase(gameSession);
                break;
//            case RESULT:
//                endGame(gameSession);
//                break;
        }
        updateGameSession(gameSession);
    }

    private void startGuessingPhase(GameSession gameSession) {
        System.out.println("GameService.startGuessingPhase");
        startNewGuessRound(gameSession);
    }

    private void startNewGuessRound(GameSession gameSession) {
        System.out.println("GameService.startNewGuessRound");
        gameSession.startNewGuessRound();
        updateGameSession(gameSession);
        assignImagesToPlayers(gameSession);
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(),
                "Guessing Round " + gameSession.getCurrentGuessRound());
        updateSubmissionProgress(gameSession, "guess");
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
    }

    private void assignImagesToPlayers(GameSession gameSession) {
        gameSession.getPlayers().forEach(player -> {
            String imageUrl = getNextImageForPlayer(gameSession, player.getId());
            messageUtil.sendToPlayer(gameSession.getSessionId(), player.getSocketId(), "image", imageUrl);
        });
    }

    private String getNextImageForPlayer(GameSession gameSession, String playerId) {
        String targetPlayerId = gameSession.getGuessTargetForPlayer(playerId);
        return gameSession.getGeneratedImages().get(targetPlayerId);
    }

    private void startTurnResultPhase(GameSession gameSession) {
        System.out.println("GameService.startTurnResultPhase");
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Turn Result Phase");

        TurnResultDto scores = scoreCalculator.calculateFinalScores(gameSession);

        System.out.println(scores);

        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
        messageUtil.broadcastGameResult(gameSession.getSessionId(), scores);
    }

    void updateGameSession(GameSession gameSession) {
        redisUtil.set(GAME_SESSION_KEY + gameSession.getSessionId(), gameSession);
    }

    void updateSubmissionProgress(GameSession gameSession, String type) {
        int submitted;
        if (type.equals("prompt")) {
            submitted = gameSession.getCurrentPrompts().size();
        } else { // guess
            submitted = gameSession.getCurrentRoundSubmittedGuesses();
        }
        int total = gameSession.getPlayers().size();
        messageUtil.updateSubmissionProgress(gameSession.getSessionId(), type, submitted, total);
    }
}
