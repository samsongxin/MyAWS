package com.dapai.game;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class Deck {
    private Deck() {
    }

    public static List<Card> standard() {
        var cards = new ArrayList<Card>(108);
        for (int deck = 0; deck < 2; deck++) {
            for (Rank rank : Rank.values()) {
                if (rank.isJoker()) {
                    cards.add(new Card(deck, Suit.JOKER, rank));
                } else {
                    for (Suit suit : Suit.values()) {
                        if (suit != Suit.JOKER) {
                            cards.add(new Card(deck, suit, rank));
                        }
                    }
                }
            }
        }
        return List.copyOf(cards);
    }

    public static List<List<Card>> deal() {
        return deal(new SecureRandom());
    }

    public static List<List<Card>> deal(Random random) {
        Objects.requireNonNull(random, "A shuffle random source is required.");
        var cards = new ArrayList<>(standard());
        Collections.shuffle(cards, random);
        var hands = new ArrayList<List<Card>>(4);
        for (int seat = 0; seat < 4; seat++) {
            var hand = new ArrayList<Card>(27);
            for (int position = seat; position < cards.size(); position += 4) {
                hand.add(cards.get(position));
            }
            hands.add(List.copyOf(hand));
        }
        return List.copyOf(hands);
    }
}