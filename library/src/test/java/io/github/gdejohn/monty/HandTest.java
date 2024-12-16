package io.github.gdejohn.monty;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.TreeMap;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Card.Rank.ACE;
import static io.github.gdejohn.monty.Card.Rank.EIGHT;
import static io.github.gdejohn.monty.Card.Rank.FIVE;
import static io.github.gdejohn.monty.Card.Rank.FOUR;
import static io.github.gdejohn.monty.Card.Rank.JACK;
import static io.github.gdejohn.monty.Card.Rank.KING;
import static io.github.gdejohn.monty.Card.Rank.NINE;
import static io.github.gdejohn.monty.Card.Rank.QUEEN;
import static io.github.gdejohn.monty.Card.Rank.SEVEN;
import static io.github.gdejohn.monty.Card.Rank.SIX;
import static io.github.gdejohn.monty.Card.Rank.TEN;
import static io.github.gdejohn.monty.Card.Rank.THREE;
import static io.github.gdejohn.monty.Card.Rank.TWO;
import static io.github.gdejohn.monty.Card.Suit.CLUBS;
import static io.github.gdejohn.monty.Card.Suit.DIAMONDS;
import static io.github.gdejohn.monty.Card.Suit.HEARTS;
import static io.github.gdejohn.monty.Card.Suit.SPADES;
import static io.github.gdejohn.monty.Category.FLUSH;
import static io.github.gdejohn.monty.Category.FOUR_OF_A_KIND;
import static io.github.gdejohn.monty.Category.FULL_HOUSE;
import static io.github.gdejohn.monty.Category.HIGH_CARD;
import static io.github.gdejohn.monty.Category.ONE_PAIR;
import static io.github.gdejohn.monty.Category.STRAIGHT;
import static io.github.gdejohn.monty.Category.STRAIGHT_FLUSH;
import static io.github.gdejohn.monty.Category.THREE_OF_A_KIND;
import static io.github.gdejohn.monty.Category.TWO_PAIR;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class HandTest {
    private static final Card[] deck = Card.all().toArray(Card[]::new);

    private static Stream<Hand> hands() {
        return range(0, 46).boxed().flatMap(
            a -> range(a + 1, 47).boxed().flatMap(
                b -> range(b + 1, 48).boxed().flatMap(
                    c -> range(c + 1, 49).boxed().flatMap(
                        d -> range(d + 1, 50).boxed().flatMap(
                            e -> range(e + 1, 51).boxed().flatMap(
                                f -> range(f + 1, 52).mapToObj(
                                    g -> Hand.of(
                                        deck[a],
                                        deck[b],
                                        deck[c],
                                        deck[d],
                                        deck[e],
                                        deck[f],
                                        deck[g]
                                    )
                                )
                            )
                        )
                    )
                )
            )
        );
    }

    @Test
    void equivalenceClasses() {
        var classes = hands().map(Hand::evaluate).collect(
            groupingBy(
                Category::of,
                () -> new EnumMap<>(Category.class),
                groupingBy(identity(), TreeMap::new, counting())
            )
        );
        assertThat(classes).containsKeys(Category.values());
        assertThat(classes).allSatisfy(
            (category, hands) -> {
                assertThat(hands).hasSize(category.classes);
                assertThat(hands.keySet()).allMatch(
                    value -> Integer.bitCount(value) == category.count
                );
                assertThat(
                    hands.values().stream().mapToInt(Math::toIntExact).sum()
                ).isEqualTo(category.hands);
            }
        );
        assertThat(classes.values().stream()).isSortedAccordingTo(
            (x, y) -> Integer.compare(x.lastKey(), y.firstKey())
        );
    }

    @Test
    void combinatorialHash() {
        record Stats(int count, int max) {}
        var hashes = hands().mapToInt(Hand::hashCode).sorted().boxed().reduce(
            new Stats(0, -1),
            (stats, hash) -> {
                assertThat(hash).isGreaterThan(stats.max);
                return new Stats(stats.count + 1, hash);
            },
            (_, _) -> fail("parallel reduction")
        );
        var count = 133_784_560; // 52 choose 7
        assertThat(hashes.count).isEqualTo(count);
        assertThat(hashes.max).isEqualTo(count - 1);
    }

    @Test
    void highCard() {
        var hand = Hand.of(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FIVE.of(CLUBS),
            SEVEN.of(DIAMONDS),
            EIGHT.of(SPADES),
            NINE.of(HEARTS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000001000000_0000000010000000_0000000000100000_0000000000001111L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000000_0000000000000000_0000000011101111L);
        assertThat(hand.evaluate()).isEqualTo(0b0000_0000000000000_0000011101100);
        assertThat(hand.category()).isEqualTo(HIGH_CARD);
        assertThat(Card.string(hand.sort())).isEqualTo("(9h,8s,7d,5c,4c)");
    }

    @Test
    void onePair() {
        var hand = Hand.of(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FIVE.of(SPADES),
            FIVE.of(HEARTS),
            SEVEN.of(DIAMONDS),
            EIGHT.of(CLUBS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000000001000_0000000000001000_0000000000100000_0000000001000111L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000000_0000000000001000_0000000001100111L);
        assertThat(hand.evaluate()).isEqualTo(0b0001_0000000001000_0000001100100);
        assertThat(hand.category()).isEqualTo(ONE_PAIR);
        assertThat(Card.string(hand.sort())).isEqualTo("(5s,5h,8c,7d,4c)");
    }

    @Test
    void twoPair() {
        var hand = Hand.of(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(HEARTS),
            FOUR.of(DIAMONDS),
            FIVE.of(CLUBS),
            FIVE.of(HEARTS),
            EIGHT.of(CLUBS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000000000000_0000000000001100_0000000000000100_0000000001001011L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000000_0000000000001100_0000000001000011L);
        assertThat(hand.evaluate()).isEqualTo(0b0010_0000000001100_0000001000000);
        assertThat(hand.category()).isEqualTo(TWO_PAIR);
        assertThat(Card.string(hand.sort())).isEqualTo("(5h,5c,4h,4d,8c)");
    }

    @Test
    void threePair() {
        var hand = Hand.of(
            THREE.of(SPADES),
            THREE.of(CLUBS),
            FOUR.of(HEARTS),
            FOUR.of(DIAMONDS),
            FIVE.of(CLUBS),
            FIVE.of(HEARTS),
            EIGHT.of(CLUBS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000000000010_0000000000001100_0000000000000100_0000000001001010L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000000_0000000000001110_0000000001000000L);
        assertThat(hand.evaluate()).isEqualTo(0b0010_0000000001100_0000001000000);
        assertThat(hand.category()).isEqualTo(TWO_PAIR);
        assertThat(Card.string(hand.sort())).isEqualTo("(5h,5c,4h,4d,8c)");
    }

    @Test
    void trips() {
        var hand = Hand.of(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FIVE.of(CLUBS),
            FIVE.of(HEARTS),
            FIVE.of(SPADES),
            KING.of(DIAMONDS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000000001000_0000000000001000_0000100000000000_0000000000001111L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000001000_0000000000000000_0000100000000111L);
        assertThat(hand.evaluate()).isEqualTo(0b0011_0000000001000_0100000000100);
        assertThat(hand.category()).isEqualTo(THREE_OF_A_KIND);
        assertThat(Card.string(hand.sort())).isEqualTo("(5s,5h,5c,Kd,4c)");
    }

    @Test
    void straight() {
        var hand = Hand.of(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(DIAMONDS),
            FIVE.of(SPADES),
            SIX.of(HEARTS),
            EIGHT.of(SPADES),
            NINE.of(HEARTS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000001001000_0000000010010000_0000000000000100_0000000000000011L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000000_0000000000000000_0000000011011111L);
        assertThat(hand.evaluate()).isEqualTo(0b0100_0000000000000_0000000010000);
        assertThat(hand.category()).isEqualTo(STRAIGHT);
        assertThat(Card.string(hand.sort())).isEqualTo("(6h,5s,4d,3c,2c)");
    }

    @Test
    void broadway() {
        var hand = Hand.of(
            TEN.of(CLUBS),
            JACK.of(CLUBS),
            QUEEN.of(DIAMONDS),
            KING.of(SPADES),
            ACE.of(CLUBS),
            ACE.of(SPADES),
            ACE.of(HEARTS)
        );
        assertThat(hand.mask()).isEqualTo(0b0001100000000000_0001000000000000_0000010000000000_0001001100000000L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0001000000000000_0000000000000000_0000111100000000L);
        assertThat(hand.evaluate()).isEqualTo(0b0100_0000000000000_1000000000000);
        assertThat(hand.category()).isEqualTo(STRAIGHT);
        assertThat(Card.string(hand.sort())).isEqualTo("(As,Ks,Qd,Jc,Tc)");
    }

    @Test
    void wheel() {
        var hand = Hand.of(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(DIAMONDS),
            FIVE.of(SPADES),
            EIGHT.of(SPADES),
            EIGHT.of(HEARTS),
            ACE.of(HEARTS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000001001000_0001000001000000_0000000000000100_0000000000000011L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000000_0000000001000000_0001000000001111L);
        assertThat(hand.evaluate()).isEqualTo(0b0100_0000000000000_0000000001000);
        assertThat(hand.category()).isEqualTo(STRAIGHT);
        assertThat(Card.string(hand.sort())).isEqualTo("(5s,4d,3c,2c,Ah)");
    }

    @Test
    void flush() {
        var hand = Hand.of(
            JACK.of(CLUBS),
            TEN.of(SPADES),
            TEN.of(HEARTS),
            NINE.of(HEARTS),
            EIGHT.of(HEARTS),
            SEVEN.of(HEARTS),
            FIVE.of(HEARTS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000100000000_0000000111101000_0000000000000000_0000001000000000L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000000_0000000100000000_0000001011101000L);
        assertThat(hand.evaluate()).isEqualTo(0b0101_0000000000000_0000111101000);
        assertThat(hand.category()).isEqualTo(FLUSH);
        assertThat(Card.string(hand.sort())).isEqualTo("(Th,9h,8h,7h,5h)");
    }

    @Test
    void fullHouse() {
        var hand = Hand.of(
            FIVE.of(CLUBS),
            FOUR.of(CLUBS),
            THREE.of(CLUBS),
            THREE.of(DIAMONDS),
            TWO.of(CLUBS),
            TWO.of(DIAMONDS),
            TWO.of(HEARTS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000000000000_0000000000000001_0000000000000011_0000000000001111L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000001_0000000000000010_0000000000001100L);
        assertThat(hand.evaluate()).isEqualTo(0b0110_0000000000001_0000000000010);
        assertThat(hand.category()).isEqualTo(FULL_HOUSE);
        assertThat(Card.string(hand.sort())).isEqualTo("(2h,2d,2c,3d,3c)");
    }

    @Test
    void tripsAndTwoPair() {
        var hand = Hand.of(
            FOUR.of(DIAMONDS),
            FOUR.of(CLUBS),
            THREE.of(CLUBS),
            THREE.of(DIAMONDS),
            TWO.of(CLUBS),
            TWO.of(DIAMONDS),
            TWO.of(HEARTS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000000000000_0000000000000001_0000000000000111_0000000000000111L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000001_0000000000000110_0000000000000000L);
        assertThat(hand.evaluate()).isEqualTo(0b0110_0000000000001_0000000000100);
        assertThat(hand.category()).isEqualTo(FULL_HOUSE);
        assertThat(Card.string(hand.sort())).isEqualTo("(2h,2d,2c,4d,4c)");
    }

    @Test
    void twoTrips() {
        var hand = Hand.of(
            FIVE.of(CLUBS),
            THREE.of(HEARTS),
            THREE.of(CLUBS),
            THREE.of(DIAMONDS),
            TWO.of(CLUBS),
            TWO.of(DIAMONDS),
            TWO.of(HEARTS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000000000000_0000000000000011_0000000000000011_0000000000001011L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000011_0000000000000000_0000000000001000L);
        assertThat(hand.evaluate()).isEqualTo(0b0110_0000000000010_0000000000001);
        assertThat(hand.category()).isEqualTo(FULL_HOUSE);
        assertThat(Card.string(hand.sort())).isEqualTo("(3h,3d,3c,2h,2d)");
    }

    @Test
    void quads() {
        var hand = Hand.of(
            SIX.of(HEARTS),
            SEVEN.of(CLUBS),
            NINE.of(CLUBS),
            NINE.of(HEARTS),
            NINE.of(DIAMONDS),
            NINE.of(SPADES),
            QUEEN.of(CLUBS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000010000000_0000000010010000_0000000010000000_0000010010100000L);
        assertThat(hand.ranks()).isEqualTo(0b0000000010000000_0000000000000000_0000000000000000_0000010000110000L);
        assertThat(hand.evaluate()).isEqualTo(0b0111_0000010000000_0010000000000);
        assertThat(hand.category()).isEqualTo(FOUR_OF_A_KIND);
        assertThat(Card.string(hand.sort())).isEqualTo("(9s,9h,9d,9c,Qc)");
    }

    @Test
    void quadsAndPair() {
        var hand = Hand.of(
            SIX.of(HEARTS),
            SIX.of(CLUBS),
            NINE.of(CLUBS),
            NINE.of(HEARTS),
            NINE.of(DIAMONDS),
            NINE.of(SPADES),
            QUEEN.of(SPADES)
        );
        assertThat(hand.mask()).isEqualTo(0b0000010010000000_0000000010010000_0000000010000000_0000000010010000L);
        assertThat(hand.ranks()).isEqualTo(0b0000000010000000_0000000000000000_0000000000010000_0000010000000000L);
        assertThat(hand.evaluate()).isEqualTo(0b0111_0000010000000_0010000000000);
        assertThat(hand.category()).isEqualTo(FOUR_OF_A_KIND);
        assertThat(Card.string(hand.sort())).isEqualTo("(9s,9h,9d,9c,Qs)");
    }

    @Test
    void quadsAndTrips() {
        var hand = Hand.of(
            SIX.of(HEARTS),
            SIX.of(CLUBS),
            SIX.of(SPADES),
            NINE.of(CLUBS),
            NINE.of(HEARTS),
            NINE.of(DIAMONDS),
            NINE.of(SPADES)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000010010000_0000000010010000_0000000010000000_0000000010010000L);
        assertThat(hand.ranks()).isEqualTo(0b0000000010000000_0000000000010000_0000000000000000_0000000000000000L);
        assertThat(hand.evaluate()).isEqualTo(0b0111_0000010000000_0000000010000);
        assertThat(hand.category()).isEqualTo(FOUR_OF_A_KIND);
        assertThat(Card.string(hand.sort())).isEqualTo("(9s,9h,9d,9c,6s)");
    }

    @Test
    void straightFlush() {
        var hand = Hand.of(
            SEVEN.of(HEARTS),
            EIGHT.of(HEARTS),
            NINE.of(HEARTS),
            TEN.of(HEARTS),
            JACK.of(HEARTS),
            QUEEN.of(SPADES),
            KING.of(HEARTS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000010000000000_0000101111100000_0000000000000000_0000000000000000L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000000_0000000000000000_0000111111100000L);
        assertThat(hand.evaluate()).isEqualTo(0b1000_0000000000000_0001000000000);
        assertThat(hand.category()).isEqualTo(STRAIGHT_FLUSH);
        assertThat(Card.string(hand.sort())).isEqualTo("(Jh,Th,9h,8h,7h)");
    }

    @Test
    void royalFlush() {
        var hand = Hand.of(
            TEN.of(CLUBS),
            JACK.of(CLUBS),
            QUEEN.of(CLUBS),
            KING.of(CLUBS),
            ACE.of(CLUBS),
            ACE.of(SPADES),
            ACE.of(HEARTS)
        );
        assertThat(hand.mask()).isEqualTo(0b0001000000000000_0001000000000000_0000000000000000_0001111100000000L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0001000000000000_0000000000000000_0000111100000000L);
        assertThat(hand.evaluate()).isEqualTo(0b1000_0000000000000_1000000000000);
        assertThat(hand.category()).isEqualTo(STRAIGHT_FLUSH);
        assertThat(Card.string(hand.sort())).isEqualTo("(Ac,Kc,Qc,Jc,Tc)");
    }

    @Test
    void steelWheel() {
        var hand = Hand.of(
            TWO.of(DIAMONDS),
            THREE.of(DIAMONDS),
            FOUR.of(DIAMONDS),
            FIVE.of(DIAMONDS),
            SIX.of(SPADES),
            SEVEN.of(HEARTS),
            ACE.of(DIAMONDS)
        );
        assertThat(hand.mask()).isEqualTo(0b0000000000010000_0000000000100000_0001000000001111_0000000000000000L);
        assertThat(hand.ranks()).isEqualTo(0b0000000000000000_0000000000000000_0000000000000000_0001000000111111L);
        assertThat(hand.evaluate()).isEqualTo(0b1000_0000000000000_0000000001000);
        assertThat(hand.category()).isEqualTo(STRAIGHT_FLUSH);
        assertThat(Card.string(hand.sort())).isEqualTo("(5d,4d,3d,2d,Ad)");
    }
}
