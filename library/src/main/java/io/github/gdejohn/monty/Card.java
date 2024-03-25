package io.github.gdejohn.monty;

import java.util.Arrays;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.Rank.ranks;
import static io.github.gdejohn.monty.Card.Suit.suits;
import static java.util.Arrays.stream;
import static java.util.Objects.checkIndex;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

public record Card(Rank rank, Suit suit, byte ordinal) {
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

    public record Rank(byte ordinal) implements Comparable<Rank> {
        private static final Rank[] ranks = range(0, 13).mapToObj(Rank::new).toArray(Rank[]::new);

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

        public Rank {
            checkIndex(ordinal, 13);
        }

        private Rank(int ordinal) {
            this((byte) ordinal);
        }

        static Rank unpack(int rank) {
            return ranks[Integer.numberOfTrailingZeros(rank)];
        }

        public static Stream<Rank> ranks() {
            return stream(ranks);
        }

        @Override
        public int compareTo(Rank that) {
            return Byte.compare(this.ordinal, that.ordinal);
        }

        @Override
        public String toString() {
            return "23456789TJQKA".substring(this.ordinal, this.ordinal + 1);
        }

        public Card of(Suit suit) {
            return cards[Card.ordinal(this, suit)];
        }
    }

    public record Suit(byte ordinal) implements Comparable<Suit> {
        private static final Suit[] suits = range(0, 4).mapToObj(Suit::new).toArray(Suit[]::new);

        public static final Suit CLUBS = suits[0],
                                 DIAMONDS = suits[1],
                                 HEARTS = suits[2],
                                 SPADES = suits[3];

        public Suit {
            checkIndex(ordinal, 4);
        }

        private Suit(int ordinal) {
            this((byte) ordinal);
        }

        public static Stream<Suit> suits() {
            return stream(suits);
        }

        @Override
        public int compareTo(Suit that) {
            return Byte.compare(this.ordinal, that.ordinal);
        }

        @Override
        public String toString() {
            return "cdhs".substring(this.ordinal, this.ordinal + 1);
        }
    }

    private static byte ordinal(Rank rank, Suit suit) {
        return (byte) (rank.ordinal + (suit.ordinal * 13));
    }

    private static final Card[] cards = suits().flatMap(
        suit -> ranks().map(rank -> new Card(rank, suit, ordinal(rank, suit)))
    ).toArray(Card[]::new);

    static Card unpack(long card) {
        return cards[Long.numberOfTrailingZeros(card)];
    }

    public static Stream<Card> cards() {
        return stream(cards);
    }

    public Card {
        if (ordinal != ordinal(rank, suit)) {
            throw new IllegalArgumentException(
                "ordinal = %d does not represent a valid card".formatted(ordinal)
            );
        }
    }

    @Override
    public String toString() {
        return rank.toString() + suit.toString();
    }

    long pack() {
        return 1L << ordinal;
    }
}
