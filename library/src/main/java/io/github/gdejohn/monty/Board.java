package io.github.gdejohn.monty;

import java.util.stream.Stream;

import static io.github.gdejohn.monty.Hand.hand;

public final class Board {
    final Hand partial;

    final int count;

    Board(Card... cards) {
        this.partial = hand(cards);
        this.count = cards.length;
    }

    public static Board preflop() {
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

    public Stream<Card> stream() {
        return partial.stream();
    }

    @Override
    public String toString() {
        return Card.toString(stream());
    }
}
