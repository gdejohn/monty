package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Rank;
import io.github.gdejohn.monty.Card.Suit;

import java.util.Iterator;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.offset;

/// A seven-card poker hand evaluator for Texas hold 'em.
///
/// Mostly bitwise logical operators and shifts, some integer arithmetic (no multiplication, no
/// division, no modulus), five reads from a 16KB lookup table (comfortably fits in L1 cache),
/// nearly branchless (one small jump table, no loops, no conditional statements), garbage free,
/// no standard library, everything final or effectively final.
public final class Hand implements Iterable<Card> {
    /// A bit vector representing ranks grouped by suit.
    ///
    /// There are four 16-bit blocks, one for each suit. The positions of 1-bits in the 13
    /// low-order bits of a block indicate which ranks occur with the suit represented by that
    /// block. The three high-order bits in each block are unused.
    ///
    /// The cards `[As,Ah,Ad,Jd,Jc,4c,2c]` are represented like this:
    ///
    /// ```text
    ///       ┌──spades──────┐┌──hearts──────┐┌──diamonds────┐┌──clubs───────┐
    ///     0b***1000000000000***1000000000000***1001000000000***0001000000101
    ///          A               A               A  J               J      4 2
    /// ```
    ///
    /// @see Card#offset(int)
    /// @see #slice(int, long)
    private final long cards;

    /// A bit vector representing ranks grouped by frequency.
    ///
    /// There are four 16-bit blocks. The blocks represent the frequency of a rank, increasing
    /// from least to most significant bits. The positions of 1-bits in the 13 low-order bits of a
    /// block indicate which ranks occur with the frequency represented by that block. The three
    /// high-order bits in each block are unused.
    ///
    /// The rank frequencies for the cards `[As,Ah,Ad,Jd,Jc,4c,2c]` are represented like this:
    ///
    /// ```text
    ///       ┌──quads───────┐┌──trips───────┐┌──pairs───────┐┌──kickers─────┐
    ///     0b***0000000000000***1000000000000***0001000000000***0000000000101
    ///                          A                  J                      4 2
    /// ```
    ///
    /// @see Card#offset(int)
    /// @see #slice(int, long)
    /// @see #COUNT
    private final long ranks;

    /// Make a new hand with the given [cards][#cards] and [ranks][#ranks].
    private Hand(long cards, long ranks) {
        this.cards = cards;
        this.ranks = ranks;
    }

    private static final Hand EMPTY = new Hand(0L, 0L);

    /// A hand containing no cards.
    public static Hand empty() {
        return EMPTY;
    }

    public static Hand of(Card... cards) {
        var hand = Hand.empty();
        for (var card : cards) {
            hand = hand.add(card);
        }
        if (hand.size() != cards.length) {
            throw new IllegalArgumentException("duplicate cards");
        }
        return hand;
    }

    /// A sliding mask that extracts frequencies from [ranks][#ranks].
    private static final long COUNT = 1L << offset(2)  // trips
                                    | 1L << offset(1)  // pairs
                                    | 1L << offset(0); // kickers

    /// Make a new hand containing the given card and this hand's cards.
    ///
    /// Hands that contain exactly seven distinct cards can be [evaluated][#evaluate()].
    /// Intermediate hands represent partial evaluations that can be reused for the community
    /// cards shared by every player.
    public Hand add(Card card) {
        int ordinal = Rank.ordinal(card.offset());
        long rank = ranks & COUNT << ordinal;
        return new Hand(
            cards | card.mask(),
            ranks ^ rank | (rank << offset(1)) | ((rank - 1 >>> -1) << ordinal)
        );
    }

