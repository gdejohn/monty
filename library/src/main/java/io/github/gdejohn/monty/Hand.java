package io.github.gdejohn.monty;

/** A seven-card poker hand evaluator for Texas hold 'em. */
public final class Hand {
    /**
     * A bit vector representing the cards in this hand.
     * <p>
     * There are four 16-bit blocks, one for each suit. The positions of 1-bits in the 13
     * low-order bits of a block indicate which ranks occur with the suit represented by that
     * block. The three high-order bits in each block are unused.
     * <p>
     * The cards {@code [As,Ah,Ad,Jd,Jc,4c,2c]} are represented like this:
     * <pre>
     *       ┌──spades──────┐┌──hearts──────┐┌──diamonds────┐┌──clubs───────┐
     *     0b***1000000000000***1000000000000***1001000000000***0001000000101
     *          A               A               A  J               J      4 2</pre>
     *
     * @see #offset
     * @see #slice
     */
    final long cards;

    /**
     * A bit vector representing the rank frequencies in this hand.
     * <p>
     * There are four 16-bit blocks. The blocks represent the frequency of a rank, increasing from
     * least to most significant bits. The positions of 1-bits in the 13 low-order bits of a block
     * indicate which ranks occur with the frequency represented by that block. The three
     * high-order bits in each block are unused.
     * <p>
     * The ranks for the cards {@code [As,Ah,Ad,Jd,Jc,4c,2c]} are represented like this:
     * <pre>
     *       ┌──quads───────┐┌──trips───────┐┌──pairs───────┐┌──kickers─────┐
     *     0b***0000000000000***1000000000000***0001000000000***0000000000101
     *                          A                  J                      4 2</pre>
     *
     * @see #offset
     * @see #slice
     * @see #COUNT
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

    public static Hand hand(Card... cards) {
        var hand = Hand.empty();
        for (var card : cards) {
            hand = hand.add(card);
        }
        if (hand.size() != cards.length) {
            throw new IllegalArgumentException("duplicate cards");
        }
        return hand;
    }

    long cards() {
        return (long) slice(cards, 0)
             | (long) slice(cards, 1) << 13
             | (long) slice(cards, 2) << 26
             | (long) slice(cards, 3) << 39;
    }

    public int size() {
        return Long.bitCount(cards);
    }

    public Category category() {
        return Category.unpack(evaluate());
    }

    @Override
    public String toString() {
        return Card.toString(category().sort(this));
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Hand hand && hand.cards == cards;
    }

    /** Minimal perfect hash function based on combinatorial number system. */
    @Override
    public int hashCode() {
        var rank = 0;
        var cards = cards();
        for (var k = 0; cards != 0; k++) {
            var n = Long.numberOfTrailingZeros(cards);
            rank += choose[k][n];
            cards &= cards - 1;
        }
        return rank; // index of these cards in lexicographic order
    }

    private static final int[][] choose = choose();

    private static int[][] choose() {
        var choose = new int[7][52];
        for (int n = 0; n < 52; n++) {
            choose[0][n] = n;
        }
        for (int k = 1; k < 7; k++) {
            for (int n = k; n < 52; n++) {
                choose[k][n] = choose[k - 1][n - 1] + choose[k][n - 1];
            }
        }
        return choose;
    }

    private static int rank(Card card) {
        return card.offset & 0b1111;
    }

    /** Calculate offsets in 16-bit steps. */
    static int offset(int n) {
        return n << 4;
    }

    /** A sliding mask that extracts frequencies from {@link #ranks}. */
    private static final long COUNT = 1L << offset(2)  // trips
                                    | 1L << offset(1)  // pairs
                                    | 1L << offset(0); // kickers

