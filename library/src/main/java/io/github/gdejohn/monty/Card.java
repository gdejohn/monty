package io.github.gdejohn.monty;

import java.util.Arrays;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.Rank.RANKS;
import static io.github.gdejohn.monty.Card.Suit.SUITS;
import static java.util.Objects.checkIndex;
import static java.util.stream.Collector.Characteristics.UNORDERED;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

/// A playing card with one of thirteen [ranks][Rank] and one of four [suits][Suit].
public final class Card {
    /// The rank of a [card][Card].
    public static final class Rank implements Comparable<Rank> {
        /// Cached instances of every rank.
        static final Rank[] RANKS = range(0, 13).mapToObj(Rank::new).toArray(Rank[]::new);

        public static final Rank TWO = RANKS[0],
                               THREE = RANKS[1],
                                FOUR = RANKS[2],
                                FIVE = RANKS[3],
                                 SIX = RANKS[4],
                               SEVEN = RANKS[5],
                               EIGHT = RANKS[6],
                                NINE = RANKS[7],
                                 TEN = RANKS[8],
                                JACK = RANKS[9],
                               QUEEN = RANKS[10],
                                KING = RANKS[11],
                                 ACE = RANKS[12];

        private final int ordinal;

        /// Make a rank for the given ordinal.
        private Rank(int ordinal) {
            this.ordinal = ordinal;
        }

        /// Extract the rank ordinal from the given card offset.
        static int ordinal(int offset) {
            return offset & 0b1111;
        }

        /// Every rank.
        public static Stream<Rank> all() {
            return Arrays.stream(RANKS);
        }

        /// A card with this rank and the given `suit`.
        public Card of(Suit suit) {
            return Card.of(this.ordinal, suit.ordinal);
        }

        /// The zero-based index of this rank in ascending order.
        public int ordinal() {
            return ordinal;
        }

        @Override
        public int compareTo(Rank rank) {
            return Integer.compare(this.ordinal, rank.ordinal);
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Rank rank && rank.ordinal == this.ordinal;
        }

        @Override
        public int hashCode() {
            return ordinal;
        }

        @Override
        public String toString() {
            return String.valueOf("23456789TJQKA".charAt(ordinal));
        }
    }

    /// The suit of a [card][Card].
    public static final class Suit {
        /// Cached instances of every suit.
        static final Suit[] SUITS = range(0, 4).mapToObj(Suit::new).toArray(Suit[]::new);

        /// ♣
        public static final Suit CLUBS = SUITS[0];

        /// ♦
        public static final Suit DIAMONDS = SUITS[1];

        /// ♥
        public static final Suit HEARTS = SUITS[2];

        /// ♠
        public static final Suit SPADES = SUITS[3];

        private final int ordinal;

        /// Make a suit for the given ordinal.
        private Suit(int ordinal) {
            this.ordinal = ordinal;
        }

        /// Extract the suit ordinal from the given card offset.
        static int ordinal(int offset) {
            return offset >>> 4;
        }

        /// Every suit.
        public static Stream<Suit> all() {
            return Arrays.stream(SUITS);
        }

        /// The zero-based index of this suit in ascending alphabetical order.
        public int ordinal() {
            return ordinal;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Suit suit && suit.ordinal == this.ordinal;
        }

        @Override
        public int hashCode() {
            return ordinal;
        }

        @Override
        public String toString() {
            return String.valueOf("cdhs".charAt(ordinal));
        }
    }

    /// Aligns ranks grouped by suit on 16-bit subwords in a 64-bit integer.
    final byte offset;

    /// Make a card with the given rank and suit.
    private Card(Rank rank, Suit suit) {
        this.offset = (byte) (offset(suit.ordinal) + rank.ordinal);
    }

    /// Cached instances of every card.
    static final Card[] CARDS = Rank.all().flatMap(
        rank -> Suit.all().map(suit -> new Card(rank, suit))
    ).toArray(Card[]::new);

    /// Make a card with the given rank ordinal and suit ordinal.
    static Card of(int rank, int suit) {
        return CARDS[(rank << 2) + suit];
    }

    /// Extract the lowest card from the given bit vector.
    static Card lowest(long cards) {
        int offset = Long.numberOfTrailingZeros(cards);
        return Card.of(
            checkIndex(Rank.ordinal(offset), 13),
            checkIndex(Suit.ordinal(offset), 4)
        );
    }

    /// Every card, ascending by suit alphabetically and then by rank.
    public static Stream<Card> all() {
        return Suit.all().flatMap(
            suit -> Rank.all().map(rank -> rank.of(suit))
        );
    }

    private static final Collector<Card,?,Hand> COLLECTOR = Collector.of(
        () -> new Object() {
            private Hand hand = Hand.empty();
        },
        (result, card) -> result.hand = result.hand.add(card),
        (first, second) -> {
            for (Card card : second.hand) {
                first.hand = first.hand.add(card);
            }
            return first;
        },
        result -> result.hand,
        UNORDERED
    );

    /// Collect cards into a hand.
    public static Collector<Card,?,Hand> toHand() {
        return COLLECTOR;
    }

    static String string(Stream<Card> cards) {
        return cards.map(Card::toString).collect(joining(",", "(", ")"));
    }

    static int ordinal(int offset) {
        return Rank.ordinal(offset) + (Suit.ordinal(offset) * 13);
    }

    /// Calculate offsets in 16-bit steps.
    static int offset(int n) {
        return n << 4;
    }

    /// The bit vector representation of this card.
    long mask() {
        return 1L << offset;
    }

    /// The rank of this card.
    public Rank rank() {
        return RANKS[Rank.ordinal(offset)];
    }

    /// The suit of this card.
    public Suit suit() {
        return SUITS[Suit.ordinal(offset)];
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Card card && card.offset == this.offset;
    }

    @Override
    public int hashCode() {
        return Card.ordinal(offset);
    }

    @Override
    public String toString() {
        return "%s%s".formatted(rank(), suit());
    }
}
