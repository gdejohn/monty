package io.github.gdejohn.monty;

import io.github.gdejohn.monty.Deck.Generator.DelegatingGenerator;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Deck.Generator.FastBoundedGenerator.GENERATOR;

/// A lazy deck of [cards][Card] for interleaved shuffling and dealing.
public final class Deck {
    /// A splittable bounded random number generator.
    sealed interface Generator {
        /// A random nonnegative integer less than a given `bound`.
        ///
        /// @see RandomGenerator#nextInt(int)
        int next(int bound);

        /// A generator split off from this one to use in another thread.
        ///
        /// @see SplittableGenerator#split()
        Generator split();

        /// A generator defined in terms of a given [SplittableGenerator].
        record DelegatingGenerator(SplittableGenerator rng) implements Generator {
            @Override
            public int next(int bound) {
                return rng.nextInt(bound);
            }

            @Override
            public DelegatingGenerator split() {
                return new DelegatingGenerator(rng.split());
            }
        }

        /// Fast unbiased bounded pseudorandom number generation.
        record FastBoundedGenerator(SplittableGenerator rng) implements Generator {
            /// LXM pseudorandom number generator with 256 state bits.
            ///
            /// Plenty of entropy to sample all 52! possible permutations
            /// (approximately 2^226) of a deck of cards without bias.
            ///
            /// @see java.util.random L128X128MixRandom
            static final FastBoundedGenerator GENERATOR = new FastBoundedGenerator(
                SplittableGenerator.of("L128X128MixRandom")
            );

            @Override
            public int next(int bound) {
                long n = Integer.toUnsignedLong(rng.nextInt()) * bound;
                if ((n & 0xFFFF_FFFFL) < bound) {
                    long threshold = -bound % bound;
                    while ((n & 0xFFFF_FFFFL) < threshold) {
                        n = Integer.toUnsignedLong(rng.nextInt()) * bound;
                    }
                }
                return (int) (n >>> 32);
            }

            @Override
            public FastBoundedGenerator split() {
                return new FastBoundedGenerator(rng.split());
            }

        }
    }

    /// The source of random numbers used to shuffle this deck.
    private final Generator generator;

    /// The cards in this deck.
    private final Card[] cards;

    /// The number of live cards remaining.
    private int bound;

    /// Make a new new deck with the given `generator` and `cards`.
    private Deck(Generator generator, Stream<Card> cards) {
        this.generator = generator;
        this.cards = cards.toArray(Card[]::new);
        shuffle();
    }

    /// Create a new deck with the given `rng` and `cards`.
    private Deck(@Nullable SplittableGenerator rng, Stream<Card> cards) {
        this(rng != null ? new DelegatingGenerator(rng) : GENERATOR.split(), cards);
    }

    /// Create a new deck with the given `rng` and dead cards excluded.
    Deck(@Nullable SplittableGenerator rng, Predicate<Card> board, Predicate<Card> pocket) {
        this(rng, Card.all().filter(board.or(pocket).negate()));
    }

    /// Create a new deck with the given `rng` and no dead cards.
    public Deck(SplittableGenerator rng) {
        this(rng, Card.all());
    }

    /// Create a deck with a default source of randomness and no dead cards.
    public Deck() {
        this((SplittableGenerator) null, Card.all());
    }

    /// Split this deck for use across multiple threads.
    public Deck split() {
        return new Deck(generator.split(), Arrays.stream(cards));
    }

    /// Add all of the previously dealt cards back into this deck.
    public void shuffle() {
        bound = cards.length;
    }

    /// True if and only if there are no cards remaining.
    public boolean empty() {
        return bound == 0;
    }

    /// Choose a card uniformly at random from the remaining live cards.
    public Card deal() {
        if (empty()) {
            throw new IllegalStateException("empty deck");
        }
        int index = generator.next(bound--);
        Card card = cards[index];
        cards[index] = cards[bound];
        cards[bound] = card; // move the chosen card to mark it dead
        return card;
    }
}
