package com.dapai.game;

public record Card(int deck, Suit suit, Rank rank) {
    public Card {
        if (deck < 0 || deck > 1 || suit == null || rank == null) {
            throw new IllegalArgumentException("A card requires deck 0 or 1, suit, and rank.");
        }
        if (rank.isJoker() != (suit == Suit.JOKER)) {
            throw new IllegalArgumentException("Joker ranks require the JOKER suit and vice versa.");
        }
    }

    public String id() {
        return deck + "-" + suit + "-" + rank;
    }

    public boolean isWildcard(Rank level) {
        return suit == Suit.HEARTS && rank == level;
    }
}