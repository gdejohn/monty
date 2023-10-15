package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Card.Cards;

public final class Board extends Cards {
    public static final Board PRE_FLOP = new Board();

    private Board(Card... cards) {
        super(cards);
    }

    public static Board flop(Card first, Card second, Card third) {
        return new Board(first, second, third);
    }

    public static Board turn(Card first, Card second, Card third, Card fourth) {
        return new Board(first, second, third, fourth);
    }

    public static Board river(Card first, Card second, Card third, Card fourth, Card fifth) {
        return new Board(first, second, third, fourth, fifth);
    }
}
