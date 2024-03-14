package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Cards;
import io.github.gdejohn.monty.Card.Rank;
import io.github.gdejohn.monty.Card.Suit;

import static io.github.gdejohn.monty.Category.FLUSH;
import static io.github.gdejohn.monty.Category.FULL_HOUSE;
import static io.github.gdejohn.monty.Category.HIGH_CARD;
import static io.github.gdejohn.monty.Category.PAIR;
import static io.github.gdejohn.monty.Category.QUADS;
import static io.github.gdejohn.monty.Category.STRAIGHT;
import static io.github.gdejohn.monty.Category.STRAIGHT_FLUSH;
import static io.github.gdejohn.monty.Category.TRIPS;
import static io.github.gdejohn.monty.Category.TWO_PAIR;

/**
 * A poker hand evaluator for Texas hold 'em.
 */
public final class Hand implements Comparable<Hand> {
    /**
     * A bit vector representing the cards in this hand.
     *
     * <p>There are four blocks of thirteen bits each, one block for each suit.
     * The positions of set bits within a block indicate which ranks occur with
     * the suit represented by that block.
     */
    final long cards;

    /**
     * A multiset bit vector representing the rank frequencies in this hand.
     *
     * <p>There are four blocks of thirteen bits each. The blocks represent the
     * frequency of a rank, increasing from least to most significant bits. The
     * positions of set bits within a block indicate which ranks occur with the
     * frequency represented by that block.
     */
    final long ranks;

    /**
     * A multiset bit vector representing the suit frequencies in this hand.
     *
     * <p>There are seven blocks of four bits each. The blocks represent the
     * frequency of a suit from one through seven, increasing from least to
     * most significant bits. The positions of set bits within a block indicate
     * which suits occur with the frequency represented by that block.
     */
    final int suits;

    private Hand(long cards, long ranks, int suits) {
        this.cards = cards;
        this.ranks = ranks;
        this.suits = suits;
    }

    private static final Hand EMPTY = new Hand(0L, 0L, 0);

    public static Hand empty() {
        return EMPTY;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Hand that && this.evaluate() == that.evaluate();
    }

    @Override
    public int hashCode() {
        return evaluate();
    }

    @Override
    public String toString() {
        return Cards.toString(category().cards(this));
    }

    @Override
    public int compareTo(Hand hand) {
        return Integer.compare(this.evaluate(), hand.evaluate());
    }

    public Category category() {
        return Category.unpack(evaluate());
    }

    public Hand deal(Card card) {
        return new Hand(cards | card.pack(), add(card.rank()), add(card.suit()));
    }

    /**
     * Increment the count of the given rank.
     */
    private long add(Rank rank) {
        var count = ranks & (0x4002001L << rank.ordinal());
        return ranks ^ count ^ (count << 13) ^ (((count - 1) >>> 63) << rank.ordinal());
    }

    /**
     * Increment the count of the given suit.
     */
    private int add(Suit suit) {
        var count = suits & (0x111111 << suit.ordinal());
        return suits ^ count ^ (count << 4) ^ (((count - 1) >>> 31) << suit.ordinal());
    }

    /**
     * Determine the equivalence class of this poker hand.
     *
     * <p>Given two hands {@code x} and {@code y}, {@code x.evaluate()} is less
     * than, equal to, or greater than {@code y.evaluate()} if and only if
     * {@code x} is beaten by, ties, or beats {@code y}, respectively.
     */
    public int evaluate() {
        var kickers = (int) ranks & 0x1FFF;
        var pairs = (int) (ranks >>> 13) & 0x1FFF;
        var trips = (int) (ranks >>> 26) & 0x1FFF;
        var quads = (int) (ranks >>> 39);
        var suit = (int) (cards >>> shift(suits >>> 16)) & 0x1FFF;
        var value = value(kickers, pairs, trips, quads, suit);
        return switch (value >>> 13) {
            case 0b0000 -> HIGH_CARD.pack(value & 0x1FFF); // 1,1,1,1,1,1,1
            case 0b0001 -> PAIR.pack((pairs << 13) | drop(drop(kickers))); // 2,1,1,1,1,1
            case 0b0011 -> TWO_PAIR.pack((pairs << 13) | drop(drop(kickers))); // 2,2,1,1,1
            case 0b0010 -> TWO_PAIR.pack((drop(pairs) << 13) | drop(kickers | last(pairs))); // 2,2,2,1
            case 0b0100 -> TRIPS.pack((trips << 13) | drop(drop(kickers))); // 3,1,1,1,1
            case 0b0101 -> FULL_HOUSE.pack((trips << 13) | pairs); // 3,2,1,1
            case 0b0111 -> FULL_HOUSE.pack((trips << 13) | drop(pairs)); // 3,2,2
            case 0b0110 -> FULL_HOUSE.pack((drop(trips) << 13) | last(trips)); // 3,3,1
            case 0b1000 -> QUADS.pack((quads << 13) | drop(drop(kickers))); // 4,1,1,1
            case 0b1001 -> QUADS.pack((quads << 13) | drop(kickers | pairs)); // 4,2,1
            case 0b1100 -> QUADS.pack((quads << 13) | trips); // 4,3
            case 0b1011 -> STRAIGHT.pack(value & 0x1FFF);
            case 0b1110 -> FLUSH.pack(value & 0x1FFF);
            case 0b1111 -> STRAIGHT_FLUSH.pack(value & 0x1FFF);
            default -> throw new IllegalStateException();
        };
    }

