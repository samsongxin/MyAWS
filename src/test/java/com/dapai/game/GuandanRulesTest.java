package com.dapai.game;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class GuandanRulesTest {
    private final GuandanRules rules = new GuandanRules();

    @Test
    void levelAndJokerSinglesHaveTheCorrectOrder() {
        var ace = combination(List.of(card(Rank.ACE)), CombinationType.SINGLE, Rank.TWO);
        var level = combination(List.of(wild(0)), CombinationType.SINGLE, Rank.TWO);
        var small = combination(List.of(joker(0, Rank.SMALL_JOKER)), CombinationType.SINGLE, Rank.TWO);
        var big = combination(List.of(joker(0, Rank.BIG_JOKER)), CombinationType.SINGLE, Rank.TWO);
        assertThat(level.beats(ace)).isTrue();
        assertThat(small.beats(level)).isTrue();
        assertThat(big.beats(small)).isTrue();
    }

    @Test
    void recognizesPairsTriplesAndFullHouses() {
        assertThat(combination(group(Rank.SIX, 2), CombinationType.PAIR, Rank.TWO).strength()).isEqualTo(6);
        assertThat(combination(group(Rank.SIX, 3), CombinationType.TRIPLE, Rank.TWO).strength()).isEqualTo(6);
        var fullHouse = concat(group(Rank.SIX, 3), group(Rank.ACE, 2));
        assertThat(combination(fullHouse, CombinationType.FULL_HOUSE, Rank.TWO).strength()).isEqualTo(6);
    }

    @Test
    void recognizesAceLowAndAceHighStraights() {
        var low = List.of(card(Rank.ACE), card(Rank.TWO), card(Rank.THREE), card(Rank.FOUR), card(Rank.FIVE));
        var high = List.of(card(Rank.TEN), card(Rank.JACK), card(Rank.QUEEN), card(Rank.KING), card(Rank.ACE));
        assertThat(combination(low, CombinationType.STRAIGHT, Rank.TWO).strength()).isEqualTo(5);
        assertThat(combination(high, CombinationType.STRAIGHT, Rank.TWO).strength()).isEqualTo(14);
    }

    @Test
    void recognizesConsecutivePairsAndTriples() {
        var pairs = concat(group(Rank.ACE, 2), group(Rank.TWO, 2), group(Rank.THREE, 2));
        assertThat(combination(pairs, CombinationType.CONSECUTIVE_PAIRS, Rank.TWO).strength()).isEqualTo(3);
        var triples = concat(group(Rank.KING, 3), group(Rank.ACE, 3));
        assertThat(combination(triples, CombinationType.CONSECUTIVE_TRIPLES, Rank.TWO).strength()).isEqualTo(14);
    }

    @Test
        void wildcardCompletesAPair() {
        assertThat(combination(List.of(card(Rank.KING), wild(0)), CombinationType.PAIR, Rank.TWO).strength())
                .isEqualTo(13);
    }

    @Test
    void wildcardMayChangeSuitToCompleteStraightFlush() {
        var cards = List.of(card(Rank.THREE), card(Rank.FOUR), card(Rank.FIVE), card(Rank.SIX), wild(0));
        assertThat(rules.evaluate(cards, Rank.TWO)).contains(
                new Combination(CombinationType.STRAIGHT_FLUSH, 5, 6),
                new Combination(CombinationType.STRAIGHT_FLUSH, 5, 7));
    }

    @Test
    void bombHierarchyIsIndependentOfOrdinaryCombinationRank() {
        var four = combination(group(Rank.ACE, 4), CombinationType.BOMB, Rank.TWO);
        var five = combination(group(Rank.THREE, 5), CombinationType.BOMB, Rank.TWO);
        var six = combination(group(Rank.THREE, 6), CombinationType.BOMB, Rank.TWO);
        var flushCards = List.of(card(Rank.THREE), card(Rank.FOUR), card(Rank.FIVE), card(Rank.SIX), card(Rank.SEVEN));
        var flush = combination(flushCards, CombinationType.STRAIGHT_FLUSH, Rank.TWO);
        var jokers = combination(List.of(joker(0, Rank.SMALL_JOKER), joker(1, Rank.SMALL_JOKER),
                joker(0, Rank.BIG_JOKER), joker(1, Rank.BIG_JOKER)), CombinationType.FOUR_JOKERS, Rank.TWO);
        assertThat(five.beats(four)).isTrue();
        assertThat(flush.beats(five)).isTrue();
        assertThat(six.beats(flush)).isTrue();
        assertThat(jokers.beats(six)).isTrue();
        assertThat(four.beats(combination(List.of(joker(0, Rank.BIG_JOKER)), CombinationType.SINGLE, Rank.TWO)))
                .isTrue();
    }

    private Combination combination(List<Card> cards, CombinationType type, Rank level) {
        return rules.evaluate(cards, level).stream().filter(option -> option.type() == type)
                .findFirst().orElseThrow(() -> new AssertionError("Missing " + type + " for " + cards));
    }

    private Card card(Rank rank) {
        return new Card(0, Suit.SPADES, rank);
    }

    private Card wild(int deck) {
        return new Card(deck, Suit.HEARTS, Rank.TWO);
    }

    private Card joker(int deck, Rank rank) {
        return new Card(deck, Suit.JOKER, rank);
    }

    private List<Card> group(Rank rank, int count) {
        return Deck.standard().stream().filter(card -> card.rank() == rank).limit(count).toList();
    }

    @SafeVarargs
    private final List<Card> concat(List<Card>... groups) {
        var cards = new ArrayList<Card>();
        for (List<Card> group : groups) {
            cards.addAll(group);
        }
        return cards;
    }
}