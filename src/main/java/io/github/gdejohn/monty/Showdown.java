package io.github.gdejohn.monty;

public sealed interface Showdown {
    public static record Win() implements Showdown {
        private static final Win WIN = new Win();

        @Override
        public int split() {
            return 1;
        }
    }

    public static record Loss() implements Showdown {
        private static final Loss LOSS = new Loss();

        @Override
        public int split() {
            return 0;
        }
    }

    public static record Tie(int split) implements Showdown {
        private static final Tie[] TIES;

        static {
            TIES = new Tie[24];
            for (var split = 2; split < 24; split++) {
                TIES[split] = new Tie(split);
            }
        }

        public Tie {
            if (split < 2 || split > 23) {
                throw new IllegalArgumentException();
            }
        }
    }

    public static Win win() {
        return Win.WIN;
    }

    public static Loss loss() {
        return Loss.LOSS;
    }

    public static Tie tie(int split) {
        return Tie.TIES[split];
    }

    int split();
}
