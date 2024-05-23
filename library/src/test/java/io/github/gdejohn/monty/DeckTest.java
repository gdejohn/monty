package io.github.gdejohn.monty;

import org.junit.jupiter.api.Test;

import static io.github.gdejohn.monty.Deck.deck;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeckTest {
    @Test
    void deal() {
        var deck = deck();
        long cards = 0;
        for (int n = 0; n < 52; n++) {
            assertThat(deck.empty()).isFalse();
            long card = 1L << deck.deal().ordinal();
            assertThat(cards & card).isEqualTo(0);
            cards |= card;
        }
        assertThat(deck.empty()).isTrue();
        assertThatThrownBy(deck::deal).isInstanceOf(IllegalArgumentException.class);
    }
}
