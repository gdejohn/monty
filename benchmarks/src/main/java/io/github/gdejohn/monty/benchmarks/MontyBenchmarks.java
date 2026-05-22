/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.github.gdejohn.monty.benchmarks;

import io.github.gdejohn.monty.Card;
import io.github.gdejohn.monty.Card.Rank;
import io.github.gdejohn.monty.Category;
import io.github.gdejohn.monty.Deck;
import io.github.gdejohn.monty.Hand;
import io.github.gdejohn.monty.Monty;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.Map;
import java.util.Spliterator;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.random.RandomGenerator.SplittableGenerator;

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
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.BinaryOperator.maxBy;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Thread;

@BenchmarkMode(Throughput)
@OutputTimeUnit(SECONDS)
@Threads(1)
public class MontyBenchmarks {
    private static final String DEFAULT_ALGORITHM = "L128X128MixRandom";

    private static final long TRIALS = Long.MAX_VALUE;

    private static Monty monty() {
        return Monty.pocket(TWO.of(CLUBS), SEVEN.of(HEARTS)).players(6);
    }

    @State(Thread)
    public static class Samples {
        private static final Spliterator.OfInt SPLITERATOR =
            monty().limit(TRIALS)
                   .spliterator();

        final Spliterator.OfInt spliterator = SPLITERATOR.trySplit();
    }

    @Benchmark
    public boolean sample(Samples samples, Blackhole blackhole) {
        return samples.spliterator.tryAdvance((IntConsumer) blackhole::consume);
    }

    /// Use the default implementation of [SplittableGenerator#nextInt(int)].
    @State(Thread)
    public static class BaselineSamples {
        private static final Spliterator.OfInt SPLITERATOR =
            monty().rng(SplittableGenerator.of(DEFAULT_ALGORITHM))
                   .limit(TRIALS)
                   .spliterator();

        final Spliterator.OfInt spliterator = SPLITERATOR.trySplit();
    }

    @Benchmark
    public boolean sampleBaseline(BaselineSamples samples, Blackhole blackhole) {
        return samples.spliterator.tryAdvance((IntConsumer) blackhole::consume);
    }

    @State(Thread)
    public static class FastDealer {
        /// A sequence of cards that fully exercises the hand evaluator.
        ///
        /// The only branch in the evaluator is a 14-entry jump table. The
        /// entries corresponding to the hands made from the 32 contiguous
        /// seven-card subsequences in this 38-card sequence approximate a
        /// representative sample of the distribution of entries corresponding
        /// to random hands made from any seven cards.
        ///
        /// The first 14 seven-card subsequences correspond one-to-one with the
        /// jump table entries. The comment next to the first card in each of
        /// those subsequences lists the total number of subsequences
        /// associated with that entry, the ratio of the probabilities that the
        /// entry corresponds to a hand made from these subsequences versus any
        /// possible hand, and a description of the class of hands that map to
        /// that entry.
        private static final Card[] cards = {
            QUEEN.of(CLUBS),    //  1,   34:1 (0.031 / 0.000924), 3 2 2
            QUEEN.of(SPADES),   //  1,   76:1 (0.031 / 0.000410), 3 3 1
             KING.of(CLUBS),    //  1, 6300:1 (0.031 / 0.000005), 4 3
             KING.of(SPADES),   //  1,  100:1 (0.031 / 0.000308), 4 2 1
              ACE.of(CLUBS),    //  1,   23:1 (0.031 / 0.001368), 4 1 1 1
             KING.of(DIAMONDS), //  1,  1.3:1 (0.031 / 0.024627), 3 2 1 1
              ACE.of(SPADES),   //  1, 0.65:1 (0.031 / 0.048299), 3 1 1 1 1
              ACE.of(HEARTS),   //  1,  100:1 (0.031 / 0.000311), straight flush
              ACE.of(DIAMONDS), //  1, 0.68:1 (0.031 / 0.046194), straight
            QUEEN.of(HEARTS),   //  1,  1.0:1 (0.031 / 0.030255), flush
             JACK.of(HEARTS),   //  3, 0.54:1 (0.094 / 0.174119), 1 1 1 1 1 1 1
             KING.of(HEARTS),   // 14,  1.0:1 (0.438 / 0.438225), 2 1 1 1 1 1
             NINE.of(CLUBS),    //  4, 0.58:1 (0.125 / 0.216485), 2 2 1 1 1
              TEN.of(HEARTS),   //  1,  1.7:1 (0.031 / 0.018470), 2 2 2 1
            EIGHT.of(CLUBS),
              SIX.of(HEARTS),
             FIVE.of(SPADES),
              SIX.of(CLUBS),
            EIGHT.of(SPADES),
              TEN.of(CLUBS),
            THREE.of(DIAMONDS),
             FIVE.of(DIAMONDS),
             FOUR.of(CLUBS),
            THREE.of(SPADES),
            SEVEN.of(DIAMONDS),
            EIGHT.of(DIAMONDS),
             FOUR.of(SPADES),
              TWO.of(CLUBS),
             JACK.of(DIAMONDS),
              TEN.of(DIAMONDS),
              TEN.of(SPADES),
              SIX.of(SPADES),
             NINE.of(SPADES),
             FOUR.of(DIAMONDS),
              TWO.of(SPADES),
            THREE.of(CLUBS),
             JACK.of(CLUBS),
              TWO.of(DIAMONDS)
        };

