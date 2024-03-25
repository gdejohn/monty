package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Cards;

import static io.github.gdejohn.monty.Category.FLUSH;
import static io.github.gdejohn.monty.Category.FULL_HOUSE;
import static io.github.gdejohn.monty.Category.HIGH_CARD;
import static io.github.gdejohn.monty.Category.PAIR;
import static io.github.gdejohn.monty.Category.QUADS;
import static io.github.gdejohn.monty.Category.STRAIGHT;
import static io.github.gdejohn.monty.Category.STRAIGHT_FLUSH;
import static io.github.gdejohn.monty.Category.TRIPS;
import static io.github.gdejohn.monty.Category.TWO_PAIR;

public final class Hand implements Comparable<Hand> {
    /**
     * A bit vector representing the cards in this hand.
     *
     * <p>There are four blocks of thirteen bits each, one block for each suit.
     * The positions of set bits within a block indicate which ranks occur with
     * the suit represented by that block.
     *
     * <p>The cards {@code [As,Ah,Ad,Jd,Jc,4c,2c]} look like this:
     *
     * <pre>
     *     ┌──spades───┐┌──hearts───┐┌──diamonds─┐┌───clubs───┐
     *   0b1000000000000100000000000010010000000000001000000101
     *     A            A            A  J            J      4 2</pre>
     */
    final long cards;

    /**
     * A bit vector representing the rank frequencies in this hand.
     *
     * <p>There are four blocks of thirteen bits each. The blocks represent the
     * frequency of a rank, increasing from least to most significant bits. The
     * positions of set bits within a block indicate which ranks occur with the
     * frequency represented by that block.
     *
     * <p>The ranks for the cards {@code [As,Ah,Ad,Jd,Jc,4c,2c]} look like
     * this:
     *
     * <pre>
     *     ┌───quads───┐┌───trips───┐┌───pairs───┐┌──kickers──┐
     *   0b0000000000000100000000000000010000000000000000000101
     *                  A               J                   4 2</pre>
     */
    final long ranks;

    private Hand(long cards, long ranks) {
        this.cards = cards;
        this.ranks = ranks;
    }

    private static final Hand EMPTY = new Hand(0L, 0L);

    public static Hand empty() {
        return EMPTY;
    }

    public Hand deal(Card card) {
        var shift = card.rank().ordinal();
        var rank = ranks & (0x4002001L << shift);
        return new Hand(
            cards | card.pack(),
            ranks ^ rank | (rank << 13) | ((rank - 1 >>> -1) << shift)
        );
    }

    public Category category() {
        return Category.unpack(evaluate());
    }

    @Override
    public String toString() {
        return Cards.toString(category().cards(this));
    }

    @Override
    public int hashCode() {
        return evaluate();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Hand hand && this.evaluate() == hand.evaluate();
    }

    /**
     * Compare this hand to a given hand.
     *
     * @return a negative number if this hand loses to the given hand,
     *         zero if this hand and the given hand tie each other,
     *         or a positive number if this hand beats the given hand
     */
    @Override
    public int compareTo(Hand hand) {
        return this.evaluate() - hand.evaluate(); // neither is ever negative
    }

