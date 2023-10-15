package io.github.gdejohn.monty;

import static io.github.gdejohn.monty.Card.reverseLowball;
import static io.github.gdejohn.monty.Card.Rank.FIVE;
import static io.github.gdejohn.monty.Hand.Category.FLUSH;
import static io.github.gdejohn.monty.Hand.Category.FOUR_OF_A_KIND;
import static io.github.gdejohn.monty.Hand.Category.FULL_HOUSE;
import static io.github.gdejohn.monty.Hand.Category.HIGH_CARD;
import static io.github.gdejohn.monty.Hand.Category.ONE_PAIR;
import static io.github.gdejohn.monty.Hand.Category.STRAIGHT;
import static io.github.gdejohn.monty.Hand.Category.STRAIGHT_FLUSH;
import static io.github.gdejohn.monty.Hand.Category.THREE_OF_A_KIND;
import static io.github.gdejohn.monty.Hand.Category.TWO_PAIR;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.toCollection;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import io.github.gdejohn.monty.Card.Cards;
import io.github.gdejohn.monty.Card.Rank;
import io.github.gdejohn.monty.Card.Suit;

public final class Hand implements Comparable<Hand> {
    public enum Category {
        HIGH_CARD(5, 407, 23_294_460),

        ONE_PAIR(5, 1_470, 58_627_800),

        TWO_PAIR(4, 763, 31_433_400) {
            @Override
            Stream<Card> cards(Hand hand) {
                var pairs = Category.unpack(hand).limit(4);
                var kicker = Category.unpack(hand).skip(4).max(comparing(Card::rank)).stream();
                return Stream.concat(pairs, kicker);
            }
        },

        THREE_OF_A_KIND(5, 575, 6_461_620),

        STRAIGHT(2, 10, 6_180_020) {
            @Override
            Stream<Card> cards(Hand hand) {
                var high = Rank.unpack(hand.evaluate());
                var cards = Cards.unpack(hand.cards).collect(
                    groupingBy(
                        Card::rank,
                        () -> new EnumMap<>(Rank.class),
                        maxBy(comparing(Card::suit))
                    )
                ).values().stream().flatMap(Optional::stream);
                if (high == FIVE) { // wheel
                    return cards.sorted(reverseLowball()).dropWhile(card -> card.rank() != high);
                } else {
                    return cards.sorted(reverseOrder()).dropWhile(card -> card.rank() != high).limit(5);
                }
            }
        },

        FLUSH(7, 1_277, 4_047_644) {
            @Override
            Stream<Card> cards(Hand hand) {
                return flush(hand, reverseOrder()).limit(5);
            }
        },

        FULL_HOUSE(4, 156, 3_473_184),

        FOUR_OF_A_KIND(5, 156, 224_848) {
            @Override
            Stream<Card> cards(Hand hand) {
                var quads = Category.unpack(hand).limit(4);
                var kicker = Category.unpack(hand).skip(4).max(comparing(Card::rank)).stream();
                return Stream.concat(quads, kicker);
            }
        },

        STRAIGHT_FLUSH(2, 10, 41_584) {
            @Override
            Stream<Card> cards(Hand hand) {
                var high = Rank.unpack(hand.evaluate());
                if (high == FIVE) { // steel wheel
                    return flush(hand, reverseLowball()).dropWhile(card -> card.rank() != high);
                } else {
                    return flush(hand, reverseOrder()).dropWhile(card -> card.rank() != high).limit(5);
                }
            }
        };

        private static final int SHIFT = 26;

        private static final Category[] values = Category.values();

        private final int bitCount;

        private final int classes;

        private final int hands;

        Category(int bitCount, int classes, int hands) {
            this.bitCount = bitCount;
            this.classes = classes;
            this.hands = hands;
        }

        private static Stream<Card> flush(Hand hand, Comparator<Card> order) {
            return Cards.unpack(hand.cards).collect(
                groupingBy(
                    Card::suit,
                    () -> new EnumMap<>(Suit.class),
                    toCollection(() -> new TreeSet<>(order))
                )
            ).entrySet().stream().max(
                comparingByValue(comparingInt(Set::size))
            ).stream().map(Entry::getValue).flatMap(Set::stream);
        }

        static Category unpack(int value) {
            return Category.values[value >>> Category.SHIFT];
        }

