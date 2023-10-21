package io.github.gdejohn.monty;

import static io.github.gdejohn.monty.Card.Rank.FIVE;
import static java.util.Arrays.stream;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.toCollection;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.stream.Stream;

import io.github.gdejohn.monty.Card.Cards;
import io.github.gdejohn.monty.Card.Rank;
import io.github.gdejohn.monty.Card.Suit;

public enum Category {
    HIGH_CARD(5, 407, 23_294_460),

    ONE_PAIR(5, 1_470, 58_627_800),

    TWO_PAIR(4, 763, 31_433_400) {
        @Override
        Stream<Card> cards(Hand hand) {
            var pairs = Category.unpack(hand).limit(4);
            var kicker = Category.unpack(hand).skip(4).max(comparing(Card::rank)).stream();
            return Stream.concat(pairs, kicker);
        }
    },

    THREE_OF_A_KIND(5, 575, 6_461_620),

    STRAIGHT(2, 10, 6_180_020) {
        @Override
        Stream<Card> cards(Hand hand) {
            var high = Rank.unpack(hand.evaluate());
            var cards = Cards.unpack(hand.cards).collect(
                groupingBy(
                    Card::rank,
                    () -> new EnumMap<>(Rank.class),
                    maxBy(comparing(Card::suit))
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

    FLUSH(7, 1_277, 4_047_644) {
        @Override
        Stream<Card> cards(Hand hand) {
            return flush(hand, comparing(Card::rank).reversed()).limit(5);
        }
    },

    FULL_HOUSE(4, 156, 3_473_184),

    FOUR_OF_A_KIND(5, 156, 224_848) {
        @Override
        Stream<Card> cards(Hand hand) {
            var quads = Category.unpack(hand).limit(4);
            var kicker = Category.unpack(hand).skip(4).max(comparing(Card::rank)).stream();
            return Stream.concat(quads, kicker);
        }
    },

    STRAIGHT_FLUSH(2, 10, 41_584) {
        @Override
        Stream<Card> cards(Hand hand) {
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

    private static final Comparator<Card> reverseLowball = comparingInt(card -> -(card.rank().ordinal() + 1) % 13);

    private static final int SHIFT = 26;

    private static final Category[] categories = Category.values();

    final int bits;

    final int classes;

    final int hands;

    Category(int bits, int classes, int hands) {
        this.bits = bits;
        this.classes = classes;
        this.hands = hands;
    }

    static Stream<Category> categories() {
        return stream(categories);
    }

    private static Stream<Card> flush(Hand hand, Comparator<Card> order) {
        return Cards.unpack(hand.cards).collect(
            groupingBy(
                Card::suit,
                () -> new EnumMap<>(Suit.class),
                toCollection(() -> new TreeSet<>(order))
            )
        ).entrySet().stream().max(
            comparingByValue(comparingInt(Set::size))
        ).stream().map(Entry::getValue).flatMap(Set::stream);
    }

    static Category unpack(int value) {
        return categories[value >>> Category.SHIFT];
    }

    Stream<Card> cards(Hand hand) {
        return Category.unpack(hand).limit(5);
    }

    static Stream<Card> unpack(Hand hand) {
        return Cards.unpack(hand.cards).collect(
            groupingBy(
                Card::rank,
                () -> new EnumMap<>(Rank.class),
                toCollection(() -> new TreeSet<>(comparing(Card::suit).reversed()))
            )
        ).entrySet().stream().sorted(
            comparingInt(
                (Entry<Rank, TreeSet<Card>> entry) -> entry.getValue().size()
            ).thenComparing(Entry::getKey).reversed()
        ).map(Entry::getValue).flatMap(Set::stream);
    }

    final int pack(long ranks) {
        return (this.ordinal() << Category.SHIFT) | (int) ranks;
    }
}