    /**
     * A bit vector representing straights, kickers, and this hand's category.
     *
     * <p>The thirteen lowest bits contain the high rank of the straight if a
     * straight exists, or the five highest ranks if there are at least five
     * ranks. The next four bits encode a selector expression that determines
     * the category of this hand.
     */
    private static int value(int kickers, int pairs, int trips, int quads, int suit) {
        var ranks = kickers | pairs | trips | quads;
        var flush = sign(suit);
        var value = values[(ranks ^ ((ranks ^ suit) & (0x2000 - flush)))];
        var straight = sign(value) ^ sign(drop(value));
        var a = (flush | straight | sign(quads)) << 3;
        var b = (flush | (sign(trips) & ~straight)) << 2;
        var c = (flush | straight | sign(drop(trips)) | sign(drop(pairs))) << 1;
        var d = straight | (sign(pairs) & ~flush);
        var e = sign(drop(drop(pairs)));
        return ((a | b | c | d - e) << 13) | value;
    }

    /**
     * Look up the shift value needed to extract the flush ranks if a flush exists.
     */
    private static int shift(int suits) {
        return shifts[(suits * -0x4D700001) >>> 28];
    }

    /**
     * Return 1 if {@code n} is positive, otherwise 0.
     */
    private static int sign(int n) {
        return -n >>> 63;
    }

    /**
     * Clear the least significant set bit.
     */
    private static int drop(int n) {
        return n & (n - 1);
    }

    /**
     * Clear all bits except for the least significant set bit.
     */
    private static int last(int n) {
        return n & -n;
    }

    private static final byte[] shifts = {52, 13, 0, 26, 39, 13, 13, 39, 0, 39, 26, 0, 26};

    private static final short[] values = values();

    private static short[] values() {
        var values = new short[8129];
        for (var a = 1; a < 1 << 9; a <<= 1)
            for (var b = a << 1; b < 1 << 10; b <<= 1)
                for (var c = b << 1; c < 1 << 11; c <<= 1)
                    for (var d = c << 1; d < 1 << 12; d <<= 1)
                        for (var e = d << 1; e < 1 << 13; e <<= 1)
                            values[a | b | c | d | e] = (short) (a | b | c | d | e);

        for (var a = 1; a < 1 << 8; a <<= 1)
            for (var b = a << 1; b < 1 << 9; b <<= 1)
                for (var c = b << 1; c < 1 << 10; c <<= 1)
                    for (var d = c << 1; d < 1 << 11; d <<= 1)
                        for (var e = d << 1; e < 1 << 12; e <<= 1)
                            for (var f = e << 1; f < 1 << 13; f <<= 1)
                                values[a | b | c | d | e | f] = (short) (b | c | d | e | f);

        for (var a = 1; a < 1 << 9; a <<= 1)
            for (var b = a << 1; b < 1 << 10; b <<= 1)
                for (var c = b << 1; c < 1 << 11; c <<= 1)
                    for (var d = c << 1; d < 1 << 12; d <<= 1)
                        for (var e = d << 1; e < 1 << 13; e <<= 1)
                            for (var f = e << 1; f < 1 << 13; f <<= 1)
                                for (var g = f << 1; g < 1 << 13; g <<= 1)
                                    values[a | b | c | d | e | f | g] = (short) (c | d | e | f | g);

        var wheel = values[0b1000000001111] = 0b0000000001000;
        for (var a = 0b0000000100000; a < 0b1000000000000; a <<= 1)
            for (var b = a << 1; b <= 0b1000000000000; b <<= 1)
                values[0b1000000001111 | a | b] = wheel;

        for (var ranks = 0b0000000011111; ranks <= 0b1111100000000; ranks <<= 1) {
            var straight = (short) (last(ranks) << 4);

            for (var a = 0b0000000000001; a < straight; a <<= 1)
                for (var b = a << 1; b < straight; b <<= 1)
                    values[ranks | a | b] = straight;

            for (var a = 0b0000000000001; a < straight; a <<= 1)
                for (var b = straight << 2; b <= 0b1000000000000; b <<= 1)
                    values[ranks | a | b] = straight;

            for (var a = straight << 2; a <= 0b0100000000000; a <<= 1)
                for (var b = a << 1; b <= 0b1000000000000; b <<= 1)
                    values[ranks | a | b] = straight;
        }
        return values;
    }
}
