package io.github.gdejohn.monty;

import java.util.Arrays;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Stream;

public final class Deck {
    static final class Generator implements SplittableGenerator {
        private static final String DEFAULT_RNG = "L128X128MixRandom";

        private final SplittableGenerator rng;

        private Generator(SplittableGenerator rng) {
            this.rng = rng;
        }

        Generator(byte[] seed) {
            this(RandomGeneratorFactory.<SplittableGenerator>of(DEFAULT_RNG).create(seed));
        }

        Generator() {
            this(SplittableGenerator.of(DEFAULT_RNG));
        }

        @Override
        public SplittableGenerator split() {
            return new Generator(rng.split());
        }

        @Override
        public int nextInt(int bound) {
            if (bound < 1) {
                throw new IllegalArgumentException("bound must be positive");
            }
            long n = Integer.toUnsignedLong(rng.nextInt()) * bound;
            if ((n & 0xFFFF_FFFFL) < bound) {
                long threshold = (1L << 31) % bound;
                while ((n & 0xFFFF_FFFFL) < threshold) {
                    n = Integer.toUnsignedLong(rng.nextInt()) * bound;
                }
            }
            return (int) (n >>> 32);
        }

        @Override
        public long nextLong() {
            throw new AssertionError();
        }

        @Override
        public SplittableGenerator split(SplittableGenerator source) {
            throw new AssertionError();
        }

        @Override
        public Stream<SplittableGenerator> splits(long streamSize) {
            throw new AssertionError();
        }

        @Override
        public Stream<SplittableGenerator> splits(SplittableGenerator source) {
            throw new AssertionError();
        }

        @Override
        public Stream<SplittableGenerator> splits(long streamSize, SplittableGenerator source) {
            throw new AssertionError();
        }
    }

    private final SplittableGenerator rng;

    private final Card[] cards;

    private int bound;

    Deck(SplittableGenerator rng, Stream<Card> cards) {
        this.cards = cards.toArray(Card[]::new);
        this.rng = rng;
        shuffle();
    }

    public Deck(SplittableGenerator rng) {
        this(rng, Card.all());
    }

    public Deck() {
        this(new Generator());
    }

    public Deck split() {
        return new Deck(rng.split(), Arrays.stream(cards));
    }

    public void shuffle() {
        bound = cards.length;
    }

    public boolean empty() {
        return bound == 0;
    }

    public Card deal() {
        int index = rng.nextInt(bound--);
        var card = cards[index];
        cards[index] = cards[bound];
        cards[bound] = card;
        return card;
    }
}
