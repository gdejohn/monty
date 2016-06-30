package edu.msudenver.gdejohn.poker;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Ordering.natural;
import static com.google.common.collect.Sets.newHashSet;
import static com.google.common.collect.Sets.newHashSetWithExpectedSize;
import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.readLines;
import static com.google.common.math.IntMath.binomial;
import static edu.msudenver.gdejohn.poker.Card.ACE_SPADES;
import static edu.msudenver.gdejohn.poker.Card.KING_SPADES;
import static edu.msudenver.gdejohn.poker.Card.NINE_HEARTS;
import static edu.msudenver.gdejohn.poker.Card.SIX_DIAMONDS;
import static edu.msudenver.gdejohn.poker.Card.SIX_SPADES;
import static edu.msudenver.gdejohn.poker.Card.TEN_CLUBS;
import static edu.msudenver.gdejohn.poker.Card.TEN_SPADES;
import static edu.msudenver.gdejohn.poker.Card.TWO_DIAMONDS;
import static edu.msudenver.gdejohn.poker.Card.getCard;
import static edu.msudenver.gdejohn.poker.Combinations.choose;
import static edu.msudenver.gdejohn.poker.Hand.everyHand;
import static edu.msudenver.gdejohn.poker.LookupTables.choose;
import static edu.msudenver.gdejohn.poker.LookupTables.rank;
import static edu.msudenver.gdejohn.poker.LookupTables.rankSeven;
import static edu.msudenver.gdejohn.poker.Player.equity;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.Short.parseShort;
import static java.nio.charset.Charset.defaultCharset;
import static java.util.EnumSet.noneOf;
import static java.util.EnumSet.of;
import static java.util.EnumSet.range;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

public class HandTest {
	private static final short quotientSet = 7_462; // number of equivalence classes for 5-combinations
	
	private static final int totalHands = binomial(52, 5);
	
	@BeforeClass
	public static void initializeLookupTables() {
		rank(0);
	}
	
	@Test
	public void testEnumeration()
	{
		Set<Set<Card>> combinations = newHashSetWithExpectedSize(totalHands);
		
		Set<Hand> hands = newHashSetWithExpectedSize(totalHands);
		
		for (Hand hand : everyHand)
		{
			assertTrue(combinations.add(newHashSet(hand)));
			
			assertTrue(hands.add(hand));
		}
		
		assertEquals(totalHands, combinations.size());
		
		assertEquals(totalHands, hands.size());
	}
	
	@Test
	public void testCombinadics()
	{
		NavigableMap<Integer, Hand> hands = new TreeMap<>();
		
		for (Hand hand : everyHand)
		{
			assertNull(hands.put(hand.index, hand));
		}
		
		assertEquals(totalHands, hands.size());
		
		assertEquals(0, hands.firstKey().intValue());
		
		assertEquals(totalHands - 1, hands.lastKey().intValue());
		
		assertEquals(new Hand(range(TWO_DIAMONDS, SIX_DIAMONDS)), hands.firstEntry().getValue());
		
		assertEquals(new Hand(range(TEN_SPADES, ACE_SPADES)), hands.lastEntry().getValue());
	}
	
	@Test
	public void testIndex()
	{
		Set<Hand> hands = newHashSetWithExpectedSize(totalHands);
		
		for (int index = 0; index < totalHands; index++)
		{
			Hand hand = new Hand(index);
			
			assertEquals(index, hand.index);
			
			assertEquals(rank(index), hand.rank);
			
			assertTrue(hands.add(hand));
		}
		
		for (Hand hand : everyHand)
		{
			assertTrue(hands.remove(hand));
		}
		
		assertTrue(hands.isEmpty());
	}
	
	@Test
	public void testCategories()
	{
		Multiset<Category> hands = EnumMultiset.create(Category.class);
		
		for (Hand hand : everyHand)
		{
			Category category = hand.categorize();
			
			assertEquals(new NaiveHand(hand).category, category);
			
			hands.add(category);
		}
		
		assertEquals(totalHands, hands.size());
		
		for (Category category : Category.values())
		{
			assertEquals(category.hands, hands.setCount(category, 0));
		}
		
		assertTrue(hands.isEmpty());
	}
	
	// @Ignore
	@Test
	public void testEquity()
	{
		assertEquals(0.135, equity(of(NINE_HEARTS, KING_SPADES), of(TEN_CLUBS, SIX_SPADES, TEN_SPADES), 5), 0.001);
	}
	
