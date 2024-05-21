package io.github.gdejohn.monty;

import java.util.Arrays;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Stream;

import static io.github.gdejohn.monty.Board.preflop;

public final class Deck {
    static final class Generator implements SplittableGenerator {
        private static final String DEFAULT_RNG = "L128X128MixRandom";

        private final SplittableGenerator rng;

        private Generator(SplittableGenerator rng) {
            this.rng = rng;
        }

        private Generator(String rng) {
            this(SplittableGenerator.of(rng));
        }

        Generator() {
            this(DEFAULT_RNG);
        }

        private Generator(String rng, byte[] seed) {
            this(RandomGeneratorFactory.<SplittableGenerator>of(rng).create(seed));
        }

        Generator(byte[] seed) {
            this(DEFAULT_RNG, seed);
        }

        @Override
        public SplittableGenerator split() {
            return new Generator(rng.split());
        }

        @Override
        public int nextInt(int bound) {
            var n = Integer.toUnsignedLong(rng.nextInt()) * bound;
            if ((n & 0xFFFF_FFFFL) < bound) {
                var threshold = (1L << 31) % bound;
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

    private Deck(Card[] cards, SplittableGenerator rng) {
        this.cards = cards;
        this.rng = rng;
        shuffle();
    }

    public static Deck deck(Board board, Pocket pocket, SplittableGenerator rng) {
        long dead = board.cards() | pocket.cards();
        var deck = dead ^ -1L >>> -52;
        var cards = new Card[Long.bitCount(deck)];
        for (var index = 0; index < cards.length; index++) {
            var card = deck & -deck;
            cards[index] = Card.unpack(card);
            deck ^= card;
        }
        return new Deck(cards, rng);
    }

    public static Deck deck(Pocket pocket, SplittableGenerator rng) {
        return deck(preflop(), pocket, rng);
    }

    public static Deck deck(Board board, Pocket pocket) {
        return deck(board, pocket, new Generator());
    }

    public static Deck deck(Pocket pocket) {
        return deck(preflop(), pocket);
    }

    public static Deck deck(SplittableGenerator rng) {
        return new Deck(Card.every().toArray(Card[]::new), rng);
    }

    public static Deck deck() {
        return deck(new Generator());
    }

    public Deck split() {
        return new Deck(Arrays.copyOf(cards, cards.length), rng.split());
    }

    public boolean empty() {
        return bound > 0;
    }

    public void shuffle() {
        bound = cards.length;
    }

    public Card deal() {
        if (bound < 1) {
            throw new IllegalStateException("no cards remaining");
        }
        int index = rng.nextInt(bound--);
        var card = cards[index];
        cards[index] = cards[bound];
        cards[bound] = card;
        return card;
    }

    /** Finish dealing the community cards and partially evaluate them. */
    Hand deal(Board board) {
        Hand partial = board.partial;
        for (int n = board.count; n < 5; n++) {
            partial = partial.add(deal());
        }
        return partial;
    }

    /** Determine an opponents hand. */
    Hand deal(Hand partial) {
        return partial.add(deal()).add(deal());
    }
}
