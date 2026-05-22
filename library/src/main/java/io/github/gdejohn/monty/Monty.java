package io.github.gdejohn.monty;

import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.function.IntConsumer;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static java.lang.Integer.signum;
import static java.lang.Math.log;
import static java.math.MathContext.DECIMAL128;
import static java.math.RoundingMode.CEILING;

/// Estimate equity and expected value for Texas hold 'em.
public sealed abstract class Monty {
    /// Hole cards.
    private record Pocket(Card first, Card second) {
        /// True if and only if the given `card` is equal to one of these hole cards.
        boolean contains(Card card) {
            return card.equals(first) || card.equals(second);
        }
    }

    /// Default error magnitude.
    private static final double DEFAULT_ERROR = 0.001d;

    /// Default confidence level.
    private static final double DEFAULT_CONFIDENCE = 0.999_999d;

    /// Optional source of random numbers to use for shuffling.
    @Nullable
    private final SplittableGenerator rng;

    /// The total number of players, including the hero.
    private final int players;

    /// The hero's hole cards.
    private final Pocket pocket;

    /// The partial hand made from the community cards already dealt.
    protected final Hand board;

    private Monty(@Nullable SplittableGenerator rng, int players, Pocket pocket, Hand board) {
        if (players < 2 || players > 23) {
            throw new IllegalArgumentException(
                "players=%,d must be greater than 1 and less than 24".formatted(
                    players
                )
            );
        } else if (board.contains(pocket.first()) || board.contains(pocket.second())) {
            throw new IllegalArgumentException(
                "board=%s and pocket=(%s,%s) must be disjoint".formatted(
                    board,
                    pocket.first(),
                    pocket.second()
                )
            );
        }
        this.rng = rng;
        this.players = players;
        this.pocket = pocket;
        this.board = board;
    }

    private Monty(Monty monty, Hand board) {
        this(monty.rng, monty.players, monty.pocket, board);
    }

    private Monty(Monty monty, int players) {
        this(monty.rng, players, monty.pocket, monty.board);
    }

    private Monty(Monty monty, SplittableGenerator rng) {
        this(rng, monty.players, monty.pocket, monty.board);
    }

    /// Specify your hole cards.
    public static Preflop pocket(Card first, Card second) {
        if (first.equals(second)) {
            throw new IllegalArgumentException(
                "pocket=(%s,%s) must not contain duplicates".formatted(
                    first,
                    second
                )
            );
        }
        return new Preflop(new Pocket(first, second));
    }

    /// Copy this `Monty` instance but change the number of players.
    ///
    /// If the number of players isn't specified, the default value is `2`.
    public abstract Monty players(int players);

    /// Copy this `Monty` instance but change the random number generator.
    public abstract Monty rng(SplittableGenerator rng);

    /// First betting round, zero cards on the board.
    public static final class Preflop extends Monty {
        /// The default number of players.
        private static final int HEADS_UP = 2;

        private Preflop(Pocket pocket) {
            super(null, HEADS_UP, pocket, Hand.empty());
        }

        private Preflop(Preflop preflop, SplittableGenerator rng) {
            super(preflop, rng);
        }

        private Preflop(Preflop preflop, int players) {
            super(preflop, players);
        }

        @Override
        public Preflop players(int players) {
            return new Preflop(this, players);
        }

        @Override
        public Preflop rng(SplittableGenerator rng) {
            return new Preflop(this, rng);
        }

        /// Specify the first three community cards on the board.
        public Flop flop(Card first, Card second, Card third) {
            if (Long.bitCount(first.mask() | second.mask() | third.mask()) != 3) {
                throw new IllegalArgumentException(
                    "flop=(%s,%s,%s) must not contain duplicates".formatted(
                        first,
                        second,
                        third
                    )
                );
            }
            return new Flop(this, board.add(first).add(second).add(third));
        }
    }

    /// Second betting round, three cards on the board.
    public static final class Flop extends Monty {
        private Flop(Preflop preflop, Hand board) {
            super(preflop, board);
        }

        private Flop(Flop flop, int players) {
            super(flop, players);
        }

        private Flop(Flop flop, SplittableGenerator rng) {
            super(flop, rng);
        }