	@Test
	public void testFiveCardEvaluation() throws IOException
	{
		Multimap<Short, Hand> hands = LinkedListMultimap.create(quotientSet);
		SortedSetMultimap<Short, NaiveHand> representatives = TreeMultimap.create();
		for (Set<Card> cards : choose(Card.class, 5)) {
			Hand hand = new Hand(cards);
			hands.put(hand.rank, hand);
			representatives.put(hand.rank, new NaiveHand(cards));
		}
		assertEquals(totalHands, hands.size());
		assertEquals(quotientSet, hands.keySet().size());
		assertEquals(quotientSet, representatives.size());
		assertEquals(quotientSet, representatives.keySet().size());
		assertTrue(natural().isStrictlyOrdered(representatives.values()));
		Map<String, Rank> ranks = newHashMap();
		for (Rank rank : Rank.values()) {
			ranks.put(rank.toString(), rank);
		}
		for (String line : readLines(getResource("equivalence_classes.txt"), defaultCharset())) {
			String[] tokens = line.split(" ");
			Multiset<Rank> expectedRanks = EnumMultiset.create(Rank.class);
			for (String rank : tokens[1].split("-")) {
				expectedRanks.add(ranks.get(rank));
			}
			short rank = parseShort(tokens[0]);
			boolean flush = parseBoolean(tokens[2]);
			int cardinality = parseInt(tokens[3]);
			Collection<Hand> equivalenceClass = hands.removeAll(rank);
			assertEquals(cardinality, equivalenceClass.size());
			for (Hand hand : equivalenceClass) {
				Multiset<Rank> actualRanks = EnumMultiset.create(Rank.class);
				Set<Suit> suits = noneOf(Suit.class);
				for (Card card : hand) {
					actualRanks.add(card.rank);
					suits.add(card.suit);
				}
				assertEquals(expectedRanks, actualRanks);
				assertTrue(flush == (suits.size() == 1));
			}
		}
		assertTrue(hands.isEmpty());
	}
	
