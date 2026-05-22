package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Rank;
import io.github.gdejohn.monty.Card.Suit;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.offset;
import static java.util.stream.IntStream.range;

/// Seven-card poker hand evaluation for Texas hold 'em.
///
/// Mostly bitwise logical operators and shifts, some integer arithmetic (no multiplication, no
/// division, no modulo), five reads from a 16KB lookup table (comfortably fits in L1 cache),
/// nearly branchless (one small jump table, no loops, no conditional statements, no ternary
/// operator), garbage free, no standard library, everything final or effectively final.
public final class Hand implements Iterable<Card> {
    /// A bit vector representing ranks grouped by suit.
    ///
    /// There are four 16-bit blocks, one for each suit. This documentation orders suits ascending
    /// alphabetically from least significant bits to most significant bits, but any order works
    /// as long as it's used consistently. The positions of 1-bits in the 13 low-order bits of a
    /// block indicate which ranks occur in this hand with the suit represented by that block. The
    /// three high-order bits in each block `***` are unused.
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
    final long cards;

    /// A bit vector representing ranks grouped by frequency.
    ///
    /// There are four 16-bit blocks. The blocks represent the frequency of a rank, increasing
    /// from least to most significant bits. The positions of 1-bits in the 13 low-order bits of a
    /// block indicate which ranks occur with the frequency represented by that block. The three
    /// high-order bits in each block `***` are unused.
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
    final long ranks;

    /// Make a hand with the given [cards][#cards] and [ranks][#ranks].
    private Hand(long cards, long ranks) {
        this.cards = cards;
        this.ranks = ranks;
    }

    private static final Hand EMPTY = new Hand(0L, 0L);

    /// A hand containing no cards.
    public static Hand empty() {
        return EMPTY;
    }

