package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Cards;

public final class Pocket extends Cards {
    private Pocket(Card... cards) {
        super(cards);
    }

    int evaluate(Hand hand) {
        for (var index = 0; index < cards.length; index++) {
            hand = hand.deal(cards[index]);
        }
        return hand.evaluate();
    }

    public static Pocket pocket(Card first, Card second) {
        return new Pocket(first, second);
    }
}
