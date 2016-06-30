package edu.msudenver.gdejohn.poker;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.reverse;
import static com.google.common.collect.MultimapBuilder.enumKeys;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Sets.immutableEnumSet;
import static edu.msudenver.gdejohn.poker.Category.FLUSH;
import static edu.msudenver.gdejohn.poker.Category.FOUR_OF_A_KIND;
import static edu.msudenver.gdejohn.poker.Category.FULL_HOUSE;
import static edu.msudenver.gdejohn.poker.Category.HIGH_CARD;
import static edu.msudenver.gdejohn.poker.Category.ONE_PAIR;
import static edu.msudenver.gdejohn.poker.Category.STRAIGHT;
import static edu.msudenver.gdejohn.poker.Category.STRAIGHT_FLUSH;
import static edu.msudenver.gdejohn.poker.Category.THREE_OF_A_KIND;
import static edu.msudenver.gdejohn.poker.Category.TWO_PAIR;
import static edu.msudenver.gdejohn.poker.Rank.ACE;
import static edu.msudenver.gdejohn.poker.Rank.FIVE;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Set;

import com.google.common.base.VerifyException;
import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;

public class NaiveHand implements Comparable<NaiveHand> {
	private static final Comparator<Entry<Rank>> byCountThenRank =
		comparingInt(Entry<Rank>::getCount).thenComparing(Entry<Rank>::getElement);
	
	private static final Comparator<NaiveHand> byCategoryThenRanks =
		comparing(NaiveHand::getCategory).thenComparing(NaiveHand::getRanks, natural().lexicographical());
	
	public final Set<Card> hand;
	
	public final Category category;
	
	private final LinkedList<Rank> distinctRanks = new LinkedList<>();
	
	public NaiveHand(Set<Card> cards) {
		hand = immutableEnumSet(cards);
		checkArgument(hand.size() == 5);
		Set<Suit> suits = EnumSet.noneOf(Suit.class);
		Multiset<Rank> ranks = EnumMultiset.create(Rank.class);
		for (Card card : hand) {
			suits.add(card.suit);
			ranks.add(card.rank);
		}
		ranks.entrySet()
			.stream()
			.sorted(byCountThenRank)
			.map(Entry::getElement)
			.forEach(distinctRanks::addFirst);
		Rank first = distinctRanks.getFirst();
		switch (distinctRanks.size()) {
			case 5: {
				boolean flush = suits.size() == 1;
				if (first.ordinal() - distinctRanks.getLast().ordinal() == 4) {
					category = flush ? STRAIGHT_FLUSH : STRAIGHT;
				}
				else if (first == ACE && distinctRanks.get(1) == FIVE) {
					distinctRanks.addLast(distinctRanks.removeFirst()); // ace plays low, move to end
					category = flush ? STRAIGHT_FLUSH : STRAIGHT;
				}
				else {
					category = flush ? FLUSH : HIGH_CARD;
				}
				return;
			}
			case 4: {
				category = ONE_PAIR;
				return;
			}
			case 3: {
				category = ranks.count(first) == 2 ? TWO_PAIR : THREE_OF_A_KIND;
				return;
			}
			case 2: {
				category = ranks.count(first) == 3 ? FULL_HOUSE : FOUR_OF_A_KIND;
				return;
			}
			default: {
				throw new VerifyException("Hot snow falling up");
			}
		}
	}
	
	public final Set<Card> getCards() {
		return hand;
	}
	
	public final Category getCategory() {
		return category;
	}
	
	private final LinkedList<Rank> getRanks() {
		return distinctRanks;
	}
	
	@Override
	public final boolean equals(Object that) {
		return that instanceof NaiveHand && this.hand.equals(((NaiveHand) that).hand);
	}
	
	@Override
	public final int hashCode() {
		return hand.hashCode();
	}
	
	@Override
	public final String toString() {
		Multimap<Rank, Card> cards = enumKeys(Rank.class).linkedListValues().build();
		for (Card card : hand) {
			cards.put(card.rank, card);
		}
		Deque<Card> hand = new LinkedList<>();
		for (Rank rank : reverse(distinctRanks)) {
			for (Card card : cards.get(rank)) {
				hand.addFirst(card);
			}
		}
		return hand.toString();
	}
	
	@Override
	public final int compareTo(NaiveHand that) {
		return byCategoryThenRanks.compare(this, that);
	}
	
	public static void main1(String[] args) throws FileNotFoundException, IOException
	{
		/*try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream("lut.dat")))
		{
			out.writeObject(new short[com.google.common.math.IntMath.binomial(52, 5)]);
		}*/
		
		try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream("a")))
		{
			out.writeObject(new short[52]);
		}
	}
	
	public static void main2(String[] args) throws FileNotFoundException, IOException
	{
		short[][][][][] lut = new short[52][][][][];
		
		for (int a = 0; a < 48; a++)
		{
			lut[a] = new short[52][][][];
			
			for (int b = a + 1; b < 49; b++)
			{
				lut[a][b] = new short[52][][];
				
				for (int c = b + 1; c < 50; c++)
				{
					lut[a][b][c] = new short[52][];
					
					for (int d = c + 1; d < 51; d++)
					{
						lut[a][b][c][d] = new short[52];
						
						for (int e = d + 1; e < 52; e++)
						{
							lut[a][b][c][d][e] = LookupTables.rankOrdered(a, b, c, d, e);
						}
					}
				}
			}
		}
		
		try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream("lut.dat")))
		{
			out.writeObject(lut);
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException
	{
		short[][][][][][][] lut = new short[52][][][][][][];
		
		for (int a = 0; a < 46; a++)
		{
			lut[a] = new short[52][][][][][];
			
			for (int b = a + 1; b < 47; b++)
			{
				lut[a][b] = new short[52][][][][];
				
				for (int c = b + 1; c < 48; c++)
				{
					lut[a][b][c] = new short[52][][][];
					
					for (int d = c + 1; d < 49; d++)
					{
						lut[a][b][c][d] = new short[52][][];
						
						for (int e = d + 1; e < 50; e++)
						{
							lut[a][b][c][d][e] = new short[52][];
							
							for (int f = e + 1; f < 51; f++)
							{
								lut[a][b][c][d][e][f] = new short[52];
								
								for (int g = f + 1; g < 52; g++)
								{
									lut[a][b][c][d][e][f][g] = LookupTables.rankOrdered(a, b, c, d, e, f, g);
									
									// enumerate permutations
								}
							}
						}
					}
				}
			}
		}
		
		try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream("lut.dat")))
		{
			out.writeObject(lut);
		}/*
	}
	
	public static void main(String[] args)
	{
		int[][][][] lut = new int[5][][][];
		
		for (int i = 0; i < 3; i++)
		{
			lut[i] = new int[5][];
			
			for (int j = i + 1; j < 4; j++)
			{
				int[] 
						
				for (int k = j + 1; k < 5; k++)
				{
					
				}
			}
		}*/
	}
}