    /// Every seven-card hand.
    public static Stream<Hand> all() {
        return range(0, 46).boxed().flatMap(
            a -> range(a + 1, 47).boxed().flatMap(
                b -> range(b + 1, 48).boxed().flatMap(
                    c -> range(c + 1, 49).boxed().flatMap(
                        d -> range(d + 1, 50).boxed().flatMap(
                            e -> range(e + 1, 51).boxed().flatMap(
                                f -> range(f + 1, 52).mapToObj(
                                    g -> Hand.of(
                                        Card.CARDS[a],
                                        Card.CARDS[b],
                                        Card.CARDS[c],
                                        Card.CARDS[d],
                                        Card.CARDS[e],
                                        Card.CARDS[f],
                                        Card.CARDS[g]
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    /// Make a hand containing the given `cards`.
    ///
    /// @throws IllegalArgumentException if there are more than seven `cards`
    /// @throws IllegalArgumentException if there are duplicate `cards`
    public static Hand of(Card... cards) {
        if (cards.length > 7) {
            throw new IllegalArgumentException(
                "too many cards %s".formatted(Arrays.toString(cards))
            );
        }
        var hand = Hand.empty();
        for (var card : cards) {
            hand = hand.add(card);
        }
        if (hand.size() != cards.length) {
            throw new IllegalArgumentException(
                "duplicate cards %s".formatted(Arrays.toString(cards))
            );
        }
        return hand;
    }

    /// Make a hand containing the given card and this hand's cards.
    ///
    /// Hands that contain exactly seven distinct cards can be [evaluated][#evaluate()].
    /// Intermediate hands represent partial evaluations that can be reused for the community cards
    /// shared by every player.
    public Hand add(Card card) {
        int rank = Rank.ordinal(card.offset);
        long mask = ranks & (COUNT << rank);
        return new Hand(
            cards | card.mask(),
            ranks ^ mask | (mask << offset(1)) | ((mask - 1 >>> -1) << rank)
        );
    }

    /// A sliding mask used to extract frequencies from [#ranks].
    private static final long COUNT = 1L << offset(2) | 1L << offset(1) | 1L;

    /// Determine the value of this hand, assuming it contains exactly seven distinct cards.
    ///
    /// The value is a positive 30-bit integer representing the equivalence class of the best
    /// five-card hand that can be made from the seven cards in this hand, computed directly
    /// without checking each of the 21 combinations (7 choose 5). Hands are ordered by their
    /// values: a hand with a greater value beats a hand with a lesser value, two hands with equal
    /// values tie each other.
    ///
    /// Hand values represent equivalence classes using the first 13 low-order bits for less
    /// significant ranks, the next 13 bits for more significant ranks (possibly empty), and the
    /// last 4 bits for the category of the hand. There are 7,462 equivalence classes for five-card
    /// hands, but only 4,824 equivalence classes are possible when making the best five-card hand
    /// from seven cards. For example, given the five cards `[5s,5h,4h,3h,2h]`, there is no way of
    /// choosing two other cards such that neither is included in the best five-card hand that can
    /// be made from those seven cards.
    ///
    /// Hands are evaluated by first hashing them to a 5-bit integer in the range `[-2..15]`
    /// (-2 to 15, inclusive). The hash function encodes the frequencies of pairs `[0..3]`,
    /// three-of-a-kinds `[0..2]`, four-of-a-kinds `[0..1]`, straights `[0..1]`, flushes `[0..1]`,
    /// and straight flushes `[0..1]`. This partitions all 133,784,560 seven-card hands (52 choose
    /// 7\) into 14 buckets such that every hand in each bucket can be evaluated with the same
    /// expression, which is selected by a tableswitch on the hash code.
    public int evaluate() {
        int kickers = slice(0, ranks),
              pairs = slice(1, ranks),
              trips = slice(2, ranks),
              quads = slice(3, ranks),
           straight = values[kickers | pairs | trips | quads],
              flush = values[slice(0, cards)]  // clubs
                    | values[slice(1, cards)]  // diamonds
                    | values[slice(2, cards)]  // hearts
                    | values[slice(3, cards)], // spades
               hash = (( (-blsr(blsr(pairs))              ^     -pairs ) >>> -4) &  0b1000)
                    | ((-(      blsr(trips)               | blsr(pairs)) >>> -3) &  0b0100)
                    | ((  (   flush | -flush     ) >>  -2 |     -trips   >>> -2) & -0b0010)
                    | ((  (straight & (flush - 1)) >>> -4 |     -quads   >>> -1) &  0b1111);
        return switch (hash) {
            case  0b1000 -> onePair(pairs, blsr(blsr(kickers)));
            case  0b1100 -> twoPair(pairs, blsr(blsr(kickers)));
            case  0b0100 -> twoPair(blsr(pairs), blsr(blsi(pairs) | kickers));
            case  0b0010 -> threeOfAKind(trips, blsr(blsr(kickers)));
            case  0b1010 -> fullHouse(trips, pairs);
            case  0b1110 -> fullHouse(trips, blsr(pairs));
            case  0b0110 -> fullHouse(blsr(trips), blsi(trips));
            case  0b0001 -> fourOfAKind(quads, blsr(blsr(kickers)));
            case  0b1001 -> fourOfAKind(quads, blsr(pairs | kickers));
            case  0b0011 -> fourOfAKind(quads, trips);
            case -0b0010 -> flush(flush);
            case  0b1111 -> straight(~straight);
            case -0b0001 -> straightFlush(~flush);
            case  0b0000 -> blsr(blsr(kickers)); // high card
            default -> throw new AssertionError("invalid hand");
        };
    }

    /// Extract the nth 16-bit subword from a bit vector.
    ///
    /// ```java
    /// assert slice(1, 0b0000001111000000_1110000000000111L)
    ///              == 0b0000001111000000;
    /// ```
    private static short slice(int n, long vector) {
        return (short) (vector >>> offset(n));
    }

    /// Clear the least significant 1-bit.
    ///
    /// ```java
    /// assert blsr(0b101100)
    ///          == 0b101000;
    /// ```
    ///
    /// @see <a href="https://www.felixcloutier.com/x86/blsr">blsr</a>
    private static int blsr(int ranks) {
        return ranks & (ranks - 1);
    }

    /// Extract the least significant 1-bit.
    ///
    /// ```java
    /// assert blsr(0b101100)
    ///          == 0b000100;
    /// ```
    ///
    /// @see <a href="https://www.felixcloutier.com/x86/blsi">blsi</a>
    private static int blsi(int ranks) {
        return ranks & -ranks;
    }

    /// `[Js,Jh,Ad,6c,5c] -> 0b0001_0001000000000_1000000011000`
    private static int onePair(int pairs, int kickers) {
        return (1 << 26) | (pairs << 13) | kickers;
    }

    /// `[Qs,Qh,8d,8c,5c] -> 0b0010_0010001000000_0000000001000`
    private static int twoPair(int pairs, int kickers) {
        return (2 << 26) | (pairs << 13) | kickers;
    }

    /// `[Ts,Th,Td,7c,4c] -> 0b0011_0000100000000_0000000100100`
    private static int threeOfAKind(int trips, int kickers) {
        return (3 << 26) | (trips << 13) | kickers;
    }

    /// `[Js,Th,9d,8c,7c] -> 0b0100_0000000000000_0001000000000`
    private static int straight(int straight) {
        return (4 << 26) | straight;
    }

    /// `[Kd,Td,7d,5d,2d] -> 0b0101_0000000000000_0100100101001`
    private static int flush(int flush) {
        return (5 << 26) | flush;
    }

    /// `[5s,5h,5d,3s,3c] -> 0b0110_0000000001000_0000000000010`
    private static int fullHouse(int trips, int pair) {
        return (6 << 26) | (trips << 13) | pair;
    }

    /// `[Ks,Kh,Kd,Kc,8c] -> 0b0111_0100000000000_0000001000000`
    private static int fourOfAKind(int quads, int kickers) {
        return (7 << 26) | (quads << 13) | kickers;
    }

    /// `[Js,Ts,9s,8s,7s] -> 0b1000_0000000000000_0001000000000`
    private static int straightFlush(int flush) {
        return (8 << 26) | flush;
    }

    /// A lookup table for the values of straights and flushes.
    ///
    /// Nonzero values are only associated with sets of five, six, or seven ranks represented by
    /// 13-bit indices with 1-bits at the corresponding positions. A positive value always has
    /// exactly five 1-bits indicating the five highest ranks in the index. A negative value
    /// always has exactly one 0-bit indicating the high rank of a straight in the index. If the
    /// index only represents ranks from a single suit, then a positive value indicates a flush
    /// and a negative value indicates a straight flush.
    ///
    /// The index for the cards `[As,Ah,Ad,Jd,Jc,4c,2c]` looks like this:
    ///
    /// ```text
    ///       ┌──ranks────┐
    ///     0b1001000000101
    ///       A  J      4 2
    /// ```
    ///
    /// The corresponding value for that index is 0 because straights and flushes are impossible
    /// with only four distinct ranks.
    ///
    /// @see #values()
    private static final short[] values = values();

    /// Generate the lookup table.
    private static short[] values() {
        // rank masks
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

        int length = (ACE | KING | QUEEN | JACK | TEN | NINE | EIGHT) + 1;
        var values = new short[length]; // 2 bytes * 0b1111111000001 = 16KB

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

    long mask() {
        return cards;
    }

    int count(Suit suit) {
        return Integer.bitCount(slice(suit.ordinal(), cards));
    }

    /// The category of this hand.
    public Category category() {
        return Category.of(this.evaluate());
    }

    /// The number of cards in this hand.
    public int size() {
        return Long.bitCount(cards);
    }

    /// True if and only if this hand contains the given `card`.
    public boolean contains(Card card) {
        return (cards & card.mask()) != 0;
    }

    /// A stream of the cards in this hand.
    public Stream<Card> stream() {
        return LongStream.iterate(
            cards,
            cards -> cards != 0L, // not empty
            cards -> cards & (cards - 1L) // remove lowest
        ).mapToObj(Card::lowest);
    }

    /// The cards that make the best five-card hand, in descending order of importance.
    Stream<Card> sort() {
        if (this.size() != 7) {
            throw new IllegalStateException("partial hand");
        }
        int value = this.evaluate();
        var comparator = Category.of(value).comparator(this, value);
        return this.stream().sorted(comparator).limit(5);
    }

    @Override
    public Iterator<Card> iterator() {
        return stream().iterator();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Hand hand && hand.cards == cards;
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
            hash += choose[k++][n]; // combinatorial number system
        }
        return hash;
    }

    @Override
    public String toString() {
        return Card.string(this.stream());
    }
}