        @Override
        public Flop players(int players) {
            return new Flop(this, players);
        }

        @Override
        public Flop rng(SplittableGenerator rng) {
            return new Flop(this, rng);
        }

        /// Specify the fourth community card on the board.
        public Turn turn(Card fourth) {
            if (board.contains(fourth)) {
                throw new IllegalArgumentException(
                    "turn=%s must not already be on the board=%s".formatted(
                        fourth,
                        board
                    )
                );
            }
            return new Turn(this, board.add(fourth));
        }
    }

    /// Third betting round, four cards on the board.
    public static final class Turn extends Monty {
        private Turn(Flop flop, Hand board) {
            super(flop, board);
        }

        private Turn(Turn turn, int players) {
            super(turn, players);
        }

        private Turn(Turn turn, SplittableGenerator rng) {
            super(turn, rng);
        }

        @Override
        public Turn players(int players) {
            return new Turn(this, players);
        }

        @Override
        public Turn rng(SplittableGenerator rng) {
            return new Turn(this, rng);
        }

        /// Specify the fifth and final community card on the board.
        public River river(Card fifth) {
            if (board.contains(fifth)) {
                throw new IllegalArgumentException(
                    "river=%s must not already be on the board=%s".formatted(
                        fifth,
                        board
                    )
                );
            }
            return new River(this, board.add(fifth));
        }
    }

    /// Final betting round, five cards on the board.
    public static final class River extends Monty {
        private River(Turn turn, Hand board) {
            super(turn, board);
        }

        private River(River river, int players) {
            super(river, players);
        }

