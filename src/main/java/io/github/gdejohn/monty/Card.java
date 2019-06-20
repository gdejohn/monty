package io.github.gdejohn.monty;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;

public final class Card implements Comparable<Card> {
    public static final class Board {
        private final long cards;

        private Board(long cards) {
            this.cards = cards;
        }

        Board() {
            this(0L);
        }

        long cards() {
            return cards;
        }
    }

    public static final class Pocket {
        private final long cards;

        private Pocket(long cards) {
            this.cards = cards;
        }

        long cards() {
            return cards;
        }
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

    static void count(int n, long cards) {
        if (Long.bitCount(cards) != n) {
            throw new IllegalArgumentException();
        }
    }

    static Card unpack(long card) {
        return Card.values[Long.numberOfTrailingZeros(card)];
    }

    static LongStream combinations() {
        return LongStream.iterate(
            -1L >>> -7,
            cards -> cards < (1L << 52),
            cards -> { // Gosper's hack
                var x = cards & -cards;
                var y = cards + x;
                return y | (((y ^ cards) >> 2) / x);
            }
        );
    }

    static Comparator<Card> reverseLowball() {
        return REVERSE_LOWBALL;
    }

    public static Pocket pocket(Card first, Card second) {
        var cards = first.pack() | second.pack();
        Card.count(2, cards);
        return new Pocket(cards);
    }

    public static Board board(Card first, Card second, Card third) {
        var cards = first.pack() | second.pack() | third.pack();
        Card.count(3, cards);
        return new Board(cards);
    }

    public static Board board(Card first, Card second, Card third, Card fourth) {
        var cards = first.pack() | second.pack() | third.pack() | fourth.pack();
        Card.count(4, cards);
        return new Board(cards);
    }

    public static Board board(Card first, Card second, Card third, Card fourth, Card fifth) {
        var cards = first.pack() | second.pack() | third.pack() | fourth.pack() | fifth.pack();
        Card.count(5, cards);
        return new Board(cards);
    }

    long pack() {
        return 1L << Card.ordinal(rank, suit);
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
