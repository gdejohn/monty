package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Deck.Generator;

import java.util.Arrays;
import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Integer.signum;
import static java.util.Objects.requireNonNullElseGet;

/// Estimate equity and expected value for Texas hold 'em.
public sealed abstract class Monty {
    /// The hero's two hole cards.
    private final Card[] pocket;

    /// The partial hand made from the community cards already dealt.
    private final Hand partial;

    /// The number of community cards already dealt.
    private final int board;

    /// The total number of players, including the hero.
    private final int players;

    /// The source of random numbers to use for the simulation.
    private final SplittableGenerator rng;

    private static long mask(Hand partial, Card[] pocket) {
        return partial.mask() | pocket[0].mask() | pocket[1].mask();
    }

    private Monty(SplittableGenerator rng, Card[] pocket, Hand partial, int board, int players) {
        if (players < 2 || players > 23) {
            throw new IllegalArgumentException(
                "players = %d (must be greater than 1 and less than 24)".formatted(players)
            );
        } else if (Long.bitCount(mask(partial, pocket)) != board + 2) {
            throw new IllegalArgumentException(
                "board = %s, pocket = %s (must be disjoint)".formatted(
                    Card.string(partial.stream()),
                    Card.string(Arrays.stream(pocket))
                )
            );
        }
        this.rng = rng;
        this.pocket = pocket;
        this.partial = partial;
        this.board = board;
        this.players = players;
    }

    private Monty(Monty monty, Hand partial, int board) {
        this(monty.rng, monty.pocket, partial, board, monty.players);
    }

    private Monty(Monty monty, int players, int board) {
        this(monty.rng, monty.pocket, monty.partial, board, players);
    }

    private Monty(Monty monty, SplittableGenerator rng, int board) {
        this(rng, monty.pocket, monty.partial, board, monty.players);
    }

    /// Specify the hero's two hole cards.
    public static Preflop pocket(Card first, Card second) {
        if (first.offset() == second.offset()) {
            throw new IllegalArgumentException(
                "first = %s, second = %s (must not be equal)".formatted(first, second)
            );
        }
        return new Preflop(first, second);
    }

    /// Copy this `Monty` instance but change the number of players.
    public abstract Monty players(int players);

    /// Copy this `Monty` instance but change the random number generator.
    public abstract Monty rng(SplittableGenerator rng);

    /// No community cards on the board.
    public static final class Preflop extends Monty {
        private Preflop(Card... pocket) {
            super(null, pocket, Hand.empty(), 0, 2);
        }

        private Preflop(Preflop preflop, SplittableGenerator rng) {
            super(preflop, rng, 0);
        }

        private Preflop(Preflop preflop, int players) {
            super(preflop, players, 0);
        }

        /// Specify the first three community cards on the board.
        public Flop flop(Card first, Card second, Card third) {
            return new Flop(this, super.partial.add(first).add(second).add(third));
        }

        @Override
        public Preflop players(int players) {
            return new Preflop(this, players);
        }

        @Override
        public Preflop rng(SplittableGenerator rng) {
            return new Preflop(this, rng);
        }
    }

    /// Three community cards on the board.
    public static final class Flop extends Monty {
        private Flop(Preflop preflop, Hand partial) {
            super(preflop, partial, 3);
        }

        private Flop(Flop flop, int players) {
            super(flop, players, 3);
        }

        private Flop(Flop flop, SplittableGenerator rng) {
            super(flop, rng, 3);
        }

        /// Specify the fourth community card on the board.
        public Turn turn(Card fourth) {
            return new Turn(this, super.partial.add(fourth));
        }

        @Override
        public Flop players(int players) {
            return new Flop(this, players);
        }

        @Override
        public Flop rng(SplittableGenerator rng) {
            return new Flop(this, rng);
        }
    }

    /// Four community cards on the board.
    public static final class Turn extends Monty {
        private Turn(Flop flop, Hand partial) {
            super(flop, partial, 4);
        }

        private Turn(Turn turn, int players) {
            super(turn, players, 4);
        }

        private Turn(Turn turn, SplittableGenerator rng) {
            super(turn, rng, 4);
        }

        /// Specify the fifth and final community card on the board.
        public River river(Card fifth) {
            return new River(this, super.partial.add(fifth));
        }

        @Override
        public Turn players(int players) {
            return new Turn(this, players);
        }

        @Override
        public Turn rng(SplittableGenerator rng) {
            return new Turn(this, rng);
        }
    }

    /// All five community cards on the board.
    public static final class River extends Monty {
        private River(Turn turn, Hand partial) {
            super(turn, partial, 5);
        }

