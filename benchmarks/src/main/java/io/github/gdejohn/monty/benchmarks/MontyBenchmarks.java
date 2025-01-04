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
import io.github.gdejohn.monty.Deck;
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
import java.util.random.RandomGenerator.SplittableGenerator;
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
public class MontyBenchmarks {
    private static final String DEFAULT_RNG = "L128X128MixRandom";

    private static IntStream stream() {
        return Monty.pocket(EIGHT.of(CLUBS), NINE.of(CLUBS))
                    .flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
                    .stream();
    }

    private static final Spliterator.OfInt spliterator = stream().spliterator();

    @State(Thread)
    public static class Simulation {
        public final Spliterator.OfInt spliterator = MontyBenchmarks.spliterator.trySplit();
    }

    @Benchmark
    public boolean simulate(Simulation state, Blackhole blackhole) {
        return state.spliterator.tryAdvance((IntConsumer) blackhole::consume);
    }

    private static IntStream streamDefault() {
        return Monty.pocket(EIGHT.of(CLUBS), NINE.of(CLUBS))
                    .flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
                    .rng(SplittableGenerator.of(DEFAULT_RNG))
                    .stream();
    }

    private static final Spliterator.OfInt spliteratorDefault = streamDefault().spliterator();

    @State(Thread)
    public static class SimulationDefault {
        public final Spliterator.OfInt spliterator = MontyBenchmarks.spliteratorDefault.trySplit();
    }

    @Benchmark
    public boolean simulateDefault(SimulationDefault state, Blackhole blackhole) {
        return state.spliterator.tryAdvance((IntConsumer) blackhole::consume);
    }

    /// Fast pseudorandom sampling of a representative hand distribution.
    ///
    /// The overhead of generating multiple pseudorandom integers in varying intervals to
    /// determine a hand's cards one by one is significant compared to evaluating the hand, so
    /// this class uses a simple Lehmer generator (m = 2^32, c = 0) to pseudorandomly choose one
    /// of the 32 contiguous seven-card subsequences in [cards][#cards] by its starting index with
    /// just a single integer multiplication to update the generator state and an unsigned shift
    /// to extract the five high-order bits. Despite the generator's statistical shortcomings, it
    /// is more than sufficient to prevent branch target prediction from confounding the benchmark
    /// results.
    ///
    /// @see <a href="https://onlinelibrary.wiley.com/doi/full/10.1002/spe.3030">Computationally
    ///      easy, spectrally good multipliers for congruential pseudorandom number generators
    ///      (Guy L. Steele Jr. & Sebastiano Vigna)</a>
    @State(Thread)
    public static class FastDealer {
        /// A sequence of cards that fully exercises the hand evaluator.
        ///
        /// The only branch in the evaluator is a 14-entry jump table. The entries corresponding
        /// to the hands made from the 32 contiguous seven-card subsequences in this 38-card
        /// sequence approximate a representative sample of the distribution of entries
        /// corresponding to random hands made from any seven cards.
        ///
        /// The first 14 subsequences correspond one-to-one with the jump table entries. The
        /// comment next to the first card in each of those subsequences lists the total number of
        /// subsequences associated with that entry, the ratio of the probabilities that the entry
        /// corresponds to a hand made from these subsequences versus any possible hand, and a
        /// description of the class of hands that map to that entry.
        private static final Card[] cards = {
            QUEEN.of(CLUBS),    //  1, 34   (0.031 / 0.000924), 3 2 2
            QUEEN.of(SPADES),   //  1, 76   (0.031 / 0.000410), 3 3 1
             KING.of(CLUBS),    //  1, 6300 (0.031 / 0.000005), 4 3
             KING.of(SPADES),   //  1, 100  (0.031 / 0.000308), 4 2 1
              ACE.of(CLUBS),    //  1, 23   (0.031 / 0.001368), 4 1 1 1
             KING.of(DIAMONDS), //  1, 1.3  (0.031 / 0.024627), 3 2 1 1
              ACE.of(SPADES),   //  1, 0.65 (0.031 / 0.048299), 3 1 1 1 1
              ACE.of(HEARTS),   //  1, 100  (0.031 / 0.000311), straight flush
              ACE.of(DIAMONDS), //  1, 0.68 (0.031 / 0.046194), straight
            QUEEN.of(HEARTS),   //  1, 1.0  (0.031 / 0.030255), flush
             JACK.of(HEARTS),   //  3, 0.54 (0.094 / 0.174119), 1 1 1 1 1 1 1
             KING.of(HEARTS),   // 14, 1.0  (0.438 / 0.438225), 2 1 1 1 1 1
             NINE.of(CLUBS),    //  4, 0.58 (0.125 / 0.216485), 2 2 1 1 1
              TEN.of(HEARTS),   //  1, 1.7  (0.031 / 0.018470), 2 2 2 1
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

        /// The generator's internal state.
        private int state = 1; // seed must be odd for power-of-two modulus

        /// Make a hand from a contiguous subsequence of [cards][#cards].
        public Hand deal() {
            state *= 0x93d765dd; // state value repeats after 2^30 iterations
            int index = state >>> -5; // [0..31]
            var hand = Hand.empty();
            for (int n = 0; n < 7; n++) {
                hand = hand.add(cards[index++]);
            }
            return hand;
        }
    }

    /// Benchmark the evaluation of random hands.
    @Benchmark
    public int evaluateFast(FastDealer dealer) {
        var hand = dealer.deal();
        return hand.evaluate();
    }

    @State(Thread)
    public static class RandomDealer {
        public final Deck deck = new Deck();
    }

    @Benchmark
    public int evaluateRandom(RandomDealer dealer) {
        dealer.deck.shuffle();
        var hand = Hand.empty();
        for (int n = 0; n < 7; n++) {
            hand = hand.add(dealer.deck.deal());
        }
        return hand.evaluate();
    }

    @Benchmark
    public Card deal(RandomDealer dealer) {
        dealer.deck.shuffle();
        return dealer.deck.deal();
    }

    @Benchmark
    public void evaluateShared(RandomDealer dealer, Blackhole blackhole) {
        dealer.deck.shuffle();
        Hand partial = Hand.empty();
        for (int n = 0; n < 5; n++) {
            partial = partial.add(dealer.deck.deal());
        }
        for (int players = 0; players < 10; players++) {
            Hand hand = partial.add(dealer.deck.deal()).add(dealer.deck.deal());
            blackhole.consume(hand.evaluate());
        }
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
    public int evaluateFixed() {
        var hand = Hand.empty();
        for (int index = 0; index < 7; index++) {
            hand = hand.add(cards[index]);
        }
        return hand.evaluate();
    }

    @Benchmark
    public Hand partialEvaluation() {
        var hand = Hand.empty();
        for (int index = 0; index < 7; index++) {
            hand = hand.add(cards[index]);
        }
        return hand;
    }

    @State(Thread)
    public static class DefaultDealer {
        public final Deck deck = new Deck(SplittableGenerator.of(DEFAULT_RNG));
    }

    @Benchmark
    public int evaluateDefaultBoundedRNG(DefaultDealer dealer) {
        dealer.deck.shuffle();
        var hand = Hand.empty();
        for (int n = 0; n < 7; n++) {
            hand = hand.add(dealer.deck.deal());
        }
        return hand.evaluate();
    }

    @Benchmark
    public Card dealDefault(DefaultDealer dealer) {
        dealer.deck.shuffle();
        return dealer.deck.deal();
    }
}
