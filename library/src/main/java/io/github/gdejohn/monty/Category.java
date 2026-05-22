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
    /// Five kickers.
    HIGH_CARD(23_294_460, 407, 5),

    /// Two cards with the same rank, three kickers.
    ONE_PAIR(58_627_800, 1_470, 5),

    /// Two pairs, one kicker.
    TWO_PAIR(31_433_400, 763, 4),

    /// Three cards with the same rank, two kickers.
    THREE_OF_A_KIND(6_461_620, 575, 5),

    /// Five cards with consecutive ranks, ace can play low.
    STRAIGHT(6_180_020, 10, 2) {
        private static final long SUIT = 1L << offset(3)  // spades
                                       | 1L << offset(2)  // hearts
                                       | 1L << offset(1)  // diamonds
                                       | 1L << offset(0); // clubs

        static int straight(int high) {
            return -(high << 9) & (high << 14) - 1;
        }

        @Override
        Comparator<Card> comparator(Hand hand, int value) {
            return comparingLong(
                (Card card) -> (hand.mask() & (SUIT << card.rank().ordinal())) >>> card.offset
            ).thenComparing(super.comparator(hand, straight(value & -value)));
        }
    },

    /// Five cards with the same suit.
    FLUSH(4_047_644, 1_277, 7) {
        @Override
        Comparator<Card> comparator(Hand hand, int value) {
            return comparing(
                Card::suit,
                comparingInt(hand::count).reversed()
            ).thenComparing(super.comparator(hand, value));
        }
    },

    /// A three-of-a-kind and a pair.
    FULL_HOUSE(3_473_184, 156, 4),

    /// Four cards with the same rank, one kicker.
    FOUR_OF_A_KIND(224_848, 156, 5),

    /// Five cards with consecutive ranks and the same suit, ace can play low.
    STRAIGHT_FLUSH(41_584, 10, 2) {
        @Override
        Comparator<Card> comparator(Hand hand, int value) {
            return FLUSH.comparator(hand, value).thenComparing(STRAIGHT.comparator(hand, value));
        }
    };

    private static final Category[] CATEGORIES = Category.values();

    /// The number of seven-card hands in this category.
    final int hands;

    /// The number of seven-card equivalence classes in this category.
    final int classes;

    /// The number of bits needed to represent a hand in this category.
    final int bits;

    Category(int hands, int classes, int bits) {
        this.classes = classes;
        this.hands = hands;
        this.bits = bits;
    }

    /// Every category, in ascending order.
    public static Stream<Category> all() {
        return Arrays.stream(CATEGORIES);
    }

    /// Extract the category from a given hand [value][Hand#evaluate()].
    static Category of(int value) {
        return CATEGORIES[value >>> 26];
    }

    Comparator<Card> comparator(Hand hand, int value) {
        int SIGNIFICANCE = (1 << 13) | 1;
        return comparing(
            Card::rank,
            comparingInt(rank -> value & (SIGNIFICANCE << rank.ordinal()))
        ).thenComparing(Card::suit, comparingInt(Suit::ordinal)).reversed();
    }
}
