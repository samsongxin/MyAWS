package com.dapai.api;

import com.dapai.game.Card;
import com.dapai.game.Combination;
import com.dapai.game.GuandanRules;
import com.dapai.game.Rank;
import com.dapai.game.Suit;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RulesController {
    private final GuandanRules rules = new GuandanRules();

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("application", "DaPai", "status", "UP");
    }

    @PostMapping("/rules/evaluate")
    public Evaluation evaluate(@RequestBody EvaluationRequest request) {
        if (request.cards() == null) {
            throw new IllegalArgumentException("Cards are required.");
        }
        List<Card> cards = request.cards().stream().map(input -> {
            if (input == null || input.deck() == null) {
                throw new IllegalArgumentException("Every card requires a deck, suit, and rank.");
            }
            return new Card(input.deck(), input.suit(), input.rank());
        }).toList();
        List<Combination> combinations = rules.evaluate(cards, request.level());
        return new Evaluation(!combinations.isEmpty(), combinations);
    }

    public record CardInput(Integer deck, Suit suit, Rank rank) {
    }

    public record EvaluationRequest(Rank level, List<CardInput> cards) {
    }

    public record Evaluation(boolean valid, List<Combination> combinations) {
    }
}