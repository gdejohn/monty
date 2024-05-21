package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Rank;
import io.github.gdejohn.monty.Card.Suit;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.Rank.FIVE;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.toCollection;

public enum Category {
    HIGH_CARD(23_294_460, 407, 5),

    ONE_PAIR(58_627_800, 1_470, 5),

    TWO_PAIR(31_433_400, 763, 4) {
        @Override
        Stream<Card> sort(Hand hand) {
            var pairs = stream(hand).limit(4);
            var kicker = stream(hand).skip(4).max(comparing(Card::rank)).stream();
            return Stream.concat(pairs, kicker);
        }
    },

    THREE_OF_A_KIND(6_461_620, 575, 5),

    STRAIGHT(6_180_020, 10, 2) {
        @Override
        Stream<Card> sort(Hand hand) {
            Rank high = Rank.unpack(hand.evaluate());
            Stream<Card> cards = Card.stream(hand.cards()).collect(
                groupingBy(
                    Card::rank,
                    maxBy(comparing(Card::suit, Suit::compare))
                )
            ).values().stream().flatMap(Optional::stream);
            if (high == FIVE) { // wheel
                return cards.sorted(reverseLowball).dropWhile(card -> card.rank() != high);
            } else {
                return cards.sorted(comparing(Card::rank).reversed()).dropWhile(
                    card -> card.rank() != high
                ).limit(5);
            }
        }
    },

    FLUSH(4_047_644, 1_277, 7) {
        @Override
        Stream<Card> sort(Hand hand) {
            return flush(hand, comparing(Card::rank).reversed()).limit(5);
        }
    },

    FULL_HOUSE(3_473_184, 156, 4),

    FOUR_OF_A_KIND(224_848, 156, 5) {
        @Override
        Stream<Card> sort(Hand hand) {
            var quads = stream(hand).limit(4);
            var kicker = stream(hand).skip(4).max(comparing(Card::rank)).stream();
            return Stream.concat(quads, kicker);
        }
    },

    STRAIGHT_FLUSH(41_584, 10, 2) {
        @Override
        Stream<Card> sort(Hand hand) {
            var high = Rank.unpack(hand.evaluate());
            if (high == FIVE) { // steel wheel
                return flush(hand, reverseLowball).dropWhile(card -> card.rank() != high);
            } else {
                return flush(hand, comparing(Card::rank).reversed()).dropWhile(
                    card -> card.rank() != high
                ).limit(5);
            }
        }
    };

    private static final Comparator<Card> reverseLowball = comparingInt(
        card -> -(card.rank().ordinal() + 1) % 13
    );

    private static final int SHIFT = 26;

    private static final Category[] categories = Category.values();

    final int hands;

    final int classes;

    final int count;

    Category(int hands, int classes, int count) {
        this.hands = hands;
        this.classes = classes;
        this.count = count;
    }

    static Category unpack(int value) {
        return categories[value >>> Category.SHIFT];
    }

    Stream<Card> sort(Hand hand) {
        return stream(hand).limit(5);
    }

    private static Stream<Card> stream(Hand hand) {
        return Card.stream(hand.cards()).collect(
            groupingBy(
                Card::rank,
                toCollection(
                    () -> new TreeSet<>(comparing(Card::suit, Suit::compare).reversed())
                )
            )
        ).entrySet().stream().sorted(
            comparingInt(
                (Entry<Rank,TreeSet<Card>> entry) -> entry.getValue().size()
            ).thenComparing(Entry::getKey).reversed()
        ).map(Entry::getValue).flatMap(Collection::stream);
    }

    private static Stream<Card> flush(Hand hand, Comparator<Card> order) {
        return Card.stream(hand.cards()).collect(
            groupingBy(
                Card::suit,
                toCollection(() -> new TreeSet<>(order))
            )
        ).entrySet().stream().max(
            comparingByValue(comparingInt(Collection::size))
        ).stream().map(Entry::getValue).flatMap(Collection::stream);
    }
}
