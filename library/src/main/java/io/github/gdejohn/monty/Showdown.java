package io.github.gdejohn.monty;

public sealed interface Showdown {
    record Win() implements Showdown {}

    record Loss() implements Showdown {}

    record Tie(int split) implements Showdown {
        public Tie {
            if (split < 2 || split > 23) {
                throw new IllegalArgumentException();
            }
        }
    }

    Win WIN = new Win();

    Loss LOSS = new Loss();

    static Showdown showdown(int split) {
        return switch (split) {
            case 1 -> WIN;
            case 0 -> LOSS;
            default -> new Tie(split);
        };
    }
}
