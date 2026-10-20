package com.dapai.game;

public record Combination(CombinationType type, int cardCount, int strength) {
    public Combination {
        if (type == null || cardCount < 1 || strength < 0) {
            throw new IllegalArgumentException("Invalid combination descriptor.");
        }
    }

    public boolean beats(Combination previous) {
        if (previous == null) {
            return true;
        }
        if (category() != previous.category()) {
            return category() > previous.category();
        }
        return type == previous.type && cardCount == previous.cardCount
                && strength > previous.strength;
    }

    private int category() {
        return switch (type) {
            case FOUR_JOKERS -> 100;
            case BOMB -> cardCount >= 6 ? 40 + cardCount : 20 + cardCount;
            case STRAIGHT_FLUSH -> 30;
            default -> 0;
        };
    }
}