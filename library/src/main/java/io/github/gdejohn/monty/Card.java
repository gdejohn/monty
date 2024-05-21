package io.github.gdejohn.monty;

import java.util.Arrays;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Hand.offset;
import static java.util.Objects.checkIndex;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.range;

public final class Card {
    public static final class Rank implements Comparable<Rank> {
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

        private final byte ordinal;

        private Rank(byte ordinal) {
            this.ordinal = ordinal;
        }

        public Rank(int ordinal) {
            this((byte) checkIndex(ordinal, 13));
        }

        static Rank unpack(int rank) {
            return ranks[Integer.numberOfTrailingZeros(rank)];
        }

        public static Stream<Rank> every() {
            return Arrays.stream(ranks);
        }

        public byte ordinal() {
            return ordinal;
        }

        @Override
        public int compareTo(Rank rank) {
            return ordinal - rank.ordinal; // implicitly widened, can't overflow
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Rank rank && ordinal == rank.ordinal;
        }

        @Override
        public int hashCode() {
            return ordinal;
        }

        @Override
        public String toString() {
            return "23456789TJQKA".substring(ordinal, ordinal + 1);
        }

        public Card of(Suit suit) {
            return cards[Card.ordinal(this.ordinal, suit.ordinal)];
        }
    }

    public static final class Suit {
        private static final Suit[] suits = range(0, 4).mapToObj(Suit::new).toArray(Suit[]::new);

        public static final Suit CLUBS = suits[0],
                                 DIAMONDS = suits[1],
                                 HEARTS = suits[2],
                                 SPADES = suits[3];

        private final byte ordinal;

        private Suit(byte ordinal) {
            this.ordinal = ordinal;
        }

        public Suit(int ordinal) {
            this((byte) checkIndex(ordinal, 4));
        }

        public static Stream<Suit> every() {
            return Arrays.stream(suits);
        }

        /** Method reference target for alphabetical comparator. */
        public static int compare(Suit first, Suit second) {
            return first.ordinal - second.ordinal();
        }

        public byte ordinal() {
            return ordinal;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof Suit suit && ordinal == suit.ordinal;
        }

        @Override
        public int hashCode() {
            return ordinal;
        }

        @Override
        public String toString() {
            return "cdhs".substring(ordinal, ordinal + 1);
        }
    }

    /** Aligns the ranks for each suit on the 16-bit subwords in a 64-bit integer. */
    final byte offset;

    private Card(byte offset) {
        this.offset = offset;
    }

    public Card(Rank rank, Suit suit) {
        this((byte) (rank.ordinal + offset(suit.ordinal)));
    }

    private static final Card[] cards = Suit.every().flatMap(
        suit -> Rank.every().map(rank -> new Card(rank, suit))
    ).toArray(Card[]::new);

    static Card unpack(long card) {
        return cards[Long.numberOfTrailingZeros(card)];
    }

    public static Stream<Card> every() {
        return Arrays.stream(cards);
    }

    static Stream<Card> stream(long cards) {
        return LongStream.iterate(
            cards,
            packed -> packed != 0,
            packed -> packed & (packed - 1)
        ).mapToObj(Card::unpack);
    }

    static String toString(Stream<Card> cards) {
        return cards.map(Card::toString).collect(joining(",", "(", ")"));
    }

    private static int ordinal(int rank, int suit) {
        return rank + (suit * 13);
    }

    public Rank rank() {
        return Rank.ranks[offset & 0b1111];
    }

    public Suit suit() {
        return Suit.suits[offset >>> 4];
    }

    public int ordinal() {
        return ordinal(rank().ordinal, suit().ordinal);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Card card && card.offset == offset;
    }

    @Override
    public int hashCode() {
        return ordinal();
    }

    @Override
    public String toString() {
        return rank().toString() + suit().toString();
    }
}