        Stream<Card> cards(Hand hand) {
            return Category.unpack(hand).limit(5);
        }

        static Stream<Card> unpack(Hand hand) {
            return Cards.unpack(hand.cards).collect(
                groupingBy(
                    Card::rank,
                    () -> new EnumMap<>(Rank.class),
                    toCollection(() -> new TreeSet<>(comparing(Card::suit).reversed()))
                )
            ).entrySet().stream().sorted(
                comparingInt(
                    (Entry<Rank, TreeSet<Card>> entry) -> entry.getValue().size()
                ).thenComparing(Entry::getKey).reversed()
            ).map(Entry::getValue).flatMap(Set::stream);
        }

        final int pack(long ranks) {
            return (this.ordinal() << 26) | (int) ranks;
        }

        final int bitCount() {
            return bitCount;
        }

        final int classes() {
            return classes;
        }

        final int hands() {
            return hands;
        }
    }

    final long cards;

    final int ranks;

    final long rankCounts;
    
    final int suitCounts;

    Hand(long cards, int ranks, long rankCounts, int suitCounts) {
        this.cards = cards;
        this.ranks = ranks;
        this.rankCounts = rankCounts;
        this.suitCounts = suitCounts;
    }

    Hand() {
        this(0L, 0, 0L, 0);
    }

    static Hand of(Card... cards) {
        var hand = new Hand();
        for (var card : cards) {
            hand = hand.deal(card);
        }
        return hand;
    }

    Hand deal(Card card) {
        var rank = card.rank().ordinal();
        var suit = card.suit().ordinal();
        var rankCount = rankCounts & (Rank.COUNTS << rank);
        var suitCount = suitCounts & (Suit.COUNTS << suit);
        return new Hand(
            cards | card.pack(),
            ranks | (1 << rank),
            rankCounts ^ Long.max(1L << rank, rankCount | (rankCount << 13)),
            suitCounts ^ Integer.max(1 << suit, suitCount | (suitCount << 4))
        );
    }

    public int evaluate() {
        var count = Integer.bitCount(ranks);
        if (count > 4) {
            if (suitCounts > 1 << 16) { // flush
                var suit = 31 - Integer.numberOfLeadingZeros(suitCounts);
                var flush = (int) (cards >>> ((suit & 3) * 13)) & (-1 >>> -13); // suit & 3 == suit % 4
                count = (suit >>> 2) + 1; // suit >>> 2 == suit / 4
                var straightFlush = straight(flush, count);
                if (straightFlush != 0) {
                    return STRAIGHT_FLUSH.pack(straightFlush);
                } else {
                    return FLUSH.pack(select(flush, count - 5));
                }
            } else {
                var straight = straight(ranks, count);
                if (straight != 0) {
                    return STRAIGHT.pack(straight);
                } else if (count > 5) {
                    if (count == 6) { // 2-1-1-1-1-1
                        var pair = rankCounts >>> 13;
                        return ONE_PAIR.pack((pair << 13) | select(ranks ^ pair, 2));
                    } else { // 1-1-1-1-1-1-1
                        return HIGH_CARD.pack(select(ranks, 2));
                    }
                } else if (rankCounts < 1L << 26) { // 2-2-1-1-1
                    var pairs = rankCounts >> 13;
                    return TWO_PAIR.pack((pairs << 13) | select(ranks ^ pairs, 2));
                } else { // 3-1-1-1-1
                    var trips = rankCounts >>> 26;
                    return THREE_OF_A_KIND.pack((trips << 13) | select(ranks ^ trips, 2));
                }
            }
        } else if (rankCounts < 1L << 26) { // 2-2-2-1
            var pairs = select(rankCounts >>> 13, 1);
            return TWO_PAIR.pack((pairs << 13) | select(ranks ^ pairs, 1));
        } else if (rankCounts < 1L << 39) { // 3-3-1, 3-2-2, 3-2-1-1
            var fullHouse = select(rankCounts, count - 2) >>> 13;
            var pair = fullHouse & -fullHouse;
            return FULL_HOUSE.pack((fullHouse ^ pair) | (pair >>> 13) | (pair & (-1 >>> -13)));
        } else { // 4-3, 4-2-1, 4-1-1-1
            var quads = rankCounts >>> 39;
            return FOUR_OF_A_KIND.pack((quads << 13) | select(ranks ^ quads, count - 2));
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
