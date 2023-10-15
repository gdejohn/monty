package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Cards;

public final class Pocket extends Cards {
    private Pocket(Card... cards) {
        super(cards);
    }

    public static Pocket pocket(Card first, Card second) {
        return new Pocket(first, second);
    }
}
