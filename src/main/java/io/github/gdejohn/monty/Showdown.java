package io.github.gdejohn.monty;

public sealed interface Showdown {
    public static record Win() implements Showdown {
        @Override
        public int split() {
            return 1;
        }
    }

    public static record Loss() implements Showdown {
        @Override
        public int split() {
            return 0;
        }
    }

    public static record Tie(int split) implements Showdown {
        public Tie {
            if (split < 2 || split > 23) {
                throw new IllegalArgumentException();
            }
        }
    }

    public static final Win WIN = new Win();

    public static final Loss LOSS = new Loss();

    int split();
}