        private int state = 1; // must be odd for power-of-two modulus

        Hand hand() {
            state *= 0x93d765dd; // state value repeats after 2^30 iterations
            int offset = state >>> -5; // [0..31]
            var hand = Hand.empty();
            for (int n = 0; n < 7; n++) {
                hand = hand.add(cards[offset + n]);
            }
            return hand;
        }
    }

    /// Evaluate pseudorandomly sampled hands.
    ///
    /// The overhead of generating high quality pseudorandom integers in
    /// varying intervals to determine a hand's cards one by one is significant
    /// compared to evaluating the hand, so this benchmark uses a simple Lehmer
    /// generator (m = 2^32, c = 0) to determine all seven cards at once,
    /// pseudorandomly choosing one of the 32 contiguous seven-card
    /// subsequences in [FastDealer#cards] by its offset with just a single
    /// integer multiplication to update the generator state and an unsigned
    /// shift to extract the five high-order bits. Despite the generator's
    /// statistical shortcomings, it is more than sufficient to prevent branch
    /// target prediction from confounding the benchmark results.
    ///
    /// @see <a href="https://doi.org/10.1002/spe.3030">Computationally easy,
    ///      spectrally good multipliers for congruential pseudorandom number
    ///      generators</a>
    @Benchmark
    public int evaluateRandom(FastDealer dealer) {
        return dealer.hand().evaluate();
    }

    @State(Thread)
    public static class Dealer {
        final Deck deck = new Deck();
    }

    /// Evaluate hands using high quality pseudorandom numbers to shuffle the deck.
    @Benchmark
    public int evaluateShuffled(Dealer dealer) {
        dealer.deck.shuffle();
        var hand = Hand.empty();
        for (int n = 0; n < 7; n++) {
            hand = hand.add(dealer.deck.deal());
        }
        return hand.evaluate();
    }

    @State(Thread)
    public static class BaselineDealer {
        final Deck deck = new Deck(SplittableGenerator.of(DEFAULT_ALGORITHM));
    }

    @Benchmark
    public int evaluateBaseline(BaselineDealer dealer) {
        dealer.deck.shuffle();
        var hand = Hand.empty();
        for (int n = 0; n < 7; n++) {
            hand = hand.add(dealer.deck.deal());
        }
        return hand.evaluate();
    }

    private static final int PLAYERS = 23;

    @Benchmark
    @OperationsPerInvocation(PLAYERS)
    public void evaluateShared(Dealer dealer, Blackhole blackhole) {
        Deck deck = dealer.deck;
        deck.shuffle();
        var hand = Hand.empty();
        for (int n = 0; n < 5; n++) {
            hand = hand.add(deck.deal());
        }
        for (int n = 0; n < PLAYERS; n++) {
            blackhole.consume(hand.add(deck.deal()).add(deck.deal()).evaluate());
        }
    }

    @Benchmark
    public int evaluatePartial(FastDealer dealer) {
        return dealer.hand().size();
    }

