package io.github.gdejohn.monty;

import static io.github.gdejohn.monty.Category.FLUSH;
import static io.github.gdejohn.monty.Category.FOUR_OF_A_KIND;
import static io.github.gdejohn.monty.Category.FULL_HOUSE;
import static io.github.gdejohn.monty.Category.HIGH_CARD;
import static io.github.gdejohn.monty.Category.ONE_PAIR;
import static io.github.gdejohn.monty.Category.STRAIGHT;
import static io.github.gdejohn.monty.Category.STRAIGHT_FLUSH;
import static io.github.gdejohn.monty.Category.THREE_OF_A_KIND;
import static io.github.gdejohn.monty.Category.TWO_PAIR;

import java.util.stream.Stream;

import io.github.gdejohn.monty.Card.Cards;

public final class Hand implements Comparable<Hand> {
    private static final Hand EMPTY = new Hand(0L, 0L, 0, 0, 0);

    /**
     * A bit vector of the cards in this hand.
     *
     * <p>The bit vector is organized into four blocks of thirteen bits each,
     * one block for each suit.
     */
    final long cards;

    /**
     * A bit vector multiset of the rank frequencies in this hand.
     *
     * <p>The bit vector is organized into four blocks of thirteen bits each.
     * The blocks represent the frequency of a rank, increasing from least to
     * most significant bits. The positions of set bits within a block indicate
     * which ranks occur with the frequency represented by that block.
     */
    final long ranks;
    
    /**
     * A bit vector multiset of the suit frequencies in this hand.
     *
     * <p>The bit vector is organized into seven blocks of four bits each. The
     * blocks represent the frequency of a suit from one through seven,
     * increasing from least to most significant bits. The positions of set
     * bits within a block indicate which suits occur with the frequency
     * represented by that block.
     */
    final int suits;

    /**
     * A bit vector of the ranks of the cards in this hand.
     */
    final short kickers;

    /**
     * The number of distinct ranks of the cards in this hand.
     */
    final short count;

    private Hand(long cards, long ranks, int suits, int kickers, int count) {
        this.cards = cards;
        this.ranks = ranks;
        this.suits = suits;
        this.kickers = (short) kickers;
        this.count = (short) count;
    }

    public static Hand empty() {
        return EMPTY;
    }

    public Hand deal(Card card) {
        var rank = card.rank();
        return new Hand(
            cards | card.pack(),
            rank.increment(ranks),
            card.suit().increment(suits),
            kickers | rank.pack(),
            count + (((kickers >>> rank.ordinal()) & 1) ^ 1)
        );
    }

    public int evaluate() {
        if (count > 4) {
            if (suits > 1 << 16) {
                var count = (suits < (1 << 20)) ? 5 : ((suits < (1 << 24)) ? 6 : 7);
                var suit = suits >>> ((count - 1) << 2);
                var flush = (cards >>> (((suit >>> 1) - (suit >>> 3)) * 13)) & (-1 >>> -13);
                var straightFlush = straight((int) flush, count);
                if (straightFlush != 0) {
                    return STRAIGHT_FLUSH.pack(straightFlush);
                } else {
                    return FLUSH.pack(select(flush, count - 5));
                }
            } else {
                var straight = straight(kickers, count);
                if (straight != 0) {
                    return STRAIGHT.pack(straight);
                } else if (count > 5) {
                    if (count == 6) { // 2-1-1-1-1-1
                        var pair = ranks >>> 13;
                        return ONE_PAIR.pack((pair << 13) | select(kickers ^ pair, 2));
                    } else { // 1-1-1-1-1-1-1
                        return HIGH_CARD.pack(select(kickers, 2));
                    }
                } else if (ranks < 1L << 26) { // 2-2-1-1-1
                    var pairs = ranks >> 13;
                    return TWO_PAIR.pack((pairs << 13) | select(kickers ^ pairs, 2));
                } else { // 3-1-1-1-1
                    var trips = ranks >>> 26;
                    return THREE_OF_A_KIND.pack((trips << 13) | select(kickers ^ trips, 2));
                }
            }
        } else if (ranks < 1L << 26) { // 2-2-2-1
            var pairs = select(ranks >>> 13, 1);
            return TWO_PAIR.pack((pairs << 13) | select(kickers ^ pairs, 1));
        } else if (ranks < 1L << 39) { // 3-3-1, 3-2-2, 3-2-1-1
            var fullHouse = select(ranks, count - 2) >>> 13;
            var pair = fullHouse & -fullHouse;
            return FULL_HOUSE.pack((fullHouse ^ pair) | (pair >>> 13) | (pair & (-1 >>> -13)));
        } else { // 4-3, 4-2-1, 4-1-1-1
            var quads = ranks >>> 39;
            return FOUR_OF_A_KIND.pack((quads << 13) | select(kickers ^ quads, count - 2));
        }
    }

    /**
     * Check for a straight.
     *
     * @param ranks the ranks to check
     * @param count the number of distinct ranks
     *
     * @return the high rank of the straight, or {@code 0} if there is no straight
     */
    static int straight(int ranks, int count) {
        var wheel = ~((ranks << 1) | (ranks >>> 12));
        wheel = ((wheel & -wheel) >>> 5) << 3;
        if (wheel != 0) {
            return wheel;
        } else while (true) {
            var high = 31 - Integer.numberOfLeadingZeros(ranks);
            var straight = ~(ranks >>> (high - 4));
            straight = ((straight & -straight) >>> 5) << high;
            if (straight != 0  || --count < 5) {
                return straight;
            } else {
                ranks ^= 1 << high; // clear highest rank
            }
        }
    }

    /**
     * Select all bits except for the {@code n} least significant bits.
     *
     * @param bits the bits to select from
     * @param n the number of low bits to clear
     * 
     * @return the selected bits
     */
    static long select(long bits, int n) {
        while (n-- > 0) {
            bits &= bits - 1;
        }
        return bits;
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
        return Cards.toString(cards());
    }

    @Override
    public int compareTo(Hand hand) {
        return Integer.compare(this.evaluate(), hand.evaluate());
    }

    public Category category() {
        return Category.unpack(this.evaluate());
    }

    public Stream<Card> cards() {
        return category().cards(this);
    }
}
