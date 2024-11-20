package com.example.blogpractice.game.service.phase;

import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.service.manage.GameService;
import com.example.blogpractice.redis.util.RedisUtil;
import com.example.blogpractice.websocket.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PhaseService {
    private final RedisUtil redisUtil;
    private final MessageUtil messageUtil;
    private final GameService gameService;
    private static final String GAME_SESSION_KEY = "game:session:";

    public void startLoadingPhase(GameSession gameSession) {
        System.out.println("GameService.startLoadingPhase");
        messageUtil.broadcastPhaseStartMessage(gameSession.getSessionId(), gameSession.getCurrentPhase(), "Keyword Generation");
        Map<String, String> keywords = randomWordGenerator.getKeywordsFromTheme(
                gameSession.getPlayers(),
                gameSession.getSettings().getTheme(),
                gameSession.getSettings().getDifficulty(),
                gameSession.getPlayers().size()
        );
        gameSession.getCurrentKeywords().clear();
        gameSession.getCurrentKeywords().putAll(keywords);
        gameService.updateGameSession(gameSession);
        moveToNextPhase(gameSession);
    }

    private void moveToNextPhase(GameSession gameSession) {
        gameSession.moveToNextPhase();
        updateGameSession(gameSession);
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
        updateGameSession(gameSession);
    }

    private void updateGameSession(GameSession gameSession) {
        redisUtil.set(GAME_SESSION_KEY + gameSession.getSessionId(), gameSession);
    }
}
