package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Rank;
import io.github.gdejohn.monty.Card.Suit;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.Rank.FIVE;
import static io.github.gdejohn.monty.Card.distinct;
import static io.github.gdejohn.monty.Card.reverseLowball;
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
import static java.util.Comparator.comparingLong;
import static java.util.Comparator.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.maxBy;
import static java.util.stream.Collectors.toCollection;

public final class Hand implements Comparable<Hand> {
    public enum Category {
        HIGH_CARD(5, 407, 23_294_460),

        ONE_PAIR(5, 1_470, 58_627_800),

        TWO_PAIR(4, 763, 31_433_400),

        THREE_OF_A_KIND(5, 575, 6_461_620),

        STRAIGHT(2, 10, 6_180_020) {
            @Override
            Stream<Card> unpack(Hand hand) {
                var high = Rank.unpack(hand.value);
                var cards = hand.unpack().collect(
                    groupingBy(Card::rank, () -> new EnumMap<>(Rank.class), maxBy(comparing(Card::suit)))
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
            Stream<Card> unpack(Hand hand) {
                return hand.flush().sorted(reverseOrder()).limit(5);
            }
        },

        FULL_HOUSE(4, 156, 3_473_184),

        FOUR_OF_A_KIND(5, 156, 224_848),

        STRAIGHT_FLUSH(2, 10, 41_584) {
            @Override
            Stream<Card> unpack(Hand hand) {
                var high = Rank.unpack(hand.value);
                if (high == FIVE) { // steel wheel
                    return hand.flush().sorted(reverseLowball()).dropWhile(card -> card.rank() != high);
                } else {
                    return hand.flush().sorted(reverseOrder()).dropWhile(card -> card.rank() != high).limit(5);
                }
            }
        };

        private static Category[] values = Category.values();

        private final int bitCount;

        private final int classes;

        private final int hands;

        Category(int bitCount, int classes, int hands) {
            this.bitCount = bitCount;
            this.classes = classes;
            this.hands = hands;
        }

        static Category unpack(int rank) {
            return Category.values[rank >> 26];
        }

        Stream<Card> unpack(Hand hand) {
            var counts = hand.unpack().collect(
                groupingBy(Card::rank, () -> new EnumMap<>(Rank.class), counting())
            );
            return hand.unpack().sorted(
                comparing(
                    Card::rank,
                    comparingLong(counts::get)
                ).thenComparing(Card::rank).reversed()
            ).limit(5);
        }

        final int pack(int ranks) {
            return (this.ordinal() << 26) | ranks;
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

    private final long cards;

    private final int value;

    private Hand(long cards) {
        this.cards = distinct(7, cards);
        this.value = evaluate(cards);
    }

    public static Hand evaluate(Card first, Card second, Card third, Card fourth, Card fifth, Card sixth, Card seventh) {
        return new Hand(first.pack() | second.pack() | third.pack() | fourth.pack() | fifth.pack() | sixth.pack() | seventh.pack());
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object instanceof Hand) {
            return value == ((Hand) object).value;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return this.cards().map(Card::toString).collect(joining(",", "(", ")"));
    }

    @Override
    public int compareTo(Hand hand) {
        return Integer.compare(value, hand.value);
    }

    public Category category() {
        return Category.unpack(value);
    }

    public Stream<Card> cards() {
        return this.category().unpack(this);
    }

    private Stream<Card> unpack() {
        return LongStream.iterate(
            this.cards,
            cards -> cards != 0,
            cards -> cards & (cards - 1)
        ).mapToObj(Card::unpack);
    }

    private Stream<Card> flush() {
        return this.unpack().collect(
            groupingBy(
                Card::suit,
                () -> new EnumMap<>(Suit.class),
                mapping(Card::rank, toCollection(() -> EnumSet.noneOf(Rank.class))))
        ).entrySet().stream().max(comparingByValue(comparingInt(Set::size))).stream().flatMap(
            entry -> entry.getValue().stream().map(rank -> rank.of(entry.getKey()))
        );
    }

    private static int wheel(int ranks) {
        var wheel = ~((ranks << 1) | (ranks >>> 12));
        return ((wheel & -wheel) >>> 5) << 3;
    }

    private static int straight(int ranks, int high) {
        var straight = ~(ranks >>> (high - 4));
        return ((straight & -straight) >>> 5) << high;
    }

    static int evaluate(long hand) {
        var kickers = 0;
        var counts = 0L; // bit vector multiset
        for (var i = 0; i < 4; i++) { // for each suit
            var ranks = (int) (hand >>> (i * 13)) & (-1 >>> -13); // extract ranks for current suit
            var flush = ranks;
            var bitCount = Integer.bitCount(ranks);
            if (bitCount >= 5) {
                var wheel = wheel(ranks);
                if (wheel != 0) {
                    return STRAIGHT_FLUSH.pack(wheel);
                } else while (true) {
                    var high = 31 - Integer.numberOfLeadingZeros(ranks);
                    var straight = straight(ranks, high);
                    if (straight != 0) {
                        return STRAIGHT_FLUSH.pack(straight);
                    } else if (bitCount == 5) {
                        return FLUSH.pack(flush);
                    } else {
                        ranks ^= 1 << high; // clear the most significant bit
                        flush &= flush - 1; // clear the least significant bit
                        bitCount--;
                    }
                }
            }
            kickers |= ranks;
            while (bitCount > 0) {
                var rank = Integer.numberOfTrailingZeros(ranks);
                var count = (((1L << 26) | (1L << 13) | 1L) << rank) & counts; // extract multiplicity
                counts ^= Long.max(1L << rank, count | (count << 13)); // increment multiplicity
                ranks &= ranks - 1; // clear the least significant bit
                bitCount--;
            }
        }
        var ranks = kickers;
        var bitCount = Integer.bitCount(ranks);
        if (bitCount >= 5) {
            var wheel = wheel(ranks);
            if (wheel != 0) {
                return STRAIGHT.pack(wheel);
            } else while (true) {
                var high = 31 - Integer.numberOfLeadingZeros(ranks);
                var straight = straight(ranks, high);
                if (straight != 0) {
                    return STRAIGHT.pack(straight);
                } else if (bitCount == 5) {
                    break;
                } else {
                    ranks ^= 1 << high; // clear the most significant bit
                    bitCount--;
                }
            }
        }
        var count = 63 - Long.numberOfLeadingZeros(counts);
        var firstCount = count / 13;
        var first = count % 13;
        count = 63 - Long.numberOfLeadingZeros(counts ^ (1L << count));
        var secondCount = count / 13;
        var second = count % 13;
        if (firstCount == 3) {
            return FOUR_OF_A_KIND.pack((1 << (first + 13)) | (1 << second));
        } else if (secondCount == 0) {
            if (firstCount == 0) {
                return HIGH_CARD.pack((kickers &= kickers - 1) & (kickers - 1));
            } else {
                kickers ^= ((1 << 13) + 1) << first;
                return (firstCount == 1 ? ONE_PAIR : THREE_OF_A_KIND).pack((kickers &= kickers - 1) & (kickers - 1));
            }
        } else if (firstCount == 1) {
            var third = 31 - Integer.numberOfLeadingZeros(kickers ^ (1 << first) ^ (1 << second));
            return TWO_PAIR.pack((((1 << first) | (1 << second)) << 13) | (1 << third));
        } else {
            return FULL_HOUSE.pack((1 << (first + 13)) | (1 << second));
        }
    }
}
