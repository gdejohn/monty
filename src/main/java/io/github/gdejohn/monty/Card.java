package io.github.gdejohn.monty;

import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public record Card(Rank rank, Suit suit) implements Comparable<Card> {
    static sealed abstract class Cards permits Board, Pocket {
        final Card[] cards;

        protected Cards(Card[] cards) {
            this.cards = Arrays.copyOf(cards, cards.length);
            if (Long.bitCount(this.pack()) != cards.length) {
                throw new IllegalArgumentException();
            }
        }

        long pack() {
            var cards = 0L;
            for (var card : this.cards) {
                cards |= card.pack();
            }
            return cards;
        }

        Stream<Card> cards() {
            return Arrays.stream(cards);
        }

        @Override
        public String toString() {
            return Cards.toString(cards());
        }

        static Stream<Card> unpack(long cards) {
            return LongStream.iterate(
                cards,
                packed -> packed != 0,
                packed -> packed & (packed - 1)
            ).mapToObj(Card::unpack);
        }

        static String toString(Stream<Card> cards) {
            return cards.map(Card::toString).collect(joining(",", "(", ")"));
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

        int pack() {
            return 1 << ordinal();
        }
    }

    public enum Suit {
        CLUBS("♣"),
        DIAMONDS("♦"),
        HEARTS("♥"),
        SPADES("♠");

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

        int pack() {
            return 1 << ordinal();
        }
    }

    private static final Card[] values = EnumSet.allOf(Suit.class).stream().flatMap(Suit::cards).toArray(Card[]::new);

    static final long DECK = -1L >>> -52;

    private static int ordinal(Rank rank, Suit suit) {
        return rank.ordinal() + (suit.ordinal() * 13);
    }

    static Card unpack(long card) {
        return Card.values[Long.numberOfTrailingZeros(card)];
    }

    public static Stream<Card> deck() {
        return Arrays.stream(values);
    }

    int ordinal() {
        return Card.ordinal(rank, suit);
    }

    long pack() {
        return 1L << ordinal();
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
        return rank.string + suit.string;
    }
}
