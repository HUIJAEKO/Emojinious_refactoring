package com.example.blogpractice.game.service.phase;

import com.example.blogpractice.game.model.GameSession;
import com.example.blogpractice.game.service.word.RandomWordGenerator;
import com.example.blogpractice.websocket.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
@Service
@RequiredArgsConstructor
public class LoadingPhaseService {
    private final MessageUtil messageUtil;
    private final RandomWordGenerator randomWordGenerator;
    private final PhaseService phaseService;

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
        phaseService.updateGameSession(gameSession);
        phaseService.moveToNextPhase(gameSession);
    }
}
