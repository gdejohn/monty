package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Deck.Generator;

import java.util.Spliterator;
import java.util.function.IntConsumer;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static io.github.gdejohn.monty.Deck.deck;
import static java.lang.Integer.signum;

public final class Monty {
    public static final class Builder {
        private final SplittableGenerator rng;

        private Builder(SplittableGenerator rng) {
            this.rng = rng;
        }

        public Builder.Board players(int players) {
            return new Board(players);
        }

        public Builder.Board headsUp() {
            return players(2);
        }

        public final class Board {
            private final int players;

            private Board(int players) {
                this.players = players;
            }

            public Board.Pocket preflop() {
                return new Pocket();
            }

            public Board.Pocket flop(Card first, Card second, Card third) {
                return new Pocket(first, second, third);
            }

            public Board.Pocket turn(Card first, Card second, Card third, Card fourth) {
                return new Pocket(first, second, third, fourth);
            }

            public Board.Pocket river(Card first, Card second, Card third, Card fourth, Card fifth) {
                return new Pocket(first, second, third, fourth, fifth);
            }

            public final class Pocket {
                private final io.github.gdejohn.monty.Board board;

                private Pocket(Card... cards) {
                    this.board = new io.github.gdejohn.monty.Board(cards);
                }

                public Monty pocket(Card first, Card second) {
                    var pocket = new io.github.gdejohn.monty.Pocket(first, second);
                    return new Monty(pocket, board, players, rng);
                }
            }
        }
    }

    private final Pocket pocket;

    private final Board board;

    private final int players;

    private final SplittableGenerator rng;

    public Monty(Pocket pocket, Board board, int players, SplittableGenerator rng) {
        if (players < 2 || players > 23) {
            throw new IllegalArgumentException(
                "players = %d (must be greater than 1 and less than 24)".formatted(players)
            );
        } else if (board.stream().anyMatch(card -> pocket.stream().anyMatch(card::equals))) {
            throw new IllegalArgumentException(
                "pocket %s and board %s must be disjoint".formatted(pocket, board)
            );
        }
        this.pocket = pocket;
        this.board = board;
        this.players = players;
        this.rng = rng;
    }

    public static Monty.Builder rng(SplittableGenerator rng) {
        return new Builder(rng);
    }

    static Monty.Builder rng(byte[] seed) {
        return rng(new Generator(seed));
    }

    public static Monty.Builder.Board players(int players) {
        return rng(new Generator()).players(players);
    }

    public static Monty.Builder.Board headsUp() {
        return players(2);
    }

    public Showdown trials(long trials) {
        return stream().limit(trials).collect(
            Showdown::new,
            Showdown::accumulate,
            Showdown::combine
        );
    }

    /**
     * A lazy, infinite, parallel stream of simulated game outcomes.
     * <p>
     * The outcome of a game is represented by a nonnegative integer indicating the number of
     * players that the player with the given hole cards splits the pot with, including that
     * player: 0 means the player that player lost, 1 means that player won, and n > 1 means an
     * n-way tie.
     */
    public IntStream stream() {
        var parallel = true;
        var spliterator = new Simulation();
        return StreamSupport.intStream(spliterator, parallel);
    }

    private final class Simulation implements Spliterator.OfInt {
        private final Deck deck;

        private long trials;

        private Simulation(Deck deck, long trials) {
            this.deck = deck;
            this.trials = trials;
        }

        Simulation() {
            this(deck(board, pocket, rng), Long.MAX_VALUE);
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
            if (trials < 2) {
                return null;
            }
            return new Simulation(
                deck.split(),
                trials - (trials >>>= 1)
            );
        }

        @Override
        public boolean tryAdvance(IntConsumer action) {
            if (trials < 1) {
                return false;
            }
            trials--;
            deck.shuffle();
            var hand = deck.deal(board);
            int value = pocket.evaluate(hand);
            int split = 1;
            for (int n = 1; n < players; n++) {
                switch (signum(value - deck.deal(hand).evaluate())) {
                    case +0: split++;
                    case +1: continue;
                    case -1: split = 0;
                }
                break;
            }
            action.accept(split);
            return true;
        }
    }

    public final class Showdown {
        private static final long[][] pots = pots();

        private final long[] pot;

        private long winnings;

        private long trials;

        private Showdown() {
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

        public double equity() {
            return (double) winnings / pot[1] / trials;
        }

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

        private static long[][] pots() {
            var pots = new long[24][];
            long lcm = 1; // least common multiple
            for (int players = 2; players < pots.length; players++) {
                long gcd = lcm; // greatest common divisor
                long divisor = players;
                while (divisor != 0) {
                    long remainder = gcd % divisor;
                    gcd = divisor;
                    divisor = remainder;
                }
                lcm *= players / gcd;
                pots[players] = new long[players + 1];
                for (int split = 1; split <= players; split++) {
                    pots[players][split] = lcm / split;
                }
            }
            return pots;
        }
    }
}
