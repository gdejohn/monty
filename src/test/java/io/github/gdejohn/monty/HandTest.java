package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Hand.Category;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

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
import static io.github.gdejohn.monty.Hand.Category.FLUSH;
import static io.github.gdejohn.monty.Hand.Category.HIGH_CARD;
import static io.github.gdejohn.monty.Hand.Category.STRAIGHT;
import static io.github.gdejohn.monty.Hand.Category.STRAIGHT_FLUSH;
import static org.assertj.core.api.Assertions.assertThat;

class HandTest {
    private static final float LOAD_FACTOR = 0.75f;

    @Test
    void equivalenceClasses() {
        var classes = new EnumMap<Category, Map<Integer, Integer>>(Category.class);
        Card.combinations().mapToInt(Hand::evaluate).forEach(
            value -> {
                var category = Category.unpack(value);
                assertThat(Integer.bitCount(value)).isEqualTo(category.bitCount());
                classes.computeIfAbsent(
                    category,
                    key -> new HashMap<>(
                        (int) Math.ceil(category.classes() / LOAD_FACTOR),
                        LOAD_FACTOR
                    )
                ).merge(value, 1, Integer::sum);
            }
        );
        var categories = EnumSet.allOf(Category.class);
        assertThat(classes.size()).isEqualTo(categories.size());
        categories.forEach(
            category -> {
                var hands = classes.get(category);
                assertThat(hands.size()).isEqualTo(category.classes());
                assertThat(hands.values().stream().mapToInt(Integer::intValue).sum()).isEqualTo(category.hands());
            }
        );
    }

    @Disabled
    @Test
    void ranks() {
        var values = Card.combinations().mapToInt(Hand::evaluate).distinct().sorted().toArray();

        var ranks = new short[52][][][][][][];

        for (var a = 0; a < 52; a++)
            ranks[a] = new short[52][][][][][];

        for (var a = 0; a < 51; a++)
            for (var b = a + 1; b < 52; b++)
                ranks[a][b] =
                ranks[b][a] = new short[52][][][][];

        for (var a = 0; a < 50; a++)
            for (var b = a + 1; b < 51; b++)
                for (var c = b + 1; c < 52; c++)
                    ranks[a][b][c] =
                    ranks[a][c][b] =
                    ranks[b][c][a] = new short[52][][][];

        for (var a = 0; a < 49; a++)
            for (var b = a + 1; b < 50; b++)
                for (var c = b + 1; c < 51; c++)
                    for (var d = c + 1; d < 52; d++)
                        ranks[a][b][c][d] =
                        ranks[a][b][d][c] =
                        ranks[a][c][d][b] =
                        ranks[b][c][d][a] = new short[52][][];

        for (var a = 0; a < 48; a++)
            for (var b = a + 1; b < 49; b++)
                for (var c = b + 1; c < 50; c++)
                    for (var d = c + 1; d < 51; d++)
                        for (var e = d + 1; e < 52; e++)
                            ranks[a][b][c][d][e] =
                            ranks[a][b][c][e][d] =
                            ranks[a][b][d][e][c] =
                            ranks[a][c][d][e][b] =
                            ranks[b][c][d][e][a] = new short[52][];

        for (var a = 0; a < 47; a++)
            for (var b = a + 1; b < 48; b++)
                for (var c = b + 1; c < 49; c++)
                    for (var d = c + 1; d < 50; d++)
                        for (var e = d + 1; e < 51; e++)
                            for (var f = e + 1; f < 52; f++)
                                ranks[a][b][c][d][e][f] =
                                ranks[a][b][c][d][f][e] =
                                ranks[a][b][c][e][f][d] =
                                ranks[a][b][d][e][f][c] =
                                ranks[a][c][d][e][f][b] =
                                ranks[b][c][d][e][f][a] = new short[52];

        for (var a = 0; a < 46; a++)
            for (var b = a + 1; b < 47; b++)
                for (var c = b + 1; c < 48; c++)
                    for (var d = c + 1; d < 49; d++)
                        for (var e = d + 1; e < 50; e++)
                            for (var f = e + 1; f < 51; f++)
                                for (var g = f + 1; g < 52; g++)
                                    ranks[a][b][c][d][e][f][g] =
                                    ranks[a][b][c][d][e][g][f] =
                                    ranks[a][b][c][d][f][g][e] =
                                    ranks[a][b][c][e][f][g][d] =
                                    ranks[a][b][d][e][f][g][c] =
                                    ranks[a][c][d][e][f][g][b] =
                                    ranks[b][c][d][e][f][g][a] = rank(values, a, b, c, d, e, f, g);

        assertThat(ranks[51][50][49][48][47][46][45]).isEqualTo((short) 4_823);
    }

