package io.github.gdejohn.monty;

import static io.github.gdejohn.monty.Hand.hand;

public final class Board {
    final Hand partial;

    final int count;

    private Board(Card... cards) {
        this.partial = hand(cards);
        this.count = cards.length;
    }

    static Board preflop() {
        return new Board();
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

    long cards() {
        return partial.cards();
    }

    @Override
    public String toString() {
        return Card.toString(Card.stream(cards()));
    }
}
