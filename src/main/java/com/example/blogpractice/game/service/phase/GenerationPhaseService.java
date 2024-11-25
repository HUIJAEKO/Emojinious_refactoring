package com.example.blogpractice.game.service.phase;

import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.service.image.ImageGenerator;
import com.example.blogpractice.game.service.manage.GameSessionManager;
import com.example.blogpractice.websocket.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GenerationPhaseService{
    private final MessageUtil messageUtil;
    private final GameSessionManager gameSessionManager;
    private final ImageGenerator imageGenerator;
    private final PhaseService phaseService;

    public void startGenerationPhase(GameSession gameSession) {
        System.out.println("GameService.startGenerationPhase");
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Image Generation");
        messageUtil.broadcastGameState(gameSession.getSessionId(), gameSessionManager.createGameStateDto(gameSession));

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        System.out.println("gameSession = " + gameSession.getCurrentPrompts());

        gameSession.getCurrentPrompts().forEach((playerId, prompt) -> {
            //프롬프트 공백
            if (prompt == null || prompt.trim().isEmpty()) {
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
                    phaseService.updateGameSession(gameSession);
                    phaseService.moveToNextPhase(gameSession);
                });
    }
}