    private static short rank(int[] values, int a, int b, int c, int d, int e, int f, int g) {
        var value = Hand.evaluate(1L << a | 1L << b | 1L << c | 1L << d | 1L << e | 1L << f | 1L << g);
        return (short) Arrays.binarySearch(values, value);
    }

    @Test
    void highCard() {
        var hand = Hand.evaluate(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FIVE.of(CLUBS),
            SEVEN.of(DIAMONDS),
            EIGHT.of(SPADES),
            NINE.of(HEARTS)
        );
        assertThat(hand.category()).isEqualTo(HIGH_CARD);
        assertThat(hand.toString()).isEqualTo("(9♥,8♠,7♦,5♣,4♣)");
    }

    @Test
    void straight() {
        var hand = Hand.evaluate(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(DIAMONDS),
            FIVE.of(SPADES),
            SIX.of(HEARTS),
            EIGHT.of(SPADES),
            EIGHT.of(HEARTS)
        );
        assertThat(hand.category()).isEqualTo(STRAIGHT);
        assertThat(hand.toString()).isEqualTo("(6♥,5♠,4♦,3♣,2♣)");
    }

    @Test
    void broadway() {
        var hand = Hand.evaluate(
            TEN.of(CLUBS),
            JACK.of(CLUBS),
            QUEEN.of(DIAMONDS),
            KING.of(SPADES),
            ACE.of(CLUBS),
            ACE.of(SPADES),
            ACE.of(HEARTS)
        );
        assertThat(hand.category()).isEqualTo(STRAIGHT);
        assertThat(hand.toString()).isEqualTo("(A♠,K♠,Q♦,J♣,T♣)");
    }

    @Test
    void wheel() {
        var hand = Hand.evaluate(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(DIAMONDS),
            FIVE.of(SPADES),
            EIGHT.of(SPADES),
            EIGHT.of(HEARTS),
            ACE.of(HEARTS)
        );
        assertThat(hand.category()).isEqualTo(STRAIGHT);
        assertThat(hand.toString()).isEqualTo("(5♠,4♦,3♣,2♣,A♥)");
    }

    @Test
    void flush() {
        var hand = Hand.evaluate(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FOUR.of(DIAMONDS),
            FIVE.of(CLUBS),
            FIVE.of(HEARTS),
            EIGHT.of(CLUBS)
        );
        assertThat(hand.category()).isEqualTo(FLUSH);
        assertThat(hand.toString()).isEqualTo("(8♣,5♣,4♣,3♣,2♣)");
    }

    @Test
    void straightFlush() {
        var hand = Hand.evaluate(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FIVE.of(CLUBS),
            SIX.of(CLUBS),
            EIGHT.of(SPADES),
            EIGHT.of(HEARTS)
        );
        assertThat(hand.category()).isEqualTo(STRAIGHT_FLUSH);
        assertThat(hand.toString()).isEqualTo("(6♣,5♣,4♣,3♣,2♣)");
    }

    @Test
    void royalFlush() {
        var hand = Hand.evaluate(
            TEN.of(CLUBS),
            JACK.of(CLUBS),
            QUEEN.of(CLUBS),
            KING.of(CLUBS),
            ACE.of(CLUBS),
            ACE.of(SPADES),
            ACE.of(HEARTS)
        );
        assertThat(hand.category()).isEqualTo(STRAIGHT_FLUSH);
        assertThat(hand.toString()).isEqualTo("(A♣,K♣,Q♣,J♣,T♣)");
    }

    @Test
    void steelWheel() {
        var hand = Hand.evaluate(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FIVE.of(CLUBS),
            EIGHT.of(SPADES),
            EIGHT.of(HEARTS),
            ACE.of(CLUBS)
        );
        assertThat(hand.category()).isEqualTo(STRAIGHT_FLUSH);
        assertThat(hand.toString()).isEqualTo("(5♣,4♣,3♣,2♣,A♣)");
    }
}
