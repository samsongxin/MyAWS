package com.dapai.game;

public enum Rank {
    TWO(2), THREE(3), FOUR(4), FIVE(5), SIX(6), SEVEN(7), EIGHT(8),
    NINE(9), TEN(10), JACK(11), QUEEN(12), KING(13), ACE(14),
    SMALL_JOKER(16), BIG_JOKER(17);

    private final int value;

    Rank(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public boolean isJoker() {
        return this == SMALL_JOKER || this == BIG_JOKER;
    }

    public int strength(Rank level) {
        return this == level ? 15 : value;
    }
}