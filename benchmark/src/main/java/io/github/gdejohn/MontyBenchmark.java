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

package io.github.gdejohn;

import io.github.gdejohn.monty.Card;
import io.github.gdejohn.monty.Hand;
import io.github.gdejohn.monty.Monty;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

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
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.openjdk.jmh.annotations.Mode.Throughput;
import static org.openjdk.jmh.annotations.Scope.Thread;

@BenchmarkMode(Throughput)
@OutputTimeUnit(SECONDS)
@Threads(1)
public class MontyBenchmark {
    private static IntStream stream() {
        return Monty.pocket(EIGHT.of(CLUBS), NINE.of(CLUBS))
                    .flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
                    .headsUp()
                    .stream();
    }

    private static final Spliterator.OfInt spliterator = stream().spliterator();

    @State(Thread)
    public static class Simulation {
        public final Spliterator.OfInt spliterator = MontyBenchmark.spliterator.trySplit();
    }

    @Benchmark
    public boolean simulate(Simulation state, Blackhole blackhole) {
        return state.spliterator.tryAdvance((IntConsumer) blackhole::consume);
    }

    /**
     * Efficient pseudorandom sampling of a representative hand distribution.
     * <p>
     * The overhead of generating multiple pseudorandom integers in varying intervals to determine
     * a hand's cards one by one is significant compared to evaluating the hand, so this class
     * implements a simple Lehmer generator (m = 2^32, c = 0) to choose one of the 32 contiguous
     * seven-card subsequences in {@link #cards} by its starting index with just a single integer
     * multiplication to update the generator state and an unsigned shift to extract the five
     * high-order bits. Despite the generator's statistical shortcomings, it is more than
     * sufficient to prevent branch target prediction from confounding the benchmark results.
     */
    @State(Thread)
    public static class Deck {
        /**
         * A sequence of cards that fully exercises the hand evaluator.
         * <p>
         * The only branch in the evaluator is a 14-entry jump table indexed by hashing the hand's
         * cards. The entries corresponding to the 32 contiguous seven-card subsequences in this
         * 38-card sequence approximate a representative sample of the distribution of jump table
         * entries corresponding to sets of any seven cards chosen uniformly at random without
         * replacement.
         * <p>
         * The first 14 subsequences correspond one-to-one with the jump table entries. The
         * accompanying comments describe the hands that map to those entries and indicate in
         * parentheses the total number of subsequences that map to them and the ratio of the
         * probabilities that they are mapped to by these subsequences versus any seven-card hand.
         */
        private static final Card[] cards = {
            QUEEN.of(CLUBS),   //          3 2 2  (1, 0.03125 / 0.000924 =   34)
            QUEEN.of(SPADES),  //          3 3 1  (1, 0.03125 / 0.000410 =   76)
            KING.of(CLUBS),    //            4 3  (1, 0.03125 / 0.000005 = 6300)
            KING.of(SPADES),   //          4 2 1  (1, 0.03125 / 0.000308 =  100)
            ACE.of(CLUBS),     //        4 1 1 1  (1, 0.03125 / 0.001368 =   23)
            KING.of(DIAMONDS), //        3 2 1 1  (1, 0.03125 / 0.024627 =  1.3)
            ACE.of(SPADES),    //      3 1 1 1 1  (1, 0.03125 / 0.048299 = 0.65)
            ACE.of(HEARTS),    // straight flush  (1, 0.03125 / 0.000311 =  100)
            ACE.of(DIAMONDS),  //       straight  (1, 0.03125 / 0.046194 = 0.68)
            QUEEN.of(HEARTS),  //          flush  (1, 0.03125 / 0.030255 =  1.0)
            JACK.of(HEARTS),   //  1 1 1 1 1 1 1  (3, 0.09375 / 0.174119 = 0.54)
            KING.of(HEARTS),   //    2 1 1 1 1 1 (14, 0.43750 / 0.438225 =  1.0)
            NINE.of(CLUBS),    //      2 2 1 1 1  (4, 0.12500 / 0.216485 = 0.58)
            TEN.of(HEARTS),    //        2 2 2 1  (1, 0.03125 / 0.018470 =  1.7)
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

        /** The generator's internal state. */
        private int state = 1; // seed must be odd for power-of-two modulus

        /**
         * Choose a contiguous subsequence of {@link #cards} to make a hand.
         *
         * @see <a href="https://onlinelibrary.wiley.com/doi/full/10.1002/spe.3030">
         *      Guy L. Steele Jr. and Sebastiano Vigna: Computationally easy, spectrally good
         *      multipliers for congruential pseudorandom number generators</a>
         */
        public Hand deal() {
            state *= 0x93D765DD; // state value repeats after 2^30 hands dealt
            int index = state >>> -5; // [0,32)
            var hand = Hand.empty();
            for (int n = 0; n < 7; n++) {
                hand = hand.add(cards[index++]);
            }
            return hand;
        }
    }

    /** Benchmark the evaluation of random hands. */
    @Benchmark
    public int evaluate(Deck deck) {
        var hand = deck.deal();
        return hand.evaluate();
    }
}
