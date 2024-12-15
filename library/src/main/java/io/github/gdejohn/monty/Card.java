package io.github.gdejohn.monty;

import java.util.Arrays;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Objects.checkIndex;
import static java.util.stream.Collector.Characteristics.UNORDERED;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

/// A playing card with one of thirteen [ranks][Rank] and one of four [suits][Suit].
public final class Card {
    /// The rank of a [card][Card].
    public static final class Rank implements Comparable<Rank> {
        private static final Rank[] ranks = range(0, 13).mapToObj(Rank::new)
                                                        .toArray(Rank[]::new);

        public static final Rank TWO = ranks[0],
                               THREE = ranks[1],
                                FOUR = ranks[2],
                                FIVE = ranks[3],
                                 SIX = ranks[4],
                               SEVEN = ranks[5],
                               EIGHT = ranks[6],
                                NINE = ranks[7],
                                 TEN = ranks[8],
                                JACK = ranks[9],
                               QUEEN = ranks[10],
                                KING = ranks[11],
                                 ACE = ranks[12];

        private final byte ordinal;

        private Rank(int ordinal) {
            this.ordinal = (byte) ordinal;
        }

        static Rank of(Card card) {
            return ranks[ordinal(card.offset)];
        }

        /// Every rank.
        public static Stream<Rank> all() {
            return Arrays.stream(ranks);
        }

        public Card of(Suit suit) {
            return Card.of(this.ordinal, suit.ordinal);
        }

        static int ordinal(int offset) {
            return offset & 0b1111;
        }

        /// The zero-based index of this rank in ascending order.
        public byte ordinal() {
            return ordinal;
        }

        public int mask() {
            return 1 << ordinal;
        }

        @Override
        public String toString() {
            return "23456789TJQKA".substring(ordinal, ordinal + 1);
        }

        @Override
        public int hashCode() {
            return ordinal;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Rank rank && ordinal == rank.ordinal;
        }

        @Override
        public int compareTo(Rank rank) {
            return Byte.compare(ordinal, rank.ordinal);
        }
    }

    /// The suit of a [card][Card].
    public static final class Suit {
        private static final Suit[] suits = range(0, 4).mapToObj(Suit::new)
                                                       .toArray(Suit[]::new);

        public static final Suit CLUBS = suits[0],
                                 DIAMONDS = suits[1],
                                 HEARTS = suits[2],
                                 SPADES = suits[3];

        private final byte ordinal;

        private Suit(int ordinal) {
            this.ordinal = (byte) ordinal;
        }

        static Suit of(Card card) {
            return suits[ordinal(card.offset)];
        }

        /// Every suit.
        public static Stream<Suit> all() {
            return Arrays.stream(suits);
        }

        static int ordinal(int offset) {
            return offset >>> 4;
        }

        /// The zero-based index of this suit in ascending alphabetical order.
        public byte ordinal() {
            return ordinal;
        }

        @Override
        public String toString() {
            return "cdhs".substring(ordinal, ordinal + 1);
        }

        @Override
        public int hashCode() {
            return ordinal;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Suit suit && ordinal == suit.ordinal;
        }
    }

    /// Aligns the ranks for each suit on the 16-bit subwords in a 64-bit integer.
    private final byte offset;

    private Card(Rank rank, Suit suit) {
        this.offset = (byte) (offset(suit.ordinal) + rank.ordinal);
    }

    private static final Card[] cards = Rank.all().flatMap(
        rank -> Suit.all().map(suit -> new Card(rank, suit))
    ).toArray(Card[]::new);

    static Card of(int rank, int suit) {
        return cards[suit + (rank << 2)];
    }

    static Card of(int offset) {
        return Card.of(
            checkIndex(Rank.ordinal(offset), 13),
            checkIndex(Suit.ordinal(offset), 4)
        );
    }

    private static final Collector<Card,?,Hand> COLLECTOR = Collector.of(
        Hand::empty,
        Hand::add,
        (first, second) -> {
            Hand hand = first;
            for (Card card : second) {
                hand = hand.add(card);
            }
            return hand;
        },
        UNORDERED
    );

    /// Accepts cards and makes a hand out of them.
    public static Collector<Card,?,Hand> toHand() {
        return COLLECTOR;
    }

    /// Every card, ascending by suit alphabetically and then by rank.
    public static Stream<Card> all() {
        return Suit.all().flatMap(
            suit -> Rank.all().map(rank -> rank.of(suit))
        );
    }

    static String string(Stream<Card> cards) {
        return cards.map(Card::toString).collect(joining(",", "(", ")"));
    }

    static int ordinal(int offset) {
        return Rank.ordinal(offset) + Suit.ordinal(offset) * 13;
    }

    /// Calculate offsets in 16-bit steps.
    static int offset(int n) {
        return n << 4;
    }

    /// The rank of this card.
    public Rank rank() {
        return Rank.of(this);
    }

    /// The suit of this card.
    public Suit suit() {
        return Suit.of(this);
    }

    public byte offset() {
        return offset;
    }

    public long mask() {
        return 1L << offset;
    }

    @Override
    public String toString() {
        return rank().toString() + suit().toString();
    }

    @Override
    public int hashCode() {
        return ordinal(offset);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Card card && card.offset == offset;
    }

    boolean in(long cards) {
        return (cards & this.mask()) != 0;
    }
}
