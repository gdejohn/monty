package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Suit;

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.offset;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.comparingLong;

/// The category of a [hand][Hand].
public enum Category {
    HIGH_CARD(5, 407, 23_294_460),

    ONE_PAIR(5, 1_470, 58_627_800),

    TWO_PAIR(4, 763, 31_433_400),

    THREE_OF_A_KIND(5, 575, 6_461_620),

    STRAIGHT(2, 10, 6_180_020) {
        private static final long SUIT = 1L << offset(3)  // spades
                                       | 1L << offset(2)  // hearts
                                       | 1L << offset(1)  // diamonds
                                       | 1L << offset(0); // clubs

        private static int straight(int high) {
            return -(high << 9) & (high << 14) - 1;
        }

        @Override
        Comparator<Card> order(Hand hand, int value) {
            return comparingLong(
                (Card card) -> (hand.mask() & (SUIT << card.rank().ordinal())) >>> card.offset()
            ).thenComparing(super.order(hand, straight(value & -value)));
        }
    },

    FLUSH(7, 1_277, 4_047_644) {
        @Override
        Comparator<Card> order(Hand hand, int value) {
            return comparing(
                Card::suit,
                comparingInt(hand::count).reversed()
            ).thenComparing(super.order(hand, value));
        }
    },

    FULL_HOUSE(4, 156, 3_473_184),

    FOUR_OF_A_KIND(5, 156, 224_848),

    STRAIGHT_FLUSH(2, 10, 41_584) {
        @Override
        Comparator<Card> order(Hand hand, int value) {
            return FLUSH.order(hand, value).thenComparing(STRAIGHT.order(hand, value));
        }
    };

    static final int OFFSET = 26;

    private static final Category[] categories = Category.values();

    /// The number of bits needed to represent a hand in this category.
    final int count;

    /// The number of seven-card hand equivalence classes in this category.
    final int classes;

    /// The number of seven-card hands in this category.
    final int hands;

    Category(int count, int classes, int hands) {
        this.count = count;
        this.classes = classes;
        this.hands = hands;
    }

    /// Every category, in ascending order.
    public static Stream<Category> all() {
        return Arrays.stream(categories);
    }

    static Category of(int value) {
        return categories[value >>> Category.OFFSET];
    }

    Comparator<Card> order(Hand hand, int value) {
        int SIGNIFICANCE = 1 << 13 | 1;
        return comparing(
            Card::rank,
            comparingInt(
                rank -> value & SIGNIFICANCE << rank.ordinal()
            )
        ).thenComparing(Card::suit, comparingInt(Suit::ordinal)).reversed();
    }
}
