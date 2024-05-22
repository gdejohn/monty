package io.github.gdejohn.monty;

import java.util.Arrays;
import java.util.stream.Stream;

public final class Pocket {
    /** Hole cards. */
    private final Card[] cards;

    private Pocket(Card[] cards) {
        if (cards[0].ordinal() == cards[1].ordinal()) {
            throw new IllegalArgumentException("same card");
        }
        this.cards = cards;
    }

    public Pocket(Card first, Card second) {
        this(new Card[] {first, second});
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
