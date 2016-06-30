package edu.msudenver.gdejohn.poker;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Sets.immutableEnumSet;
import static edu.msudenver.gdejohn.poker.Card.ACE_CLUBS;
import static edu.msudenver.gdejohn.poker.Card.ACE_HEARTS;
import static edu.msudenver.gdejohn.poker.Card.FIVE_CLUBS;
import static edu.msudenver.gdejohn.poker.Card.FOUR_CLUBS;
import static edu.msudenver.gdejohn.poker.Card.SEVEN_CLUBS;
import static edu.msudenver.gdejohn.poker.Card.SEVEN_HEARTS;
import static edu.msudenver.gdejohn.poker.Card.THREE_CLUBS;
import static edu.msudenver.gdejohn.poker.Card.THREE_HEARTS;
import static edu.msudenver.gdejohn.poker.Card.TWO_CLUBS;
import static edu.msudenver.gdejohn.poker.Card.TWO_DIAMONDS;
import static edu.msudenver.gdejohn.poker.Card.TWO_HEARTS;
import static edu.msudenver.gdejohn.poker.Card.TWO_SPADES;
import static edu.msudenver.gdejohn.poker.Card.getCard;
import static edu.msudenver.gdejohn.poker.Category.FLUSH;
import static edu.msudenver.gdejohn.poker.Category.FOUR_OF_A_KIND;
import static edu.msudenver.gdejohn.poker.Category.FULL_HOUSE;
import static edu.msudenver.gdejohn.poker.Category.HIGH_CARD;
import static edu.msudenver.gdejohn.poker.Category.ONE_PAIR;
import static edu.msudenver.gdejohn.poker.Category.STRAIGHT;
import static edu.msudenver.gdejohn.poker.Category.STRAIGHT_FLUSH;
import static edu.msudenver.gdejohn.poker.Category.THREE_OF_A_KIND;
import static edu.msudenver.gdejohn.poker.Category.TWO_PAIR;
import static edu.msudenver.gdejohn.poker.Combinations.choose;
import static edu.msudenver.gdejohn.poker.LookupTables.choose;
import static edu.msudenver.gdejohn.poker.LookupTables.index;
import static edu.msudenver.gdejohn.poker.LookupTables.rank;
import static java.util.EnumSet.allOf;
import static java.util.EnumSet.noneOf;
import static java.util.EnumSet.of;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableSortedMap;

/**
 * A poker hand comprising five distinct cards.
 * 
 * Note that this class defines a strict weak natural ordering that is
 * inconsistent with {@link #equals}, since two distinct hands with the same
 * {@link #rank} are considered equivalent by {@link #compareTo}.
 * 
 * This class' implementations of {@link #equals} and {@link #hashCode} do not
 * follow the contract specified by {@link Set}. The immutable backing set
 * {@link #cards} does follow that contract and is exposed as a public final
 * field for flexibility.
 * 
 * Instances of this class are immutable and thread-safe.
 *
 * @author Griffin DeJohn
 * 
 * @see Card
 */
public class Hand extends ForwardingSet<Card> implements Set<Card>, Comparable<Hand>
{
	/**
	 * The five distinct cards comprising this hand.
	 * 
	 * This is the immutable set backing this hand, implementing {@link
	 * #equals} and {@link #hashCode} in accordance with the contract specified
	 * by {@link Set}.
	 * 
	 * @see Card
	 */
	public final Set<Card> cards;
	
	/**
	 * This hand's corresponding number in the combinatorial number system.
	 * 
	 * There are 2,598,960 distinct 5-combinations of cards (52 choose 5). This
	 * index is the unique integer in the interval [0, 2598959] corresponding
	 * to this hand's combination of {@link cards}.
	 * 
	 * @see Card
	 */
	public final int index;
	
	/**
	 * Imposes a strict weak order on the set of all hands.
	 * 
	 * Given two hands, the one with the greater rank beats the other. If they
	 * have equal ranks, then they are tied.
	 * 
	 * Specifically, this is the zero-based index of this hand's equivalence
	 * class in the set of all 7,462 equivalence classes ordered such that
	 * given equivalence classes <i>a</i> and <i>b</i>, if <i>a</i> > <i>b</i>,
	 * then the hands in <i>a</i> beat the hands in <i>b</i>.
	 */
	public final short rank;
	
	/**
	 * Constructs a poker hand from a given set of cards.
	 * 
	 * @param cards The given set of cards
	 * 
	 * @throws IllegalArgumentException If {@code cards.size() != 5}
	 */
	public Hand(Set<Card> cards)
	{
		this.cards = immutableEnumSet(cards);
		
		checkArgument(this.cards.size() == 5);
		
		this.index = index(this.cards);
		
		this.rank = rank(index);
	}
	
	/**
	 * Constructs a poker hand from the five given cards.
	 * 
	 * @throws NullPointerException If any of the given cards are null
	 */
	public Hand(Card first, Card second, Card third, Card fourth, Card fifth)
	{
		this(of(first, second, third, fourth, fifth));
	}
	
