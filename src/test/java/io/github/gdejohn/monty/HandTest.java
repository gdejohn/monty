package io.github.gdejohn.monty;

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
import static io.github.gdejohn.monty.Hand.Category.FOUR_OF_A_KIND;
import static io.github.gdejohn.monty.Hand.Category.FULL_HOUSE;
import static io.github.gdejohn.monty.Hand.Category.HIGH_CARD;
import static io.github.gdejohn.monty.Hand.Category.ONE_PAIR;
import static io.github.gdejohn.monty.Hand.Category.STRAIGHT;
import static io.github.gdejohn.monty.Hand.Category.STRAIGHT_FLUSH;
import static io.github.gdejohn.monty.Hand.Category.THREE_OF_A_KIND;
import static io.github.gdejohn.monty.Hand.Category.TWO_PAIR;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.github.gdejohn.monty.Hand.Category;

class HandTest {
    private static final Card[] deck = Card.deck().toArray(Card[]::new);

    private static Hand hand(Card first, Card second, Card third, Card fourth, Card fifth, Card sixth, Card seventh) {
        var hand = Hand.of(first, second, third, fourth, fifth, sixth, seventh);
        assertThat(Long.bitCount(hand.cards)).isEqualTo(7);
        return hand;
    }

    private static Stream<Hand> hands() {
        return range(0, 46).boxed().flatMap(
            a -> range(a + 1, 47).boxed().flatMap(
                b -> range(b + 1, 48).boxed().flatMap(
                    c -> range(c + 1, 49).boxed().flatMap(
                        d -> range(d + 1, 50).boxed().flatMap(
                            e -> range(e + 1, 51).boxed().flatMap(
                                f -> range(f + 1, 52).mapToObj(
                                    g -> hand(
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
        var classes = hands().collect(
            groupingBy(
                Hand::category,
                groupingBy(
                    Hand::evaluate,
                    counting()
                )
            )
        );
        var categories = EnumSet.allOf(Category.class);
        assertThat(classes.keySet().size()).isEqualTo(categories.size());
        for (var category : categories) {
            var hands = classes.get(category);
            for (var hand : hands.keySet()) {
                assertThat(Integer.bitCount(hand)).isEqualTo(category.bitCount());
            }
            assertThat(hands.size()).isEqualTo(category.classes());
            assertThat(
                hands.values().stream().mapToInt(Math::toIntExact).sum()
            ).isEqualTo(category.hands());
        }
    }

    @Test
    void benchmark() {
        var blackhole = 0;
        var partial = new Hand[6];
        for (var a = 0; a < 46; a++) {
            partial[0] = new Hand().deal(deck[a]);
            for (var b = a + 1; b < 47; b++) {
                partial[1] = partial[0].deal(deck[b]);
                for (var c = b + 1; c < 48; c++) {
                    partial[2] = partial[1].deal(deck[c]);
                    for (var d = c + 1; d < 49; d++) {
                        partial[3] = partial[2].deal(deck[d]);
                        for (var e = d + 1; e < 50; e++) {
                            partial[4] = partial[3].deal(deck[e]);
                            for (var f = e + 1; f < 51; f++) {
                                partial[5] = partial[4].deal(deck[f]);
                                for (var g = f + 1; g < 52; g++) {
                                    blackhole |=  partial[5].deal(deck[g]).evaluate();
                                }
                            }
                        }
                    }
                }
            }
        }
        assertThat(blackhole != 0);
    }

    @Test
    void unsharedBenchmark() {
        var blackhole = 0;
        for (var a = 0; a < 46; a++) {
            for (var b = a + 1; b < 47; b++) {
                for (var c = b + 1; c < 48; c++) {
                    for (var d = c + 1; d < 49; d++) {
                        for (var e = d + 1; e < 50; e++) {
                            for (var f = e + 1; f < 51; f++) {
                                for (var g = f + 1; g < 52; g++) {
                                    blackhole |= hand(
                                        deck[a],
                                        deck[b],
                                        deck[c],
                                        deck[d],
                                        deck[e],
                                        deck[f],
                                        deck[g]
                                    ).evaluate();
                                }
                            }
                        }
                    }
                }
            }
        }
        assertThat(blackhole != 0);
    }

    @Test
    void streamBenchmark() {
        assertThat(hands().mapToInt(Hand::evaluate).reduce(0, (n, m) -> n | m)).isNotEqualTo(0);
    }

    @Test
    void highCard() {
        var hand = hand(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FIVE.of(CLUBS),
            SEVEN.of(DIAMONDS),
            EIGHT.of(SPADES),
            NINE.of(HEARTS)
        );
        assertThat(hand.cards).isEqualTo(0b0000001000000_0000010000000_0000000100000_0000000001111L);
        assertThat(hand.ranks).isEqualTo((short) 0b0000011101111);
        assertThat(hand.count).isEqualTo((short) 7);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000000_0000000000000_0000011101111L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0001_0000_0000_1110);
        assertThat(hand.evaluate()).isEqualTo(0b0000_0000000000000_0000011101100);
        assertThat(hand.category()).isEqualTo(HIGH_CARD);
        assertThat(hand.toString()).isEqualTo("(9♥,8♠,7♦,5♣,4♣)");
    }

    @Test
    void onePair() {
        var hand = hand(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FIVE.of(SPADES),
            FIVE.of(HEARTS),
            SEVEN.of(DIAMONDS),
            EIGHT.of(CLUBS)
        );
        assertThat(hand.cards).isEqualTo(0b0000000001000_0000000001000_0000000100000_0000001000111L);
        assertThat(hand.ranks).isEqualTo((short) 0b0000001101111);
        assertThat(hand.count).isEqualTo((short) 6);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000000_0000000001000_0000001100111L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0001_0000_0000_1110);
        assertThat(hand.evaluate()).isEqualTo(0b0001_0000000001000_0000001100100);
        assertThat(hand.category()).isEqualTo(ONE_PAIR);
        assertThat(hand.toString()).isEqualTo("(5♠,5♥,8♣,7♦,4♣)");
    }

    @Test
    void twoPair() {
        var hand = hand(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(HEARTS),
            FOUR.of(DIAMONDS),
            FIVE.of(CLUBS),
            FIVE.of(HEARTS),
            EIGHT.of(CLUBS)
        );
        assertThat(hand.cards).isEqualTo(0b0000000000000_0000000001100_0000000000100_0000001001011L);
        assertThat(hand.ranks).isEqualTo((short) 0b0000001001111);
        assertThat(hand.count).isEqualTo((short) 5);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000000_0000000001100_0000001000011L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0001_0000_0100_0010);
        assertThat(hand.evaluate()).isEqualTo(0b0010_0000000001100_0000001000000);
        assertThat(hand.category()).isEqualTo(TWO_PAIR);
        assertThat(hand.toString()).isEqualTo("(5♥,5♣,4♥,4♦,8♣)");
    }

    @Test
    void threePair() {
        var hand = hand(
            THREE.of(SPADES),
            THREE.of(CLUBS),
            FOUR.of(HEARTS),
            FOUR.of(DIAMONDS),
            FIVE.of(CLUBS),
            FIVE.of(HEARTS),
            EIGHT.of(CLUBS)
        );
        assertThat(hand.cards).isEqualTo(0b0000000000010_0000000001100_0000000000100_0000001001010L);
        assertThat(hand.ranks).isEqualTo((short) 0b0000001001110);
        assertThat(hand.count).isEqualTo((short) 4);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000000_0000000001110_0000001000000L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0000_0001_0100_1010);
        assertThat(hand.evaluate()).isEqualTo(0b0010_0000000001100_0000001000000);
        assertThat(hand.category()).isEqualTo(TWO_PAIR);
        assertThat(hand.toString()).isEqualTo("(5♥,5♣,4♥,4♦,8♣)");
    }

    @Test
    void trips() {
        var hand = hand(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FIVE.of(CLUBS),
            FIVE.of(HEARTS),
            FIVE.of(SPADES),
            KING.of(DIAMONDS)
        );
        assertThat(hand.cards).isEqualTo(0b0000000001000_0000000001000_0100000000000_0000000001111L);
        assertThat(hand.ranks).isEqualTo((short) 0b0100000001111);
        assertThat(hand.count).isEqualTo((short) 5);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000001000_0000000000000_0100000000111L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0001_0000_0000_1110);
        assertThat(hand.evaluate()).isEqualTo(0b0011_0000000001000_0100000000100);
        assertThat(hand.category()).isEqualTo(THREE_OF_A_KIND);
        assertThat(hand.toString()).isEqualTo("(5♠,5♥,5♣,K♦,4♣)");
    }

    @Test
    void straight() {
        var hand = hand(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(DIAMONDS),
            FIVE.of(SPADES),
            SIX.of(HEARTS),
            EIGHT.of(SPADES),
            NINE.of(HEARTS)
        );
        assertThat(hand.cards).isEqualTo(0b0000001001000_0000010010000_0000000000100_0000000000011L);
        assertThat(hand.ranks).isEqualTo((short) 0b0000011011111);
        assertThat(hand.count).isEqualTo((short) 7);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000000_0000000000000_0000011011111L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0000_0000_1101_0010);
        assertThat(hand.evaluate()).isEqualTo(0b0100_0000000000000_0000000010000);
        assertThat(hand.category()).isEqualTo(STRAIGHT);
        assertThat(hand.toString()).isEqualTo("(6♥,5♠,4♦,3♣,2♣)");
    }

    @Test
    void broadway() {
        var hand = hand(
            TEN.of(CLUBS),
            JACK.of(CLUBS),
            QUEEN.of(DIAMONDS),
            KING.of(SPADES),
            ACE.of(CLUBS),
            ACE.of(SPADES),
            ACE.of(HEARTS)
        );
        assertThat(hand.cards).isEqualTo(0b1100000000000_1000000000000_0010000000000_1001100000000L);
        assertThat(hand.ranks).isEqualTo((short) 0b1111100000000);
        assertThat(hand.count).isEqualTo((short) 5);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_1000000000000_0000000000000_0111100000000L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0000_0001_1000_0110);
        assertThat(hand.evaluate()).isEqualTo(0b0100_0000000000000_1000000000000);
        assertThat(hand.category()).isEqualTo(STRAIGHT);
        assertThat(hand.toString()).isEqualTo("(A♠,K♠,Q♦,J♣,T♣)");
    }

    @Test
    void wheel() {
        var hand = hand(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(DIAMONDS),
            FIVE.of(SPADES),
            EIGHT.of(SPADES),
            EIGHT.of(HEARTS),
            ACE.of(HEARTS)
        );
        assertThat(hand.cards).isEqualTo(0b0000001001000_1000001000000_0000000000100_0000000000011L);
        assertThat(hand.ranks).isEqualTo((short) 0b1000001001111);
        assertThat(hand.count).isEqualTo((short) 6);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000000_0000001000000_1000000001111L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0000_0000_1101_0010);
        assertThat(hand.evaluate()).isEqualTo(0b0100_0000000000000_0000000001000);
        assertThat(hand.category()).isEqualTo(STRAIGHT);
        assertThat(hand.toString()).isEqualTo("(5♠,4♦,3♣,2♣,A♥)");
    }