    /**
     * Make a new hand containing the given card and this hand's cards.
     * <p>
     * Hands with exactly seven cards can be {@link #evaluate evaluated}. Intermediate hands are
     * persistent and represent partial evaluations that can be reused for the community cards
     * shared by every player.
     */
    public Hand add(Card card) {
        long rank = ranks & COUNT << rank(card);
        var hand = new Hand(
            cards | 1L << card.offset,
            ranks ^ rank | rank << offset(1) | (rank - 1 >>> -1) << rank(card)
        );
        assert hand.cards != cards : "duplicate card";
        return hand;
    }

    /**
     * Determine the value of this hand.
     * <p>
     * The value is an unsigned 30-bit integer representing the equivalence class of the best
     * five-card hand that can be made from the seven given cards, computed directly without
     * checking each of the 21 combinations (7 choose 5). Hands are ordered by their values: a
     * hand with a greater value beats a hand with a lesser value, two hands with equal values tie
     * each other.
     * <p>
     * There are 7,462 equivalence classes for five-card hands, but only 4,824 equivalence classes
     * are possible when making the best five-card hand from seven cards. (For example, given the
     * five cards [5s,5h,4h,3h,2h], there is no way of choosing two more cards from the rest of
     * the deck such that neither is included in the best five-card hand that can be made from
     * those seven cards.) Hand values represent equivalence classes using the first 13 low-order
     * bits for less significant ranks, the next 13 higher-order bits for more significant ranks
     * (possibly empty), and the last four high-order bits for the category of the hand.
     * <p>
     * Hands are evaluated by first hashing them to a signed 5-bit integer ranging from -2 to 15
     * (inclusive), constructed from the sign bits of values derived from the subwords of
     * {@link #cards} and {@link #ranks}. The hash function encodes the rank frequencies and the
     * presence of straights and flushes, determining the category of the hand and the
     * significance of each distinct rank. This partitions all 133,784,560 seven-card hands (52
     * choose 7) into 14 buckets such that every hand in each bucket can be evaluated with the
     * same expression, which is selected by a tableswitch on the hash code.
     */
    public int evaluate() {
        assert Long.bitCount(cards) == 7 : "too few cards, or too many";
        int kickers = slice(ranks, 0),
              pairs = slice(ranks, 1),
              trips = slice(ranks, 2),
              quads = slice(ranks, 3),
              value = values[kickers | pairs | trips | quads],
              flush = values[slice(cards, 0)]  // clubs
                    | values[slice(cards, 1)]  // diamonds
                    | values[slice(cards, 2)]  // hearts
                    | values[slice(cards, 3)], // spades
           category = ((-pairs ^ -drop(drop(pairs))) >>> -1)   << 3
                    | (-(drop(pairs) | drop(trips)) >>> -1)    << 2
                    | (-trips >>> -1 | (flush | -flush) >> -1) << 1
                    | (-quads >>> -1 | (value & flush - 1) >>> -4);
        return switch (category) {
            case +0b0000 -> evaluate(0, value); // high card
            case +0b1000 -> evaluate(1, pairs, drop(drop(kickers))); // one pair
            case +0b1100 -> evaluate(2, pairs, drop(drop(kickers))); // two pair
            case +0b0100 -> evaluate(2, drop(pairs), drop(last(pairs) | kickers)); // two pair
            case +0b0010 -> evaluate(3, trips, drop(drop(kickers))); // three of a kind
            case +0b1111 -> evaluate(4, ~value); // straight
            case ~0b0001 -> evaluate(5, flush); // flush
            case +0b1010 -> evaluate(6, trips, pairs); // full house
            case +0b1110 -> evaluate(6, trips, drop(pairs)); // full house
            case +0b0110 -> evaluate(6, drop(trips), last(trips)); // full house
            case +0b0001 -> evaluate(7, quads, drop(drop(kickers))); // four of a kind
            case +0b1001 -> evaluate(7, quads, drop(pairs | kickers)); // four of a kind
            case +0b0011 -> evaluate(7, quads, trips); // four of a kind
            case ~0b0000 -> evaluate(8, ~flush); // straight flush
            default -> throw new AssertionError("invalid hand");
        };
    }

