package io.github.gdejohn.monty;

import java.util.Arrays;
import java.util.stream.Stream;

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

    /** Complete a given partial board evaluation using these hole cards. */
    int evaluate(Hand partial) {
        return partial.add(cards[0]).add(cards[1]).evaluate();
    }

    public Stream<Card> stream() {
        return Arrays.stream(cards);
    }

    @Override
    public String toString() {
        return Card.toString(stream());
    }
}
