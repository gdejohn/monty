package io.github.gdejohn.monty;

public sealed interface Showdown {
    /**
     * Least common multiple of the integers from 1 to 23, inclusive.
     */
    public static final Win WIN = new Win();

    public static final Loss LOSS = new Loss();

    public record Win() implements Showdown {
        @Override
        public int split() {
            return 1;
        }
    }

    public record Loss() implements Showdown {
        @Override
        public int split() {
            return 0;
        }
    }

    public record Tie(int split) implements Showdown {
        public Tie {
            if (split < 2 || split > 23) {
                throw new IllegalArgumentException();
            }
        }
    }

    int split();

    static Tie nextSplit(Showdown showdown) {
        return new Tie(showdown.split() + 1);
    }
}