	@Ignore
	@Test
	public void testSevenCardEvaluation()
	{
		for (int[] i = new int[7]; i[0] < 46; i[0]++)
		{
			for (i[1] = i[0] + 1; i[1] < 47; i[1]++)
			{
				for (i[2] = i[1] + 1; i[2] < 48; i[2]++)
				{
					for (i[3] = i[2] + 1; i[3] < 49; i[3]++)
					{
						for (i[4] = i[3] + 1; i[4] < 50; i[4]++)
						{
							for (i[5] = i[4] + 1; i[5] < 51; i[5]++)
							{
								for (i[6] = i[5] + 1; i[6] < 52; i[6]++)
								{
									int rank = 0;
									
									for (int a = 0; a < 3; a++)
									{
										for (int b = a + 1; b < 4; b++)
										{
											for (int c = b + 1; c < 5; c++)
											{
												for (int d = c + 1; d < 6; d++)
												{
													for (int e = d + 1; e < 7; e++)
													{
														int index = 0;
														
														index += choose(i[a], 1);
														
														index += choose(i[b], 2);
														
														index += choose(i[c], 3);
														
														index += choose(i[d], 4);
														
														index += choose(i[e], 5);
														
														rank = max(rank, rank(index));
													}
												}
											}
										}
									}
									
									int index = 0;
									
									int k = 1;
									
									for (int n : i)
									{
										index += choose(n, k++);
									}
									
									assertEquals(rank, rankSeven(index));
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Ignore
	@Test
	public void testSevenCardEquivalenceClasses() throws IOException
	{
		short quotientSet = 4_824; // smaller for 7-combinations than for 5-combinations, since some hands are impossible
		
		Multiset<Short> ranks = HashMultiset.create(quotientSet);
		
		for (Set<Card> cards : choose(Card.class, 7))
		{
			ranks.add(rankSeven(cards));
		}
		
		assertEquals(quotientSet, ranks.elementSet().size());
		
		assertEquals(binomial(52, 7), ranks.size());
		
		for (String line : readLines(getResource("equivalence_classes.txt"), defaultCharset()))
		{
			String[] tokens = line.split(" ");
			
			short rank = parseShort(tokens[0]);
			
			int cardinality = parseInt(tokens[4]);
			
			assertEquals(cardinality, ranks.setCount(rank, 0));
		}
		
		assertTrue(ranks.isEmpty());
	}
	
	@Test
	public void timeFiveCardSetEvaluation()
	{
		Set<Card> cards = noneOf(Card.class);
		
		for (int c1 = 0; c1 < 46; c1++)
		{
			for (int c2 = c1 + 1; c2 < 47; c2++)
			{
				for (int c3 = c2 + 1; c3 < 48; c3++)
				{
					for (int c4 = c3 + 1; c4 < 49; c4++)
					{
						for (int c5 = c4 + 1; c5 < 50; c5++)
						{
							cards.add(getCard(c1));
							
							cards.add(getCard(c2));
							
							cards.add(getCard(c3));
							
							cards.add(getCard(c4));
							
							cards.add(getCard(c5));
							
							rank(cards);
							
							cards.clear();
						}
					}
				}
			}
		}
	}
	
	@Test
	public void timeSevenCardSetEvaluation()
	{
		Set<Card> cards = noneOf(Card.class);
		
		for (int c1 = 0; c1 < 46; c1++)
		{
			for (int c2 = c1 + 1; c2 < 47; c2++)
			{
				for (int c3 = c2 + 1; c3 < 48; c3++)
				{
					for (int c4 = c3 + 1; c4 < 49; c4++)
					{
						for (int c5 = c4 + 1; c5 < 50; c5++)
						{
							for (int c6 = c5 + 1; c6 < 51; c6++)
							{
								for (int c7 = c6 + 1; c7 < 52; c7++)
								{
									cards.add(getCard(c1));
									
									cards.add(getCard(c2));
									
									cards.add(getCard(c3));
									
									cards.add(getCard(c4));
									
									cards.add(getCard(c5));
									
									cards.add(getCard(c6));
									
									cards.add(getCard(c7));
									
									rankSeven(cards);
									
									cards.clear();
								}
							}
						}
					}
				}
			}
		}
	}
	
	@Test
	public void timeOrderedSevenCardEvaluation() {
		long sum = 0;
		for (int c1 = 0; c1 < 46; c1++)
			for (int c2 = c1 + 1; c2 < 47; c2++)
				for (int c3 = c2 + 1; c3 < 48; c3++)
					for (int c4 = c3 + 1; c4 < 49; c4++)
						for (int c5 = c4 + 1; c5 < 50; c5++)
							for (int c6 = c5 + 1; c6 < 51; c6++)
								for (int c7 = c6 + 1; c7 < 52; c7++)
									sum += rank(c1, c2, c3, c4, c5, c6, c7);
		assertNotEquals(sum, 0);
	}
	
	@Test
	public void timeRandomHandEvaluation()
	{
		int[] cards = new int[52];
		
		for (int index = 0; index < 52; index++)
		{
			cards[index] = index;
		}
		
		Random random = ThreadLocalRandom.current();
		
		for (int trials = 100_000_000; trials > 0; trials--)
		{
			int index = random.nextInt(52);
			
			int c1 = cards[index];
			
			cards[index] = cards[51];
			
			cards[51] = c1;
			
			index = random.nextInt(51);
			
			int c2 = cards[index];
			
			cards[index] = cards[50];
			
			cards[50] = c2;
			
			index = random.nextInt(50);
			
			int c3 = cards[index];
			
			cards[index] = cards[49];
			
			cards[49] = c3;
			
			index = random.nextInt(49);
			
			int c4 = cards[index];
			
			cards[index] = cards[48];
			
			cards[48] = c4;
			
			index = random.nextInt(48);
			
			int c5 = cards[index];
			
			cards[index] = cards[47];
			
			cards[47] = c5;
			
			index = random.nextInt(47);
			
			int c6 = cards[index];
			
			cards[index] = cards[46];
			
			cards[46] = c6;
			
			index = random.nextInt(46);
			
			int c7 = cards[index];
			
			cards[index] = cards[45];
			
			cards[45] = c7;
			
			rank(c1, c2, c3, c4, c5, c6, c7);
		}
	}
	
//	private static final short[][][][][][][] lut;
//	
//	static {
//		try (Input input = new UnsafeInput(LookupTables.class.getResourceAsStream("/lut.dat"))) {
//			lut = new Kryo().readObject(input, short[][][][][][][].class);
//		}
//	}
//	
//	static {
//		try (ObjectInput in = new ObjectInputStream(new BufferedInputStream(new FileInputStream("foo")))) {
//			lut = (short[][][][][][][]) in.readObject();
//		} catch (IOException | ClassNotFoundException e) {
//			throw new RuntimeException();
//		}
//	}
//	
//	@Test
//	public void testMultidimensional() {
//		long sum = 0;
//		for (int a = 0; a < 46; a++) {
//			short[][][][][][] sa = lut[a];
//			for (int b = a + 1; b < 47; b++) {
//				short[][][][][] sb = sa[b];
//				for (int c = b + 1; c < 48; c++) {
//					short[][][][] sc = sb[c];
//					for (int d = c + 1; d < 49; d++) {
//						short[][][] sd = sc[d];
//						for (int e = d + 1; e < 50; e++) {
//							short[][] se = sd[e];
//							for (int f = e + 1; f < 51; f++) {
//								short[] sf = se[f];
//								for (int g = f + 1; g < 52; g++) {
//									sum += sf[g];
//								}
//							}
//						}
//					}
//				}
//			}
//		}
//		assertNotEquals(sum, 0);
//	}
}