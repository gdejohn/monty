package io.github.gdejohn.monty;

import java.util.Arrays;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.Rank.ranks;
import static io.github.gdejohn.monty.Card.Suit.suits;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

public record Card(Rank rank, Suit suit) {
    static sealed abstract class Cards permits Board, Pocket {
        final Card[] cards;

        protected Cards(Card[] cards) {
            this.cards = Arrays.copyOf(cards, cards.length);
            if (Long.bitCount(pack()) != cards.length) {
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
            return stream(cards);
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

        private static final Rank[] ranks = Rank.values();

        private final String string;

        Rank(String string) {
            this.string = string;
        }

        static Rank unpack(int rank) {
            return ranks[Integer.numberOfTrailingZeros(rank)];
        }

        public static Stream<Rank> ranks() {
            return stream(ranks);
        }

        @Override
        public String toString() {
            return string;
        }

        public Card of(Suit suit) {
            return card(this, suit);
        }

        int pack() {
            return 1 << ordinal();
        }
    }

    public enum Suit {
        CLUBS("c"),
        DIAMONDS("d"),
        HEARTS("h"),
        SPADES("s");

        private static final Suit[] suits = Suit.values();
        
        private final String string;

        Suit(String string) {
            this.string = string;
        }

        public static Stream<Suit> suits() {
            return stream(suits);
        }

        @Override
        public String toString() {
            return string;
        }
    }

    private static final Card[] cards = suits().flatMap(
        suit -> ranks().map(rank -> new Card(rank, suit))
    ).toArray(Card[]::new);

    private static int ordinal(Rank rank, Suit suit) {
        return rank.ordinal() + (suit.ordinal() * 13);
    }

    static Card unpack(long card) {
        return cards[Long.numberOfTrailingZeros(card)];
    }

    static Card card(Rank rank, Suit suit) {
        return cards[Card.ordinal(rank, suit)];
    }

    public static Stream<Card> cards() {
        return stream(cards);
    }

    public Rank rank() {
        return rank;
    }

    public Suit suit() {
        return suit;
    }

    @Override
    public String toString() {
        return rank.string + suit.string;
    }

    int ordinal() {
        return Card.ordinal(rank, suit);
    }

    long pack() {
        return 1L << ordinal();
    }
}