    /** Extract the nth 16-bit subword. */
    private static short slice(long word, int n) {
        return (short) (word >>> offset(n));
    }

    /** Flip the least significant 1-bit. */
    private static int drop(int ranks) {
        return ranks & ranks - 1;
    }

    /** Flip every 1-bit except for the least significant one. */
    private static int last(int ranks) {
        return ranks & -ranks;
    }

    /** Pack the category and ranks together. */
    private static int evaluate(int category, int ranks) {
        return category << 26 | ranks;
    }

    /** Pack the category, high-order ranks, and low-order ranks together. */
    private static int evaluate(int category, int high, int low) {
        return evaluate(category, high << 13 | low);
    }

    /**
     * A lookup table that selects the relevant ranks of straights and flushes.
     * <p>
     * Nonzero values are associated with sets of five, six, or seven ranks represented by 13-bit
     * indices with 1-bits at the corresponding positions. A positive value always has exactly
     * five 1-bits indicating the five highest ranks in the index. A negative value always has
     * exactly one 0-bit indicating the high rank of a straight in the index. If the index only
     * represents ranks from a single suit, then a positive value indicates a flush and a negative
     * value indicates a straight flush.
     */
    private static final short[] values = values();

    /** Generate the lookup table. */
    private static short[] values() {
        // ranks
        int TWO = 1,
          THREE = 1 << 1,
           FOUR = 1 << 2,
           FIVE = 1 << 3,
            SIX = 1 << 4,
          SEVEN = 1 << 5,
          EIGHT = 1 << 6,
           NINE = 1 << 7,
            TEN = 1 << 8,
           JACK = 1 << 9,
          QUEEN = 1 << 10,
           KING = 1 << 11,
            ACE = 1 << 12;

        var length = ACE | KING | QUEEN | JACK | TEN | NINE | EIGHT + 1;
        var values = new short[length]; // 2 bytes * 0b1111111000001 = 16KB

        // map sets of five, six, or seven ranks to their five highest ranks
        for (var a = ACE; a >= SIX; a >>>= 1)
            for (var b = a >>> 1; b >= FIVE; b >>>= 1)
                for (var c = b >>> 1; c >= FOUR; c >>>= 1)
                    for (var d = c >>> 1; d >= THREE; d >>>= 1)
                        for (var e = d >>> 1; e >= TWO; e >>>= 1)
                            for (var f = e; f >= TWO; f >>>= 1)
                                for (var g = f; g >= TWO; g >>>= 1)
                                    values[a | b | c | d | e | f | g] = (short) (a | b | c | d | e);

        // remap straights to their high rank, inverted
        for (var high = ACE; high >= SIX; high >>>= 1) {
            var straight = -(high >>> 4) & (high << 1) - 1;

            // every way of choosing two ranks above the straight
            for (var a = ACE; a > high << 2; a >>>= 1)
                for (var b = a >>> 1; b > high << 1; b >>>= 1)
                    values[a | b | straight] = (short) ~high;

            // every way of choosing one rank above and zero or one below the straight
            for (var a = ACE; a > high << 1; a >>>= 1)
                for (var b = high >>> 4; b >= TWO; b >>>= 1)
                    values[a | straight | b] = (short) ~high;

            // every way of choosing zero, one, or two ranks below the straight
            for (var a = high >>> 4; a >= TWO; a >>>= 1)
                for (var b = a; b >= TWO; b >>>= 1)
                    values[straight | a | b] = (short) ~high;
        }

        var wheel = ACE | FIVE | FOUR | THREE | TWO; // ace plays low

        // every way of choosing zero, one, or two ranks above the wheel
        for (var a = ACE; a >= SEVEN; a >>>= 1)
            for (var b = a; b >= SEVEN; b >>>= 1)
                values[a | b | wheel] = (short) ~FIVE; // 5-high straight

        return values;
    }
}