        private River(River river, SplittableGenerator rng) {
            super(river, rng);
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

    /// Compute a confidence interval for the hero's estimated equity.
    ///
    /// The magnitude of the difference between the true equity and the
    /// estimate is less than the given `error` with the given `confidence`
    /// level.
    ///
    /// @see <a href="https://artowen.su.domains/mc/Ch-intro.pdf">Monte Carlo
    ///      theory, methods and examples (Corollary 2.1, page 36)</a>
    public Equity equity(double error, double confidence) {
        if (error <= 0.0d || error >= 1.0d) {
            throw new IllegalArgumentException();
        } else if (confidence <= 0.0d || confidence >= 1.0d) {
            throw new IllegalArgumentException();
        }

        // ceil(2 * log(2 / (1 - confidence)) / pow(error * 2, 2))
        BigInteger trials = BigDecimal.TWO.multiply(
            BigDecimal.valueOf(log(2 / (1 - confidence)))
        ).divide(
            BigDecimal.valueOf(error).multiply(BigDecimal.TWO).pow(2),
            DECIMAL128
        ).setScale(0, CEILING).toBigIntegerExact();

        try {
            return limit(trials.longValueExact()).collect(
                () -> new Equity(players, error, confidence),
                Equity::accumulate,
                Equity::combine
            );
        } catch (ArithmeticException cause) {
            throw new IllegalArgumentException(
                """
                number of trials=%,d needed for error=%s \
                and confidence=%s does not fit in a long""".formatted(
                    trials,
                    error,
                    confidence
                ),
                cause
            );
        }
    }

    /// Compute a default confidence interval for the hero's estimated equity.
    ///
    /// The magnitude of the difference between the true equity and the
    /// estimate is less than 0.001 with confidence level 0.999999.
    ///
    /// @see #equity(double, double)
    public Equity equity() {
        return equity(DEFAULT_ERROR, DEFAULT_CONFIDENCE);
    }

    /// A lazy, parallel stream of a given number of `trials`.
    ///
    /// The outcome of a trial is represented by a nonnegative integer
    /// indicating the number of players that the player with the given
    /// [hole cards][#pocket(Card,Card)] split the pot with, including that
    /// player: 0 means that player lost, 1 means that player won, and n > 1
    /// means an n-way tie.
    public IntStream limit(long trials) {
        if (trials < 0) {
            throw new IllegalArgumentException(
                "trials=%,d must be positive".formatted(trials)
            );
        }
        boolean parallel = true;
        return StreamSupport.intStream(new Spliterator(trials), parallel);
    }

    /// Splittable Monte Carlo simulation.
    private final class Spliterator implements java.util.Spliterator.OfInt {
        private final Deck deck;

        private long trials;

        private Spliterator(Deck deck, long trials) {
            this.deck = deck;
            this.trials = trials;
        }

        Spliterator(long trials) {
            this(new Deck(rng, board::contains, pocket::contains), trials);
        }

        @Override
        public int characteristics() {
            return IMMUTABLE | NONNULL | SIZED | SUBSIZED;
        }

        @Override
        public long estimateSize() {
            return trials;
        }

        @Nullable
        @Override
        public Spliterator trySplit() {
            if (trials <= 1) {
                return null;
            } else {
                return new Spliterator(deck.split(), trials - (trials >>>= 1));
            }
        }

        @Override
        public boolean tryAdvance(IntConsumer consumer) {
            if (trials < 1) {
                return false;
            }
            trials--;
            deck.shuffle();
            var board = Monty.this.board;
            switch (board.size()) {
                case 0: board = board.add(deck.deal())
                                     .add(deck.deal())
                                     .add(deck.deal()); // flop
                case 3: board = board.add(deck.deal()); // turn
                case 4: board = board.add(deck.deal()); // river
                case 5: break;
            }
            int player = board.add(pocket.first()).add(pocket.second()).evaluate();
            int split = 1;
            for (int n = 1; n < players; n++) {
                int opponent = board.add(deck.deal()).add(deck.deal()).evaluate();
                switch (signum(player - opponent)) {
                    case  0: split++;   // tie
                    case  1: continue;  // win
                    case -1: split = 0; // loss
                }
                break;
            }
            consumer.accept(split);
            return true;
        }
    }

    /// Confidence interval for the estimated equity.
    ///
    /// True equity is the fraction of the pot won on average across every
    /// possible outcome.
    public static final class Equity {
        /// Greatest common divisor.
        private static long gcd(long a, long b) {
            return b == 0L ? a : gcd(b, a % b);
        }

        private static long[][] pots() {
            var pots = new long[24][];
            long lcm = 1L; // least common multiple
            for (int players = 2; players < pots.length; players++) {
                lcm *= players / gcd(lcm, players);
                var pot = pots[players] = new long[players + 1];
                for (int split = 1; split <= players; split++) {
                    assert lcm % split == 0;
                    pot[split] = lcm / split;
                }
            }
            return pots;
        }

        private static final long[][] pots = pots();

        private final long[] pot;

        private final double error;

        private final double confidence;

        private long winnings = 0L;

        private long trials = 0L;

        private Equity(int players, double error, double confidence) {
            this.pot = pots[players];
            this.error = error;
            this.confidence = confidence;
        }

        /// Record the result of a trial.
        private void accumulate(int split) {
            winnings = Math.addExact(winnings, pot[split]);
            trials++;
        }

        /// Combine results from two different threads.
        private void combine(Equity equity) {
            winnings = Math.addExact(winnings, equity.winnings);
            trials += equity.trials;
        }

        /// Least common multiple of all possible ways to split the pot.
        private long lcm() {
            return pot[1]; // rangeClosed(2, players).reduce((a, b) -> a * b / gcd(a, b))
        }

        /// The fraction of the pot won on average across every trial.
        public double estimate() {
            return (double) winnings / trials / lcm();
        }

        /// The ratio of estimated winnings to the cost of calling a raise.
        public double expectedValue(long pot, long raise) {
            if (raise < 1) {
                throw new IllegalArgumentException(
                    "raise = %,d (must be positive)".formatted(raise)
                );
            } else if (pot < raise) {
                throw new IllegalArgumentException(
                    "pot = %,d, raise = %,d (pot must be >= to raise)".formatted(pot, raise)
                );
            }
            return estimate() * (pot + raise) / raise;
        }

        /// Magnitude of the difference between true equity and the estimate.
        public double error() {
            return error;
        }

        /// Chance that the actual error is less than [error][#error()].
        public double confidence() {
            return confidence;
        }

        /// The number of sampled outcomes.
        public long trials() {
            return trials;
        }

        @Override
        public String toString() {
            return "Equity[estimate=%s, error=%s, confidence=%s, trials=%,d]".formatted(
                estimate(),
                error,
                confidence,
                trials
            );
        }
    }
}
