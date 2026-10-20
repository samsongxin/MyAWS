package com.dapai.game;

import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DeckTest {
    @Test
    void twoDecksHave108DistinctPhysicalCardsAndFourJokers() {
        var cards = Deck.standard();
        assertThat(cards).hasSize(108).doesNotHaveDuplicates();
        assertThat(cards.stream().map(Card::id)).doesNotHaveDuplicates();
        assertThat(cards.stream().filter(card -> card.rank().isJoker())).hasSize(4);
        assertThat(cards.stream().filter(card -> card.isWildcard(Rank.TWO))).hasSize(2);
    }

    @Test
    void dealsFourHandsOf27Cards() {
        var hands = Deck.deal(new Random(42));
        assertThat(hands).hasSize(4).allSatisfy(hand -> assertThat(hand).hasSize(27));
        var dealt = hands.stream().flatMap(List::stream).toList();
        assertThat(dealt).containsExactlyInAnyOrderElementsOf(Deck.standard());
    }
}