    /**
     * Determine the value of this hand.
     *
     * <p>The returned value represents the equivalence class of this hand and
     * imposes the order of seven-card poker hands.
     */
    public int evaluate() {
        var kickers = (int) ranks & 0x1FFF;
        var pairs = (int) (ranks >>> 13) & 0x1FFF;
        var trips = (int) (ranks >>> 26) & 0x1FFF;
        var quads = (int) (ranks >>> 39);
        var value = values[kickers | pairs | trips | quads];
        var flush = values[(int) cards & 0x1FFF] // clubs
                  | values[(int) (cards >>> 13) & 0x1FFF] // diamonds
                  | values[(int) (cards >>> 26) & 0x1FFF] // hearts
                  | values[(int) (cards >>> 39)]; // spades
        var straight = (value & (flush - 1)) >>> -3;
        return switch (category(pairs, trips, quads, (flush | -flush) >>> -3, straight)) {
            case 0b1111 -> STRAIGHT_FLUSH.pack(-flush);
            case 0b1110 -> FLUSH.pack(flush);
            case 0b0111 -> STRAIGHT.pack(-value);
            case 0b0000 -> HIGH_CARD.pack(value); // 1+1+1+1+1+1+1
            case 0b0001 -> PAIR.pack(pairs, drop(drop(kickers))); // 2+1+1+1+1+1
            case 0b0011 -> TWO_PAIR.pack(pairs, drop(drop(kickers))); // 2+2+1+1+1
            case 0b0010 -> TWO_PAIR.pack(drop(pairs), drop(kickers | last(pairs))); // 2+2+2+1
            case 0b1000 -> TRIPS.pack(trips, drop(drop(kickers))); // 3+1+1+1+1
            case 0b1001 -> FULL_HOUSE.pack(trips, pairs); // 3+2+1+1
            case 0b1011 -> FULL_HOUSE.pack(trips, drop(pairs)); // 3+2+2
            case 0b1010 -> FULL_HOUSE.pack(drop(trips), last(trips)); // 3+3+1
            case 0b0100 -> QUADS.pack(quads, drop(drop(kickers))); // 4+1+1+1
            case 0b0101 -> QUADS.pack(quads, drop(kickers | pairs)); // 4+2+1
            case 0b1100 -> QUADS.pack(quads, trips); // 4+3
            default -> throw new IllegalStateException();
        };
    }

    /**
     * Compute a selector expression that determines the hand's category.
     */
    private static int category(int pairs, int trips, int quads, int flush, int straight) {
        return (sign(trips) & ~straight) << 3
             | sign(quads) << 2
             | (flush | sign(drop(trips)) | sign(drop(pairs))) << 1
             | straight | (sign(pairs) & ~flush) ^ sign(drop(drop(pairs)));
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

    /**
     * @return 1 if {@code n} is positive, otherwise 0
     */
    private static int sign(int n) {
        return -n >>> -1;
    }

    // 2 bytes * 8129, fits in L1 data cache
    private static final short[] values = new short[0b1111111000001];

    static {
        // map each hand with 5, 6, or 7 distinct ranks to its 5 highest ranks
        for (var a = 1; a < 1 << 9; a <<= 1)
            for (var b = a; b < 1 << 9; b <<= 1)
                for (var c = b; c < 1 << 9; c <<= 1)
                    for (var d = c << 1; d < 1 << 10; d <<= 1)
                        for (var e = d << 1; e < 1 << 11; e <<= 1)
                            for (var f = e << 1; f < 1 << 12; f <<= 1)
                                for (var g = f << 1; g < 1 << 13; g <<= 1)
                                    values[a | b | c | d | e | f | g] = (short) (c | d | e | f | g);

        // map each straight from 6-high through ace-high to its high rank, negated
        for (var ranks = 0b0000000011111; ranks < 1 << 13; ranks <<= 1) {
            var straight = last(ranks) << 4;

            // every way of choosing at most two ranks below the straight
            for (var a = 1; a < straight >>> 3; a <<= 1)
                for (var b = a << 1; b <= straight >>> 3; b <<= 1)
                    values[ranks | b | a] = (short) -straight;

            // every way of choosing at most one below and exactly one above
            for (var a = 1; a < straight >>> 3; a <<= 1)
                for (var b = straight << 2; b < 1 << 13; b <<= 1)
                    values[b | ranks | a] = (short) -straight;

            // every way of choosing exactly two above
            for (var a = straight << 2; a < 1 << 12; a <<= 1)
                for (var b = a << 1; b < 1 << 13; b <<= 1)
                    values[b | a | ranks] = (short) -straight;
        }

        var ranks = 0b1000000001111; // A,5,4,3,2
        var wheel = 0b0000000001000; // 5-high straight

        // every way of choosing at most two ranks above the wheel
        for (var a = wheel << 2; a < 1 << 13; a <<= 1)
            for (var b = a; b < 1 << 13; b <<= 1)
                values[b | a | ranks] = (short) -wheel;
    }
}
