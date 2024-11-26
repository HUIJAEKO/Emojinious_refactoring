package com.example.blogpractice.game.service.phase;

import com.example.blogpractice.game.constant.GamePhase;
import com.example.blogpractice.game.constant.GameState;
import com.example.blogpractice.game.dto.TurnResultDto;
import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.service.image.ImageGenerator;
import com.example.blogpractice.game.service.manage.GameSessionUpdateManager;
import com.example.blogpractice.game.service.manage.GameStateManager;
import com.example.blogpractice.game.service.score.ScoreCalculator;
import com.example.blogpractice.game.service.word.RandomWordGenerator;
import com.example.blogpractice.websocket.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class PhaseService {
    private final RedisTemplate<String, String> redisTemplate;
    private final MessageUtil messageUtil;
    private final RandomWordGenerator randomWordGenerator;
    private final ImageGenerator imageGenerator;
    private final ScoreCalculator scoreCalculator;
    private final GameStateManager gameStateManager;
    private final GameSessionUpdateManager gameSessionUpdateManager;
    private static final String GAME_SESSION_KEY = "game:session:";

    private void moveToNextPhase(GameSession gameSession) {
        gameSession.moveToNextPhase();
        gameSessionUpdateManager.updateGameSession(gameSession);
        switch (gameSession.getCurrentPhase()) {
            case DESCRIPTION:
                startDescriptionPhase(gameSession);
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
            case RESULT:
                endGame(gameSession);
                break;
        }
        gameSessionUpdateManager.updateGameSession(gameSession);
    }

    public void startLoadingPhase(GameSession gameSession) {
        System.out.println("PhaseService.startLoadingPhase");
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Keyword Generation");
        Map<String, String> keywords = randomWordGenerator.getKeywordsFromTheme(
                gameSession.getPlayers(),
                gameSession.getSettings().getTheme(),
                gameSession.getSettings().getDifficulty(),
                gameSession.getPlayers().size()
        );
        gameSession.getCurrentKeywords().clear();
        gameSession.getCurrentKeywords().putAll(keywords);
        gameSessionUpdateManager.updateGameSession(gameSession);
        moveToNextPhase(gameSession);
    }

    private void startDescriptionPhase(GameSession gameSession) {
        System.out.println("PhaseService.startDescriptionPhase");
        gameSession.getPlayers().forEach(player ->
                messageUtil.sendToPlayer(gameSession.getSessionId(), player.getSocketId(), "keyword", gameSession.getCurrentKeywords().get(player.getId())));
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameStateManager.createGameStateDto(gameSession));
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Description Phase");
        updateSubmissionProgress(gameSession, "prompt");
    }

    private void startGenerationPhase(GameSession gameSession) {
        System.out.println("GameService.startGenerationPhase");
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Image Generation");
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameStateManager.createGameStateDto(gameSession));

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
                    gameSessionUpdateManager.updateGameSession(gameSession);
                    moveToNextPhase(gameSession);
                });
    }

    private void startCheckingPhase(GameSession gameSession){
        System.out.println("GameService.startCheckingPhase");
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameStateManager.createGameStateDto(gameSession));
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Checking Phase");

        Map<String, String> images = gameSession.getGeneratedImages();
        gameSession.getPlayers().forEach(player ->
                messageUtil.sendToPlayer(gameSession.getSessionId(), player.getSocketId(), "image", images.get(player.getId())));
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameStateManager.createGameStateDto(gameSession));
    }

    private void startGuessingPhase(GameSession gameSession) {
        System.out.println("GameService.startGuessingPhase");
        startNewGuessRound(gameSession);
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

    private void startNewGuessRound(GameSession gameSession) {
        System.out.println("GameService.startNewGuessRound");
        gameSession.startNewGuessRound();
        gameSessionUpdateManager.updateGameSession(gameSession);
        assignImagesToPlayers(gameSession);
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(),
                "Guessing Round " + gameSession.getCurrentGuessRound());
        updateSubmissionProgress(gameSession, "guess");
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameStateManager.createGameStateDto(gameSession));
    }

    private void updateSubmissionProgress(GameSession gameSession, String type) {
        int submitted;
        if (type.equals("prompt")) {
            submitted = gameSession.getCurrentPrompts().size();
        } else { // guess
            submitted = gameSession.getCurrentRoundSubmittedGuesses();
        }
        int total = gameSession.getPlayers().size();
        messageUtil.updateSubmissionProgress(gameSession.getSessionId(), type, submitted, total);
    }

    private void startTurnResultPhase(GameSession gameSession) {
        System.out.println("GameService.startTurnResultPhase");
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameStateManager.createGameStateDto(gameSession));
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Turn Result Phase");

        TurnResultDto scores = scoreCalculator.calculateFinalScores(gameSession);

        System.out.println(scores);

        messageUtil.broadcastGameState(gameSession.getSessionId(), gameStateManager.createGameStateDto(gameSession));
        messageUtil.broadcastGameResult(gameSession.getSessionId(), scores);
    }

    public void endGame(GameSession gameSession) {
        gameSession.setState(GameState.FINISHED);
        gameSessionUpdateManager.updateGameSession(gameSession);
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameStateManager.createGameStateDto(gameSession));
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "게임이 종료되었습니다. 결과를 확인해주세요.");

        redisTemplate.delete(GAME_SESSION_KEY + gameSession.getSessionId());
    }

    @Scheduled(fixedRate = 500)
    public void checkPhaseTimeouts() {
        List<String> sessionIds = Objects.requireNonNull(redisTemplate.keys(GAME_SESSION_KEY + "*")).stream()
                .map(key -> key.replace(GAME_SESSION_KEY, ""))
                .toList();

        for (String sessionId : sessionIds) {
            GameSession gameSession = gameSessionUpdateManager.getGameSession(sessionId);
            if (gameSession.getState() == GameState.IN_PROGRESS) {
                if (gameSession.getCurrentPhase() == GamePhase.GUESSING) {
                    handleGuessingPhaseTimeout(gameSession);
                } else if (gameSession.isPhaseTimedOut()) {
                    moveToNextPhase(gameSession);
                } else{
                    continue;
                }
            }
        }
    }

    private void handleGuessingPhaseTimeout(GameSession gameSession) {
        if (shouldMoveToNextRoundOrPhase(gameSession)) {
            if (gameSession.getCurrentGuessRound() < gameSession.getPlayers().size() - 1) {
                startNewGuessRound(gameSession);
            } else {
                moveToNextPhase(gameSession);
            }
        }
    }

    private boolean shouldMoveToNextRoundOrPhase(GameSession gameSession) {
        return gameSession.areAllPlayersGuessedOrTimedOut(gameSession.getSettings().getGuessTimeLimit());
    }
}
