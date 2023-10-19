package io.github.gdejohn.monty;

import static io.github.gdejohn.monty.Board.PRE_FLOP;
import static io.github.gdejohn.monty.Card.cards;

import java.util.Arrays;
import java.util.SplittableRandom;

public class Deck {
    private static final long DECK = -1L >>> -52;
    
    private final SplittableRandom rng;
    
    private final Card[] cards;

    private int bound;

    private Deck(Card[] cards, SplittableRandom rng) {
        this.cards = cards;
        this.rng = rng;
        shuffle();
    }

    static Deck deck(Board board, Pocket pocket, SplittableRandom rng) {
        var deck = DECK ^ board.pack() ^ pocket.pack();
        var cards = new Card[Long.bitCount(deck)];
        for (var index = 0; index < cards.length; index++) {
            var card = deck & -deck;
            cards[index] = Card.unpack(card);
            deck ^= card;
        }
        return new Deck(cards, rng);
    }

    public static Deck deck(Board board, Pocket pocket) {
        return deck(board, pocket, new SplittableRandom());
    }

    public static Deck deck(Pocket pocket) {
        return deck(PRE_FLOP, pocket);
    }

    public static Deck deck() {
        return new Deck(cards().toArray(Card[]::new), new SplittableRandom());
    }

    public void shuffle() {
        bound = cards.length;
    }

    public Card deal() {
        var index = rng.nextInt(bound--);
        var card = cards[index];
        cards[index] = cards[bound];
        cards[bound] = card;
        return card;
    }

    Hand deal(Hand partial, int n) {
        while (n-- > 0) {
            var card = deal();
            partial = partial.deal(card);
        }
        return partial;
    }

    public int size() {
        return cards.length;
    }

    public Deck split() {
        return new Deck(Arrays.copyOf(cards, cards.length), rng.split());
    }
}
