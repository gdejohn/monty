package io.github.gdejohn.monty;

public final class Pocket {
    /** Hole cards. */
    private final Card[] cards;

    private Pocket(Card... cards) {
        this.cards = cards;
    }

    public static Pocket pocket(Card first, Card second) {
        if (first.ordinal() == second.ordinal()) {
            throw new IllegalArgumentException("same card");
        }
        return new Pocket(first, second);
    }

    /** Finish a given partial board evaluation with these hole cards. */
    int evaluate(Hand partial) {
        return partial.add(cards[0]).add(cards[1]).evaluate();
    }

    long cards() {
        long cards = 0;
        for (int index = 0; index < 2; index++) {
            cards |= 1L << this.cards[index].ordinal();
        }
        return cards;
    }
}