	/**
	 * Constructs the hand corresponding to the given index.
	 * 
	 * There are 2,598,960 possible hands (52 choose 5). The given index must
	 * be an integer in the interval [0, 2598959], with which a unique
	 * combination of five cards is associated.
	 * 
	 * @throws IndexOutOfBoundsException If index < 0 or index > 2598959
	 */
	public Hand(int index)
	{
		checkElementIndex(index, 2_598_960);
		
		this.index = index;
		
		this.rank = rank(index);
		
		Set<Card> cards = noneOf(Card.class);
		
		int n = 51;
		
		int k = 5;
		
		while (k > 0)
		{
			int binomial = choose(n, k);
			
			if (binomial <= index)
			{
				cards.add(getCard(n));
				
				index -= binomial;
				
				k--;
			}
			
			n--;
		}
		
		this.cards = immutableEnumSet(cards);
	}
	
	/**
	 * Constructs a random hand.
	 */
	public Hand()
	{
		this(ThreadLocalRandom.current().nextInt(2_598_960));
	}
	
	/**
	 * Tests this hand against the given object for equality.
	 * 
	 * They are equal if the given object is also a {@link Hand} and has the
	 * same {@link #index}, which implies the same {@link #cards}.
	 */
	@Override
	public final boolean equals(Object that)
	{
		return that instanceof Hand && this.index == ((Hand) that).index;
	}
	
	/**
	 * Returns this hand's {@link #index}.
	 * 
	 * This uses the combinatorial number system to implement a minimal perfect
	 * hash function for every possible hand. The returned index is the unique
	 * natural number in the combinatorial number system corresponding to this
	 * hand's combination of cards.
	 */
	@Override
	public final int hashCode()
	{
		return index;
	}
	
	/**
	 * Returns a string representation of this hand.
	 * 
	 * The string lists the {@link #cards} in this hand ordered by their
	 * significance to this hand's {@link #rank}, decreasing from left to
	 * right.
	 */
	@Override
	public final String toString()
	{
		return new NaiveHand(cards).toString();
	}
	
	/**
	 * Determines the winner between this hand and a given hand.
	 * 
	 * Whichever has the higher {@link #rank} wins.
	 * 
	 * @param that The given hand against which this hand is compared.
	 */
	@Override
	public final int compareTo(Hand that)
	{
		return this.rank - that.rank;
	}
	
	/**
	 * Returns the delegate to which {@link Set} method calls are forwarded.
	 */
	@Override
	protected final Set<Card> delegate()
	{
		return cards;
	}
	
	public final Category categorize()
	{
		return categories.floorEntry(this).getValue();
	}
	
	private static final NavigableMap<Hand, Category> categories =
		ImmutableSortedMap.<Hand, Category>naturalOrder()
			.put(new Hand(SEVEN_HEARTS, FIVE_CLUBS, FOUR_CLUBS, THREE_CLUBS, TWO_CLUBS), HIGH_CARD)
			.put(new Hand(TWO_HEARTS, TWO_CLUBS, FIVE_CLUBS, FOUR_CLUBS, THREE_CLUBS), ONE_PAIR)
			.put(new Hand(THREE_HEARTS, THREE_CLUBS, TWO_HEARTS, TWO_CLUBS, FOUR_CLUBS), TWO_PAIR)
			.put(new Hand(TWO_SPADES, TWO_HEARTS, TWO_CLUBS, FOUR_CLUBS, THREE_CLUBS), THREE_OF_A_KIND)
			.put(new Hand(FIVE_CLUBS, FOUR_CLUBS, THREE_CLUBS, TWO_CLUBS, ACE_HEARTS), STRAIGHT)
			.put(new Hand(SEVEN_CLUBS, FIVE_CLUBS, FOUR_CLUBS, THREE_CLUBS, TWO_CLUBS), FLUSH)
			.put(new Hand(TWO_SPADES, TWO_HEARTS, TWO_CLUBS, THREE_HEARTS, THREE_CLUBS), FULL_HOUSE)
			.put(new Hand(TWO_SPADES, TWO_HEARTS, TWO_CLUBS, TWO_DIAMONDS, THREE_CLUBS), FOUR_OF_A_KIND)
			.put(new Hand(FIVE_CLUBS, FOUR_CLUBS, THREE_CLUBS, TWO_CLUBS, ACE_CLUBS), STRAIGHT_FLUSH)
			.build();
	
	/**
	 * Lazily enumerates every poker hand possible with the given cards.
	 */
	public static Iterable<Hand> possibleHands(Set<Card> cards)
	{
		return transform(choose(Card.class, cards, 5), Hand::new);
	}
	
	/**
	 * Lazily enumerates all 2,598,960 distinct poker hands.
	 * 
	 * Ordered lexicographically according to {@link Card}.
	 */
	public static final Iterable<Hand> everyHand = possibleHands(allOf(Card.class));
	
	public static void main(String[] args) throws IOException
	{
		categories.entrySet().forEach(System.out::println);
		try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream("foo"))) {
			out.writeObject(new int[52]);
		}
	}
}