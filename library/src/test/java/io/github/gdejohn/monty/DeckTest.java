package io.github.gdejohn.monty;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static io.github.gdejohn.monty.Deck.deck;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeckTest {
    @Test
    void deal() {
        var deck = deck();
        var cards = Stream.generate(deck::deal).limit(52).toList();
        assertThat(cards).containsExactlyInAnyOrder(Card.every().toArray(Card[]::new));
        assertThatThrownBy(deck::deal).isInstanceOf(IllegalStateException.class);
    }
}
