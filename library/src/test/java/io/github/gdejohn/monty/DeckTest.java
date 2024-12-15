package io.github.gdejohn.monty;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DeckTest {
    @Test
    void deal() {
        var deck = new Deck();
        var cards = new HashSet<Card>();
        for (int n = 0; n < 52; n++) {
            assertThat(deck.empty()).isFalse();
            var card = deck.deal();
            assertThat(cards.add(card)).isTrue();
        }
        assertThat(Card.all().allMatch(cards::contains)).isTrue();
        assertThat(deck.empty()).isTrue();
        assertThatThrownBy(deck::deal).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lehmerGenerator() {
        int multiplier = 0x93d765dd;
        int state = multiplier;
        int period = 0;
        var counts = new int[32];
        do {
            counts[state >>> -5]++;
            state *= multiplier;
            period++;
        } while (state != multiplier);
        assertThat(period).isEqualTo(1 << 30);
        assertThat(Arrays.stream(counts)).allMatch(count -> count == 1 << 25);
    }
}