        private River(River river, int players) {
            super(river, players, 5);
        }

        private River(River river, SplittableGenerator rng) {
            super(river, rng, 5);
        }

        @Override
        public River players(int players) {
            return new River(this, players);
        }

        @Override
        public River rng(SplittableGenerator rng) {
            return new River(this, rng);
        }
    }

    /// Run a given number of trials in parallel and summarize the results.
    ///
    /// @see #stream()
    public Showdown limit(long trials) {
        return stream().limit(trials).collect(
            () -> new Showdown(players),
            Showdown::accumulate,
            Showdown::combine
        );
    }

    /// A lazy, infinite, parallel stream of simulated game outcomes.
    ///
    /// The outcome of a game is represented by a nonnegative integer indicating the number of
    /// players that the player with the given [hole cards][#pocket(Card,Card)] splits the pot
    /// with, including that player: 0 means that player lost, 1 means that player won, and n > 1
    /// means an n-way tie.
    ///
    /// @see #limit(long)
    public IntStream stream() {
        Stream<Card> cards = Card.all().filter(card -> !card.in(mask(partial, pocket)));
        var deck = new Deck(requireNonNullElseGet(rng, Generator::new), cards);
        Spliterator.OfInt simulation = new Simulation(deck);
        boolean parallel = true;
        return StreamSupport.intStream(simulation, parallel);
    }

    private final class Simulation implements Spliterator.OfInt {
        private final Deck deck;

        private long trials;

        private Simulation(Deck deck, long trials) {
            this.deck = deck;
            this.trials = trials;
        }

        private Simulation(Deck deck) {
            this(deck, Long.MAX_VALUE);
        }

        @Override
        public int characteristics() {
            return IMMUTABLE | NONNULL | SIZED | SUBSIZED;
        }

        @Override
        public long estimateSize() {
            return trials;
        }

        @Override
        public Spliterator.OfInt trySplit() {
            return trials <= 1 ? null : new Simulation(
                deck.split(),
                trials - (trials >>>= 1)
            );
        }

        @Override
        public boolean tryAdvance(IntConsumer consumer) {
            if (trials < 1) {
                return false;
            }
            trials--;
            deck.shuffle();
            Hand hand = partial;
            switch (board) {
                case 0: hand = hand.add(deck.deal())
                                   .add(deck.deal())
                                   .add(deck.deal());
                case 3: hand = hand.add(deck.deal());
                case 4: hand = hand.add(deck.deal());
                case 5: break;
            }
            int player = hand.add(pocket[0]).add(pocket[1]).evaluate();
            int split = 1;
            for (int n = 1; n < players; n++) {
                int opponent = hand.add(deck.deal()).add(deck.deal()).evaluate();
                switch (signum(player - opponent)) {
                    case +0: split++;
                    case +1: continue;
                    case -1: split = 0;
                }
                break;
            }
            consumer.accept(split);
            return true;
        }
    }

    /// A summary of the results of a simulation.
    public static final class Showdown {
        /// Greatest common divisor.
        private static long gcd(long a, long b) {
            return b == 0 ? a : gcd(b, a % b);
        }

        private static long[][] pots() {
            var pots = new long[24][];
            long lcm = 1L; // least common multiple
            for (int players = 2; players < pots.length; players++) {
                lcm *= players / gcd(lcm, players); // lcm of [1..players]
                pots[players] = new long[players + 1];
                for (int split = 1; split <= players; split++) {
                    pots[players][split] = lcm / split;
                }
            }
            return pots;
        }

        private static final long[][] pots = pots();

        private final long[] pot;

        private long winnings;

        private long trials;

        private Showdown(int players) {
            this.pot = pots[players];
            this.winnings = 0;
            this.trials = 0;
        }

        private void accumulate(int split) {
            winnings = Math.addExact(winnings, pot[split]);
            trials++;
        }

        private void combine(Showdown showdown) {
            winnings = Math.addExact(winnings, showdown.winnings);
            trials += showdown.trials;
        }

        /// The fraction of the pot won on average across every trial.
        public double equity() {
            return (double) winnings / trials / pot[1];
        }

        /// The ratio of estimated winnings to the size of the raise.
        public double expectedValue(long pot, long raise) {
            if (raise < 1) {
                throw new IllegalArgumentException(
                    "raise = %d (must be positive)".formatted(raise)
                );
            } else if (pot < raise) {
                throw new IllegalArgumentException(
                    "pot < raise (pot = %d, raise = %d)".formatted(pot, raise)
                );
            }
            return equity() * (pot + raise) / raise;
        }
    }
}