    private static final Card[] cards = {
        EIGHT.of(CLUBS),
        EIGHT.of(SPADES),
          SIX.of(CLUBS),
          SIX.of(HEARTS),
          TEN.of(HEARTS),
         NINE.of(CLUBS),
         FIVE.of(SPADES)
    };

    @Benchmark
    public int evaluateConstant() {
        var hand = Hand.empty();
        for (int index = 0; index < 7; index++) {
            hand = hand.add(cards[index]);
        }
        return hand.evaluate();
    }

    @Benchmark
    public Object evaluateNaive(Dealer dealer) {
        record Hand(Category category, Rank... ranks) {
            static Hand of(Card... cards) {
                Map<Rank,Long> counts = Arrays.stream(cards).collect(
                    groupingBy(Card::rank, counting())
                );
                Rank[] ranks = counts.keySet().stream().sorted(
                    comparing(
                        (Function<Rank,Long>) counts::get
                    ).thenComparing(naturalOrder()).reversed()
                ).toArray(Rank[]::new);
                boolean straight = ranks[0].ordinal() - ranks[ranks.length - 1].ordinal() == 4;
                boolean wheel = ranks[0] == ACE && ranks[1] == FIVE;
                boolean flush = Arrays.stream(cards).map(Card::suit).distinct().count() == 1;
                return counts.get(ranks[0]) == 4 ? new Hand(FOUR_OF_A_KIND, ranks)
                     : ranks.length == 2 ? new Hand(FULL_HOUSE, ranks)
                     : counts.get(ranks[0]) == 3 ? new Hand(THREE_OF_A_KIND, ranks)
                     : ranks.length == 3 ? new Hand(TWO_PAIR, ranks)
                     : ranks.length == 4 ? new Hand(ONE_PAIR, ranks)
                     : straight ? new Hand(flush ? STRAIGHT_FLUSH : STRAIGHT, ranks[0])
                     : wheel ? new Hand(flush ? STRAIGHT_FLUSH : STRAIGHT, FIVE)
                     : new Hand(flush ? FLUSH : HIGH_CARD, ranks);
            }
        }

        dealer.deck.shuffle();
        var cards = new Card[7];
        for (int n = 0; n < 7; n++) {
            cards[n] = dealer.deck.deal();
        }
        BinaryOperator<Hand> max = maxBy(
            nullsFirst(
                comparing(Hand::category).thenComparing(Hand::ranks, Arrays::compare)
            )
        );
        Hand hand = null;
        for (int a = 0; a < 3; a++) {
            for (int b = a + 1; b < 4; b++) {
                for (int c = b + 1; c < 5; c++) {
                    for (int d = c + 1; d < 6; d++) {
                        for (int e = d + 1; e < 7; e++) {
                            hand = max.apply(
                                hand,
                                Hand.of(cards[a], cards[b], cards[c], cards[d], cards[e])
                            );
                        }
                    }
                }
            }
        }
        return hand;
    }

    @Benchmark
    @OperationsPerInvocation(52)
    public void dealCard(Dealer dealer, Blackhole blackhole) {
        dealer.deck.shuffle();
        for (int n = 0; n < 52; n++) {
            blackhole.consume(dealer.deck.deal());
        }
    }

    @Benchmark
    @OperationsPerInvocation(52)
    public void dealCardBaseline(BaselineDealer dealer, Blackhole blackhole) {
        dealer.deck.shuffle();
        for (int n = 0; n < 52; n++) {
            blackhole.consume(dealer.deck.deal());
        }
    }

    @Benchmark
    public void dealHand(Dealer dealer, Blackhole blackhole) {
        dealer.deck.shuffle();
        for (int n = 0; n < 7; n++) {
            blackhole.consume(dealer.deck.deal());
        }
    }

    @Benchmark
    public void dealHandBaseline(BaselineDealer dealer, Blackhole blackhole) {
        dealer.deck.shuffle();
        for (int n = 0; n < 7; n++) {
            blackhole.consume(dealer.deck.deal());
        }
    }

    @Benchmark
    public int hashCode(FastDealer dealer) {
        return dealer.hand().hashCode();
    }
}
