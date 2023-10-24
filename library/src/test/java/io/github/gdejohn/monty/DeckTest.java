package io.github.gdejohn.monty;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static io.github.gdejohn.monty.Deck.deck;
import static org.assertj.core.api.Assertions.assertThat;

class DeckTest {
    @SuppressWarnings("InfiniteLoopStatement")
    @Test
    void deal() {
        var deck = deck();
        var cards = new HashSet<>();
        try {
            while (true) {
                assertThat(cards.add(deck.deal())).isTrue();
            }
        } catch (IllegalStateException e) {
            assertThat(cards).hasSize(52);
        }
    }
}
