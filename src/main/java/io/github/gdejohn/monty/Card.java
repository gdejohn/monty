package io.github.gdejohn.monty;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;

public final class Card implements Comparable<Card> {
    static sealed abstract class Cards permits Board, Pocket {
        final long[] cards;

        protected Cards(Card[] cards) {
            this.cards = Arrays.stream(cards).mapToLong(
                card -> Monty.pack(card.ordinal())
            ).toArray();
            distinct(cards.length, this.mask());
        }

        long mask() {
            var mask = 0L;
            for (var card : cards) {
                mask |= card & Card.MASK;
            }
            return mask;
        }
    }

    public static final class Board extends Cards {
        static final Board PRE_FLOP = new Board();

        private Board(Card... cards) {
            super(cards);
        }
    }

    public static final class Pocket extends Cards {
        private Pocket(Card... cards) {
            super(cards);
        }
    }

    static long distinct(int count, long cards) {
        if (Long.bitCount(cards) != count) {
            throw new IllegalArgumentException();
        } else {
            return cards;
        }
    }

    static String toString(Stream<Card> cards) {
        return cards.map(Card::toString).collect(joining(",", "(", ")"));
    }

    public enum Rank {
        TWO("2"),
        THREE("3"),
        FOUR("4"),
        FIVE("5"),
        SIX("6"),
        SEVEN("7"),
        EIGHT("8"),
        NINE("9"),
        TEN("T"),
        JACK("J"),
        QUEEN("Q"),
        KING("K"),
        ACE("A");

        static final long MASK = 0b1111L << 52;

        static final int SHIFT = 52;

        static final long COUNTS = 0b0000000000001_0000000000001_0000000000001L;

        private static final Rank[] values = Rank.values();

        private final String string;

        Rank(String string) {
            this.string = string;
        }

        static Rank unpack(int rank) {
            return Rank.values[Integer.numberOfTrailingZeros(rank)];
        }

        @Override
        public String toString() {
            return string;
        }

        public Card of(Suit suit) {
            return Card.values[Card.ordinal(this, suit)];
        }
    }

    public enum Suit {
        CLUBS("♣"),
        DIAMONDS("♦"),
        HEARTS("♥"),
        SPADES("♠");

        static final long MASK = 0b11L << 56;

        static final int SHIFT = 56;

        static final int COUNTS = 0b0001_0001_0001_0001_0001_0001;

        private final String string;

        Suit(String string) {
            this.string = string;
        }

        @Override
        public String toString() {
            return string;
        }

        Stream<Card> cards() {
            return EnumSet.allOf(Rank.class).stream().map(rank -> new Card(rank, this));
        }
    }

    private static final Comparator<Card> REVERSE_LOWBALL = comparingInt(card -> -(card.rank.ordinal() + 1) % 13);

    private static final Card[] values = EnumSet.allOf(Suit.class).stream().flatMap(Suit::cards).toArray(Card[]::new);

    static final long MASK = -1L >>> -52;

    private final Rank rank;

    private final Suit suit;

    private final String string;

    private Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
        this.string = rank.string + suit.string;
    }

    private static int ordinal(Rank rank, Suit suit) {
        return rank.ordinal() + (suit.ordinal() * 13);
    }

    static Stream<Card> unpack(long cards) {
        return LongStream.iterate(
            cards,
            packed -> packed != 0,
            packed -> packed & (packed - 1)
        ).mapToObj(packed -> Card.values[Long.numberOfTrailingZeros(packed)]);
    }

    static Comparator<Card> reverseLowball() {
        return REVERSE_LOWBALL;
    }

    public static Board board(Card first, Card second, Card third) {
        return new Board(first, second, third);
    }

    public static Board board(Card first, Card second, Card third, Card fourth) {
        return new Board(first, second, third, fourth);
    }

    public static Board board(Card first, Card second, Card third, Card fourth, Card fifth) {
        return new Board(first, second, third, fourth, fifth);
    }

    public static Pocket pocket(Card first, Card second) {
        return new Pocket(first, second);
    }

    int ordinal() {
        return Card.ordinal(rank, suit);
    }

    public Rank rank() {
        return rank;
    }

    public Suit suit() {
        return suit;
    }

    @Override
    public int compareTo(Card card) {
        return rank.compareTo(card.rank);
    }

    @Override
    public String toString() {
        return string;
    }
}