    /// Determine the value of this hand.
    ///
    /// The value is a positive 30-bit integer representing the equivalence class of the best
    /// five-card hand that can be made from the seven [cards][#add(Card)] in this hand, computed
    /// directly without checking each of the 21 combinations (7 choose 5). Hands are ordered by
    /// their values: a hand with a greater value beats a hand with a lesser value, two hands with
    /// equal values tie each other.
    ///
    /// Hand values represent equivalence classes using the first 13 low-order bits for less
    /// significant ranks, the next 13 bits for more significant ranks (possibly empty), and the
    /// last 4 bits for the category of the hand. There are 7,462 equivalence classes for
    /// five-card hands, but only 4,824 equivalence classes are possible when making the best
    /// five-card hand from seven cards. For example, given the five cards `[5s,5h,4h,3h,2h]`,
    /// there is no way of choosing two other cards such that neither is included in the best
    /// five-card hand that can be made from those seven cards.
    ///
    /// Hands are evaluated by first hashing them to a 5-bit integer ranging from -2 to 15,
    /// inclusive. The hash function encodes rank frequencies and the presence of straights and
    /// flushes, determining the category of the hand and the significance of each distinct rank.
    /// This partitions all 133,784,560 seven-card hands (52 choose 7) into 14 buckets such that
    /// every hand in each bucket can be evaluated with the same expression, which is selected by
    /// a tableswitch on the hash code.
    public int evaluate() {
        int kickers = slice(0, ranks),
              pairs = slice(1, ranks),
              trips = slice(2, ranks),
              quads = slice(3, ranks),
              value = values[kickers | pairs | trips | quads],
              flush = values[slice(0, cards)]  // clubs
                    | values[slice(1, cards)]  // diamonds
                    | values[slice(2, cards)]  // hearts
                    | values[slice(3, cards)], // spades
           category = ((-pairs ^ -drop(drop(pairs))) >>> -1)   << 3
                    | (-(drop(pairs) | drop(trips)) >>> -1)    << 2
                    | (-trips >>> -1 | (flush | -flush) >> -1) << 1
                    | (-quads >>> -1 | (value & (flush - 1)) >>> -4);
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
            default -> -1; // invalid hand
        };
    }

    /// Pack the category and ranks together.
    private static int evaluate(int category, int ranks) {
        return category << Category.OFFSET | ranks;
    }

    /// Pack the category, high-order ranks, and low-order ranks together.
    private static int evaluate(int category, int high, int low) {
        return evaluate(category, high << 13 | low);
    }

    /// Flip the least significant 1-bit.
    private static int drop(int ranks) {
        return ranks & ranks - 1;
    }

    /// Flip every 1-bit except for the least significant one.
    private static int last(int ranks) {
        return ranks & -ranks;
    }

    /// Extract the nth 16-bit subword from a bit vector.
    private static short slice(int n, long vector) {
        return (short) (vector >>> offset(n));
    }

    /// A lookup table that selects the relevant ranks of straights and flushes.
    ///
    /// Nonzero values are only associated with sets of five, six, or seven ranks represented by
    /// 13-bit indices with 1-bits at the corresponding positions. A positive value always has
    /// exactly five 1-bits indicating the five highest ranks in the index. A negative value
    /// always has exactly one 0-bit indicating the high rank of a straight in the index. If the
    /// index only represents ranks from a single suit, then a positive value indicates a flush
    /// and a negative value indicates a straight flush.
    ///
    /// @see #values()
    private static final short[] values = values();

    /// Generate the lookup table.
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

        int max = ACE | KING | QUEEN | JACK | TEN | NINE | EIGHT; // max key
        var values = new short[max + 1]; // 2 bytes * 0b1111111000001 = 16KB

        // map sets of five, six, or seven ranks to their five highest ranks
        for (int a = ACE; a >= SIX; a >>>= 1)
            for (int b = a >>> 1; b >= FIVE; b >>>= 1)
                for (int c = b >>> 1; c >= FOUR; c >>>= 1)
                    for (int d = c >>> 1; d >= THREE; d >>>= 1)
                        for (int e = d >>> 1; e >= TWO; e >>>= 1)
                            for (int f = e; f >= TWO; f >>>= 1)
                                for (int g = f; g >= TWO; g >>>= 1)
                                    values[a | b | c | d | e | f | g] = (short) (a | b | c | d | e);

        // remap straights to their high rank, inverted
        for (int high = ACE; high >= SIX; high >>>= 1) {
            int straight = -(high >>> 4) & ((high << 1) - 1);

            // every way of choosing two ranks above the straight
            for (int a = ACE; a > high << 2; a >>>= 1)
                for (int b = a >>> 1; b > high << 1; b >>>= 1)
                    values[a | b | straight] = (short) ~high;

            // every way of choosing one rank above the straight and zero or one below
            for (int a = ACE; a > high << 1; a >>>= 1)
                for (int b = high >>> 4; b >= TWO; b >>>= 1)
                    values[a | straight | b] = (short) ~high;

            // every way of choosing zero, one, or two ranks below the straight
            for (int a = high >>> 4; a >= TWO; a >>>= 1)
                for (int b = a; b >= TWO; b >>>= 1)
                    values[straight | a | b] = (short) ~high;
        }

        int wheel = ACE | FIVE | FOUR | THREE | TWO; // ace plays low

        // every way of choosing zero, one, or two ranks above the wheel
        for (int a = ACE; a >= SEVEN; a >>>= 1)
            for (int b = a; b >= SEVEN; b >>>= 1)
                values[a | b | wheel] = (short) ~FIVE; // 5-high straight

        return values;
    }

    public Category category() {
        return Category.of(this.evaluate());
    }

    long mask() {
        return cards;
    }

    long ranks() {
        return ranks;
    }

    int count(Suit suit) {
        return Integer.bitCount(slice(suit.ordinal(), cards));
    }

    public int size() {
        return Long.bitCount(cards);
    }

    @Override
    public Iterator<Card> iterator() {
        return stream().iterator();
    }

    public Stream<Card> stream() {
        return LongStream.iterate(
            cards,
            cards -> cards != 0L,
            cards -> cards & cards - 1L
        ).mapToObj(
            cards -> Card.of(Long.numberOfTrailingZeros(cards))
        );
    }

    public Stream<Card> sort() {
        if (Long.bitCount(cards) != 7) {
            throw new IllegalArgumentException("partial hand");
        }
        int value = evaluate();
        var category = Category.of(value);
        return stream().sorted(category.order(this, value)).limit(5);
    }

    @Override
    public String toString() {
        return Card.string(this.stream());
    }

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

    private static final int[][] choose = choose();

    @Override
    public int hashCode() {
        int hash = 0, k = 0;
        for (long cards = this.cards; cards != 0L; cards &= cards - 1) {
            int n = Card.ordinal(Long.numberOfTrailingZeros(cards));
            hash += choose[k++][n];
        }
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Hand hand && hand.cards == cards;
    }

    public boolean contains(Card card) {
        return card.in(this.mask());
    }
}