    @Test
    void flush() {
        var hand = hand(
            TWO.of(CLUBS),
            THREE.of(CLUBS),
            FOUR.of(CLUBS),
            FOUR.of(DIAMONDS),
            FIVE.of(CLUBS),
            FIVE.of(HEARTS),
            EIGHT.of(CLUBS)
        );
        assertThat(hand.cards).isEqualTo(0b0000000000000_0000000001000_0000000000100_0000001001111L);
        assertThat(hand.ranks).isEqualTo((short) 0b0000001001111);
        assertThat(hand.count).isEqualTo((short) 5);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000000_0000000001100_0000001000011L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0001_0000_0000_0000_0110);
        assertThat(hand.evaluate()).isEqualTo(0b0101_0000000000000_0000001001111);
        assertThat(hand.category()).isEqualTo(FLUSH);
        assertThat(hand.toString()).isEqualTo("(8♣,5♣,4♣,3♣,2♣)");
    }

    @Test
    void fullHouseA() {
        var hand = hand(
            FIVE.of(CLUBS),
            FOUR.of(CLUBS),
            THREE.of(CLUBS),
            THREE.of(DIAMONDS),
            TWO.of(CLUBS),
            TWO.of(DIAMONDS),
            TWO.of(HEARTS)
        );
        assertThat(hand.cards).isEqualTo(0b0000000000000_0000000000001_0000000000011_0000000001111L);
        assertThat(hand.ranks).isEqualTo((short) 0b0000000001111);
        assertThat(hand.count).isEqualTo((short) 4);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000001_0000000000010_0000000001100L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0001_0000_0010_0100);
        assertThat(hand.evaluate()).isEqualTo(0b0110_0000000000001_0000000000010);
        assertThat(hand.category()).isEqualTo(FULL_HOUSE);
        assertThat(hand.toString()).isEqualTo("(2♥,2♦,2♣,3♦,3♣)");
    }

    @Test
    void fullHouseB() {
        var hand = hand(
            FIVE.of(CLUBS),
            THREE.of(HEARTS),
            THREE.of(CLUBS),
            THREE.of(DIAMONDS),
            TWO.of(CLUBS),
            TWO.of(DIAMONDS),
            TWO.of(HEARTS)
        );
        assertThat(hand.cards).isEqualTo(0b0000000000000_0000000000011_0000000000011_0000000001011L);
        assertThat(hand.ranks).isEqualTo((short) 0b0000000001011);
        assertThat(hand.count).isEqualTo((short) 3);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000011_0000000000000_0000000001000L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0000_0001_0110_0000);
        assertThat(hand.evaluate()).isEqualTo(0b0110_0000000000010_0000000000001);
        assertThat(hand.category()).isEqualTo(FULL_HOUSE);
        assertThat(hand.toString()).isEqualTo("(3♥,3♦,3♣,2♥,2♦)");
    }

    @Test
    void fullHouseC() {
        var hand = hand(
            FOUR.of(DIAMONDS),
            FOUR.of(CLUBS),
            THREE.of(CLUBS),
            THREE.of(DIAMONDS),
            TWO.of(CLUBS),
            TWO.of(DIAMONDS),
            TWO.of(HEARTS)
        );
        assertThat(hand.cards).isEqualTo(0b0000000000000_0000000000001_0000000000111_0000000000111L);
        assertThat(hand.ranks).isEqualTo((short) 0b0000000000111);
        assertThat(hand.count).isEqualTo((short) 3);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000001_0000000000110_0000000000000L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0000_0011_0000_0100);
        assertThat(hand.evaluate()).isEqualTo(0b0110_0000000000001_0000000000100);
        assertThat(hand.category()).isEqualTo(FULL_HOUSE);
        assertThat(hand.toString()).isEqualTo("(2♥,2♦,2♣,4♦,4♣)");
    }

    @Test
    void quadsA() {
        var hand = hand(
            SIX.of(HEARTS),
            SEVEN.of(CLUBS),
            NINE.of(CLUBS),
            NINE.of(HEARTS),
            NINE.of(DIAMONDS),
            NINE.of(SPADES),
            QUEEN.of(CLUBS)
        );
        assertThat(hand.cards).isEqualTo(0b0000010000000_0000010010000_0000010000000_0010010100000L);
        assertThat(hand.ranks).isEqualTo((short) 0b0010010110000);
        assertThat(hand.count).isEqualTo((short) 4);
        assertThat(hand.rankCounts).isEqualTo(0b0000010000000_0000000000000_0000000000000_0010000110000L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0000_0001_0100_1010);
        assertThat(hand.evaluate()).isEqualTo(0b0111_0000010000000_0010000000000);
        assertThat(hand.category()).isEqualTo(FOUR_OF_A_KIND);
        assertThat(hand.toString()).isEqualTo("(9♠,9♥,9♦,9♣,Q♣)");
    }

    @Test
    void quadsB() {
        var hand = hand(
            SIX.of(HEARTS),
            SIX.of(CLUBS),
            NINE.of(CLUBS),
            NINE.of(HEARTS),
            NINE.of(DIAMONDS),
            NINE.of(SPADES),
            QUEEN.of(SPADES)
        );
        assertThat(hand.cards).isEqualTo(0b0010010000000_0000010010000_0000010000000_0000010010000L);
        assertThat(hand.ranks).isEqualTo((short) 0b0010010010000);
        assertThat(hand.count).isEqualTo((short) 3);
        assertThat(hand.rankCounts).isEqualTo(0b0000010000000_0000000000000_0000000010000_0010000000000L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0000_0000_1101_0010);
        assertThat(hand.evaluate()).isEqualTo(0b0111_0000010000000_0010000000000);
        assertThat(hand.category()).isEqualTo(FOUR_OF_A_KIND);
        assertThat(hand.toString()).isEqualTo("(9♠,9♥,9♦,9♣,Q♠)");
    }

    @Test
    void quadsC() {
        var hand = hand(
            SIX.of(HEARTS),
            SIX.of(CLUBS),
            SIX.of(SPADES),
            NINE.of(CLUBS),
            NINE.of(HEARTS),
            NINE.of(DIAMONDS),
            NINE.of(SPADES)
        );
        assertThat(hand.cards).isEqualTo(0b0000010010000_0000010010000_0000010000000_0000010010000L);
        assertThat(hand.ranks).isEqualTo((short) 0b0000010010000);
        assertThat(hand.count).isEqualTo((short) 2);
        assertThat(hand.rankCounts).isEqualTo(0b0000010000000_0000000010000_0000000000000_0000000000000L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0000_0000_0000_1101_0010);
        assertThat(hand.evaluate()).isEqualTo(0b0111_0000010000000_0000000010000);
        assertThat(hand.category()).isEqualTo(FOUR_OF_A_KIND);
        assertThat(hand.toString()).isEqualTo("(9♠,9♥,9♦,9♣,6♠)");
    }

    @Test
    void straightFlush() {
        var hand = hand(
            SEVEN.of(HEARTS),
            EIGHT.of(HEARTS),
            NINE.of(HEARTS),
            TEN.of(HEARTS),
            JACK.of(HEARTS),
            QUEEN.of(SPADES),
            KING.of(HEARTS)
        );
        assertThat(hand.cards).isEqualTo(0b0010000000000_0101111100000_0000000000000_0000000000000L);
        assertThat(hand.ranks).isEqualTo((short) 0b0111111100000);
        assertThat(hand.count).isEqualTo((short) 7);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000000_0000000000000_0111111100000L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0100_0000_0000_0000_0000_1000);
        assertThat(hand.evaluate()).isEqualTo(0b1000_0000000000000_0001000000000);
        assertThat(hand.category()).isEqualTo(STRAIGHT_FLUSH);
        assertThat(hand.toString()).isEqualTo("(J♥,T♥,9♥,8♥,7♥)");
    }

    @Test
    void royalFlush() {
        var hand = hand(
            TEN.of(CLUBS),
            JACK.of(CLUBS),
            QUEEN.of(CLUBS),
            KING.of(CLUBS),
            ACE.of(CLUBS),
            ACE.of(SPADES),
            ACE.of(HEARTS)
        );
        assertThat(hand.cards).isEqualTo(0b1000000000000_1000000000000_0000000000000_1111100000000L);
        assertThat(hand.ranks).isEqualTo((short) 0b1111100000000);
        assertThat(hand.count).isEqualTo((short) 5);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_1000000000000_0000000000000_0111100000000L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0001_0000_0000_0000_1100);
        assertThat(hand.evaluate()).isEqualTo(0b1000_0000000000000_1000000000000);
        assertThat(hand.category()).isEqualTo(STRAIGHT_FLUSH);
        assertThat(hand.toString()).isEqualTo("(A♣,K♣,Q♣,J♣,T♣)");
    }

    @Test
    void steelWheel() {
        var hand = hand(
            TWO.of(DIAMONDS),
            THREE.of(DIAMONDS),
            FOUR.of(DIAMONDS),
            FIVE.of(DIAMONDS),
            SIX.of(SPADES),
            SEVEN.of(HEARTS),
            ACE.of(DIAMONDS)
        );
        assertThat(hand.cards).isEqualTo(0b0000000010000_0000000100000_1000000001111_0000000000000L);
        assertThat(hand.ranks).isEqualTo((short) 0b1000000111111);
        assertThat(hand.count).isEqualTo((short) 7);
        assertThat(hand.rankCounts).isEqualTo(0b0000000000000_0000000000000_0000000000000_1000000111111L);
        assertThat(hand.suitCounts).isEqualTo(0b0000_0000_0010_0000_0000_0000_1100);
        assertThat(hand.evaluate()).isEqualTo(0b1000_0000000000000_0000000001000);
        assertThat(hand.category()).isEqualTo(STRAIGHT_FLUSH);
        assertThat(hand.toString()).isEqualTo("(5♦,4♦,3♦,2♦,A♦)");
    }
}
