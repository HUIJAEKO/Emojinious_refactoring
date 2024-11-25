package com.example.blogpractice.game.service.phase;

import com.example.blogpractice.game.dto.TurnResultDto;
import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.service.image.ImageGenerator;
import com.example.blogpractice.game.service.manage.GameSessionManager;
import com.example.blogpractice.game.service.score.ScoreCalculator;
import com.example.blogpractice.redis.util.RedisUtil;
import com.example.blogpractice.websocket.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PhaseService {
    private final RedisUtil redisUtil;
    private final MessageUtil messageUtil;
    private final GameSessionManager gameSessionManager;
    private final DescriptionPhaseService descriptionPhaseService;
    private final ImageGenerator imageGenerator;
    private final ScoreCalculator scoreCalculator;
    private static final String GAME_SESSION_KEY = "game:session:";

    void moveToNextPhase(GameSession gameSession) {
        gameSession.moveToNextPhase();
        updateGameSession(gameSession);
        switch (gameSession.getCurrentPhase()) {
            case DESCRIPTION:
                descriptionPhaseService.startDescriptionPhase(gameSession);
                break;
            case GENERATION:
                startGenerationPhase(gameSession);
                break;
            case CHECKING:
                startCheckingPhase(gameSession);
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

    private void startGenerationPhase(GameSession gameSession) {
        System.out.println("GameService.startGenerationPhase");
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Image Generation");
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        System.out.println("gameSession = " + gameSession.getCurrentPrompts());

        gameSession.getCurrentPrompts().forEach((playerId, prompt) -> {
            //프롬프트 공백
            if(prompt == null || prompt.trim().isEmpty()){
                prompt = " ";
            }

            System.out.println("prompt = " + prompt);
            CompletableFuture<Void> future = imageGenerator.getImagesFromMessageAsync(prompt)
                    .thenAccept(imageUrl -> {
                        gameSession.setGeneratedImage(playerId, imageUrl);
                        System.out.println("img gen player: " + playerId);
                    });
            futures.add(future);
        });

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    System.out.println("all images generated");
                    updateGameSession(gameSession);
                    moveToNextPhase(gameSession);
                });
    }

    private void startCheckingPhase(GameSession gameSession){
        System.out.println("GameService.startCheckingPhase");
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Checking Phase");

        Map<String, String> images = gameSession.getGeneratedImages();
        gameSession.getPlayers().forEach(player ->
                messageUtil.sendToPlayer(gameSession.getSessionId(), player.getSocketId(), "image", images.get(player.getId())));
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));
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
