package com.dapai.game;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GuandanRules {
    public List<Combination> evaluate(List<Card> cards, Rank level) {
        if (level == null || level.isJoker()) {
            throw new IllegalArgumentException("The level must be a rank from TWO through ACE.");
        }
        if (cards == null || cards.isEmpty() || cards.size() > 27) {
            throw new IllegalArgumentException("Select between 1 and 27 cards.");
        }
        if (cards.stream().anyMatch(card -> card == null) || new HashSet<>(cards).size() != cards.size()) {
            throw new IllegalArgumentException("Cards must be non-null and physically distinct.");
        }
        if (cards.size() == 1) {
            return List.of(new Combination(CombinationType.SINGLE, 1, cards.get(0).rank().strength(level)));
        }

        int[] counts = new int[18];
        int wildcards = 0;
        for (Card card : cards) {
            if (card.isWildcard(level)) {
                wildcards++;
            } else {
                counts[card.rank().value()]++;
            }
        }
        Set<Combination> options = new LinkedHashSet<>();
        if (cards.size() == 4 && counts[16] == 2 && counts[17] == 2) {
            options.add(new Combination(CombinationType.FOUR_JOKERS, 4, 0));
        }
        if (cards.size() == 2 || cards.size() == 3 || (cards.size() >= 4 && cards.size() <= 10)) {
            addGroups(cards.size(), counts, wildcards, level, options);
        }
        if (cards.size() == 5) {
            addFullHouses(counts, wildcards, level, options);
            addSequences(cards, counts, wildcards, level, 5, 1, CombinationType.STRAIGHT, options);
        }
        if (cards.size() == 6) {
            addSequences(cards, counts, wildcards, level, 3, 2, CombinationType.CONSECUTIVE_PAIRS, options);
            addSequences(cards, counts, wildcards, level, 2, 3, CombinationType.CONSECUTIVE_TRIPLES, options);
        }
        return List.copyOf(options);
    }

    private void addGroups(int size, int[] counts, int wildcards, Rank level, Set<Combination> options) {
        CombinationType type = size == 2 ? CombinationType.PAIR
                : size == 3 ? CombinationType.TRIPLE : CombinationType.BOMB;
        for (Rank rank : Rank.values()) {
            if (rank.isJoker() && size != 2) {
                continue;
            }
            int[] pattern = new int[18];
            pattern[rank.value()] = size;
            if (matches(counts, wildcards, pattern)) {
                options.add(new Combination(type, size, rank.strength(level)));
            }
        }
    }

    private void addFullHouses(int[] counts, int wildcards, Rank level, Set<Combination> options) {
        for (Rank triple : Rank.values()) {
            if (triple.isJoker()) {
                continue;
            }
            for (Rank pair : Rank.values()) {
                if (triple == pair) {
                    continue;
                }
                int[] pattern = new int[18];
                pattern[triple.value()] = 3;
                pattern[pair.value()] = 2;
                if (matches(counts, wildcards, pattern)) {
                    options.add(new Combination(CombinationType.FULL_HOUSE, 5, triple.strength(level)));
                }
            }
        }
    }

    private void addSequences(List<Card> cards, int[] counts, int wildcards, Rank level, int length,
                              int copies, CombinationType type, Set<Combination> options) {
        for (int start = 1; start <= 15 - length; start++) {
            int[] pattern = new int[18];
            for (int offset = 0; offset < length; offset++) {
                int sequenceRank = start + offset;
                pattern[sequenceRank == 1 ? 14 : sequenceRank] = copies;
            }
            if (matches(counts, wildcards, pattern)) {
                int strength = start + length - 1;
                options.add(new Combination(type, cards.size(), strength));
                if (type == CombinationType.STRAIGHT && isSameSuit(cards, level)) {
                    options.add(new Combination(CombinationType.STRAIGHT_FLUSH, cards.size(), strength));
                }
            }
        }
    }

    private boolean isSameSuit(List<Card> cards, Rank level) {
        return cards.stream().filter(card -> !card.isWildcard(level))
                .map(Card::suit).distinct().count() == 1;
    }

    private boolean matches(int[] counts, int wildcards, int[] pattern) {
        int missing = 0;
        for (int rank = 2; rank < counts.length; rank++) {
            if (counts[rank] > pattern[rank]) {
                return false;
            }
            int deficit = pattern[rank] - counts[rank];
            if (rank >= 16 && deficit != 0) {
                return false;
            }
            missing += deficit;
        }
        return missing == wildcards;
    }
}