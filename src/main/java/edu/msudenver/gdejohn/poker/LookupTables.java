package edu.msudenver.gdejohn.poker;

import static com.google.common.collect.Multimaps.asMap;
import static com.google.common.math.IntMath.binomial;
import static java.lang.Long.numberOfTrailingZeros;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;

public class LookupTables {
	private static final int[][] choose = new int[8][];
	
	private static final short[] fiveCardRanks;
	
	static final short[] sevenCardRanks;
	
	static {
		for (int k = 1; k < 8; k++) {
			choose[k] = new int[52];
			for (int n = 0; n < 52; n++) {
				choose[k][n] = k > n ? 0 : binomial(n, k);
			}
		}
		try (ObjectInput in = new ObjectInputStream(new BufferedInputStream(Hand.class.getResourceAsStream("/five_card_ranks.dat")))) {
			fiveCardRanks = (short[]) in.readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
		try (ObjectInput in = new ObjectInputStream(new BufferedInputStream(Hand.class.getResourceAsStream("/seven_card_ranks.dat")))) {
			sevenCardRanks = (short[]) in.readObject();
		}
		catch (IOException | ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}
	
	public static short[] generateFiveCombinations() {
		SetMultimap<NaiveHand, Integer> equivalenceClasses = TreeMultimap.create();
		for (Set<Card> cards : Combinations.choose(Card.class, 5)) {
			equivalenceClasses.put(new NaiveHand(cards), index(cards));
		}
		short[] fiveCombinations = new short[binomial(52, 5)];
		short rank = 0;
		for (Set<Integer> equivalenceClass : asMap(equivalenceClasses).values()) {
			for (int index : equivalenceClass) {
				fiveCombinations[index] = rank;
			}
			rank++;
		}
		return fiveCombinations;
	}
	
	public static short[] generateSevenCombinations(short[] fiveCombinations) {
		short[] sevenCombinations = new short[binomial(52, 7)];
		for (int[] i = new int[7]; i[0] < 46; i[0]++) {
			for (i[1] = i[0] + 1; i[1] < 47; i[1]++) {
				for (i[2] = i[1] + 1; i[2] < 48; i[2]++) {
					for (i[3] = i[2] + 1; i[3] < 49; i[3]++) {
						for (i[4] = i[3] + 1; i[4] < 50; i[4]++) {
							for (i[5] = i[4] + 1; i[5] < 51; i[5]++) {
								for (i[6] = i[5] + 1; i[6] < 52; i[6]++) {
									short equivalenceClass = 0;
									for (int a = 0; a < 3; a++) {
										for (int b = a + 1; b < 4; b++) {
											for (int c = b + 1; c < 5; c++) {
												for (int d = c + 1; d < 6; d++) {
													for (int e = d + 1; e < 7; e++) {
														int index = 0;
														index += choose(i[a], 1);
														index += choose(i[b], 2);
														index += choose(i[c], 3);
														index += choose(i[d], 4);
														index += choose(i[e], 5);
														short candidate = fiveCombinations[index];
														if (candidate > equivalenceClass) {
															equivalenceClass = candidate;
														}
													}
												}
											}
										}
									}
									int index = 0;
									int k = 1;
									for (int n : i) {
										index += choose(n, k++);
									}
									sevenCombinations[index] = equivalenceClass;
								}
							}
						}
					}
				}
			}
		}
		return sevenCombinations;
	}
	
	public static void saveLookupTable(short[] lookupTable, String name) throws IOException {
		try (ObjectOutput out = new ObjectOutputStream(new FileOutputStream(name))) {
			out.writeObject(lookupTable);
		}
	}
	
	public static int choose(int n, int k) {
		return choose[k][n];
	}
	
	public static int index(Set<Card> cards) {
		int index = 0;
		int k = 1;
		for (Card card : cards) {
			index += choose(card.ordinal(), k++);
		}
		return index;
	}
	
	public static short rank(int index) {
		return fiveCardRanks[index];
	}
	
	public static short rank(Set<Card> cards) {
		return rank(index(cards));
	}
	
	public static short rankSeven(int index) {
		return sevenCardRanks[index];
	}
	
	public static short rankSeven(Set<Card> cards) {
		return rankSeven(index(cards));
	}
	
	public static short rankOrdered(int c1, int c2, int c3, int c4, int c5) {
		int index = choose[1][c1];
		index += choose[2][c2];
		index += choose[3][c3];
		index += choose[4][c4];
		index += choose[5][c5];
		return fiveCardRanks[index];
	}
	
	public static short rankOrdered(int c1, int c2, int c3, int c4, int c5, int c6, int c7) {
		int index = choose[1][c1];
		index += choose[2][c2];
		index += choose[3][c3];
		index += choose[4][c4];
		index += choose[5][c5];
		index += choose[6][c6];
		index += choose[7][c7];
		return sevenCardRanks[index];
	}
	
	public static short rank(int c1, int c2, int c3, int c4, int c5, int c6, int c7) {
		return rank((1L << c1) | (1L << c2) | (1L << c3) | (1L << c4) | (1L << c5) | (1L << c6) | (1L << c7));
	}
	
	public static short rank(int[] cards) {
		return rank((1L << cards[45]) | (1L << cards[46]) | (1L << cards[47]) | (1L << cards[48]) | (1L << cards[49]) | (1L << cards[50]) | (1L << cards[51]));
	}
	
	public static short rank(long hand) {
		int index = 0;
		for (int k = 1; k <= 7; k++) {
			int n = numberOfTrailingZeros(hand);
			index += choose(n, k);
			hand &= hand - 1;
		}
		return sevenCardRanks[index];
	}
	
	public static short rankSortingNetwork(int c1, int c2, int c3, int c4, int c5, int c6, int c7) {
		if (c2 > c3) {
			int t = c2;
			c2 = c3;
			c3 = t;
		}
		if (c4 > c5) {
			int t = c4;
			c4 = c5;
			c5 = t;
		}
		if (c6 > c7) {
			int t = c6;
			c6 = c7;
			c7 = t;
		}
		if (c1 > c3) {
			int t = c1;
			c1 = c3;
			c3 = t;
		}
		if (c4 > c6) {
			int t = c4;
			c4 = c6;
			c6 = t;
		}
		if (c5 > c7) {
			int t = c5;
			c5 = c7;
			c7 = t;
		}
		if (c1 > c2) {
			int t = c1;
			c1 = c2;
			c2 = t;
		}
		if (c5 > c6) {
			int t = c5;
			c5 = c6;
			c6 = t;
		}
		if (c3 > c7) {
			int t = c3;
			c3 = c7;
			c7 = t;
		}
		if (c1 > c5) {
			int t = c1;
			c1 = c5;
			c5 = t;
		}
		if (c2 > c6) {
			int t = c2;
			c2 = c6;
			c6 = t;
		}
		if (c1 > c4) {
			int t = c1;
			c1 = c4;
			c4 = t;
		}
		if (c3 > c6) {
			int t = c3;
			c3 = c6;
			c6 = t;
		}
		if (c2 > c4) {
			int t = c2;
			c2 = c4;
			c4 = t;
		}
		if (c3 > c5) {
			int t = c3;
			c3 = c5;
			c5 = t;
		}
		if (c3 > c4) {
			int t = c3;
			c3 = c4;
			c4 = t;
		}
		return rankOrdered(c1, c2, c3, c4, c5, c6, c7);
	}
	
	public static short[][][][][] generateFiveCardDAG() throws IOException {
		short[][][][][] lut = new short[52][][][][];
		
		for (int a = 0; a < 52; a++)
			lut[a] = new short[52][][][];
		
		for (int a = 0; a < 51; a++)
			for (int b = a + 1; b < 52; b++)
				lut[a][b] =
				lut[b][a] = new short[52][][];
		
		for (int a = 0; a < 50; a++)
			for (int b = a + 1; b < 51; b++)
				for (int c = b + 1; c < 52; c++)
					lut[a][b][c] =
					lut[a][c][b] =
					lut[b][c][a] = new short[52][];
		
		for (int a = 0; a < 49; a++)
			for (int b = a + 1; b < 50; b++)
				for (int c = b + 1; c < 51; c++)
					for (int d = c + 1; d < 52; d++)
						lut[a][b][c][d] =
						lut[a][b][d][c] =
						lut[a][c][d][b] =
						lut[b][c][d][a] = new short[52];
		
		for (int a = 0; a < 48; a++)
			for (int b = a + 1; b < 49; b++)
				for (int c = b + 1; c < 50; c++)
					for (int d = c + 1; d < 51; d++)
						for (int e = d + 1; e < 52; e++)
							lut[a][b][c][d][e] =
							lut[a][b][c][e][d] =
							lut[a][b][d][e][c] =
							lut[a][c][d][e][b] =
							lut[b][c][d][e][a] = rankOrdered(a, b, c, d, e);
		
		return lut;
	}
	
	public static short[][][][][][][] generateSevenCardDAG() throws IOException {
		short[][][][][][][] lut = new short[52][][][][][][];
		
		for (int a = 0; a < 52; a++)
			lut[a] = new short[52][][][][][];
		
		for (int a = 0; a < 51; a++)
			for (int b = a + 1; b < 52; b++)
				lut[a][b] =
				lut[b][a] = new short[52][][][][];
		
		for (int a = 0; a < 50; a++)
			for (int b = a + 1; b < 51; b++)
				for (int c = b + 1; c < 52; c++)
					lut[a][b][c] =
					lut[a][c][b] =
					lut[b][c][a] = new short[52][][][];
		
		for (int a = 0; a < 49; a++)
			for (int b = a + 1; b < 50; b++)
				for (int c = b + 1; c < 51; c++)
					for (int d = c + 1; d < 52; d++)
						lut[a][b][c][d] =
						lut[a][b][d][c] =
						lut[a][c][d][b] =
						lut[b][c][d][a] = new short[52][][];
		
		for (int a = 0; a < 48; a++)
			for (int b = a + 1; b < 49; b++)
				for (int c = b + 1; c < 50; c++)
					for (int d = c + 1; d < 51; d++)
						for (int e = d + 1; e < 52; e++)
							lut[a][b][c][d][e] =
							lut[a][b][c][e][d] =
							lut[a][b][d][e][c] =
							lut[a][c][d][e][b] =
							lut[b][c][d][e][a] = new short[52][];
		
		for (int a = 0; a < 47; a++)
			for (int b = a + 1; b < 48; b++)
				for (int c = b + 1; c < 49; c++)
					for (int d = c + 1; d < 50; d++)
						for (int e = d + 1; e < 51; e++)
							for (int f = e + 1; f < 52; f++)
								lut[a][b][c][d][e][f] =
								lut[a][b][c][d][f][e] =
								lut[a][b][c][e][f][d] =
								lut[a][b][d][e][f][c] =
								lut[a][c][d][e][f][b] =
								lut[b][c][d][e][f][a] = new short[52];
		
		for (int a = 0; a < 46; a++)
			for (int b = a + 1; b < 47; b++)
				for (int c = b + 1; c < 48; c++)
					for (int d = c + 1; d < 49; d++)
						for (int e = d + 1; e < 50; e++)
							for (int f = e + 1; f < 51; f++)
								for (int g = f + 1; g < 52; g++)
									lut[a][b][c][d][e][f][g] =
									lut[a][b][c][d][e][g][f] =
									lut[a][b][c][d][f][g][e] =
									lut[a][b][c][e][f][g][d] =
									lut[a][b][d][e][f][g][c] =
									lut[a][c][d][e][f][g][b] =
									lut[b][c][d][e][f][g][a] = rankOrdered(a, b, c, d, e, f, g);
		
		return lut;
	}
	
	public static <T> T[][][] twelveFoldWay(T[] balls, int[][] boxes) {
		return null;
	}
	
	public static void main0(String[] args) throws IOException {
		try (ObjectOutput out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream("foo")))) {
			out.writeObject(generateSevenCardDAG());
		}
	}
	
	public static void main1(String[] args) throws IOException, ClassNotFoundException {
		try (ObjectInput in = new ObjectInputStream(new BufferedInputStream(new FileInputStream("foo")))) {
			System.out.println(((short[][][][][][][]) in.readObject())[0][1][2][3][4][5][6]);
		}
	}
	
	public static void main2(String[] args) throws IOException {
		try (Input input = new UnsafeInput(new GZIPInputStream(new BufferedInputStream(new FileInputStream("lut.gz"))))) {
			short[][][][][][][] lut = new Kryo().readObject(input, short[][][][][][][].class);
			System.out.println(lut[11][34][10][8][41][12][9]);
		}
	}
	
	public static void main(String[] args) {
		//ByteBuffer buffer = ByteBuffer.allocateDirect(0);
		
		long bytes = 0;
		
		for (int a = 0; a < 52; a++, bytes += 4 * 52);
		
		for (int a = 0; a < 51; a++)
			for (int b = a + 1; b < 52; b++, bytes += 4 * 52);
		
		for (int a = 0; a < 50; a++)
			for (int b = a + 1; b < 51; b++)
				for (int c = b + 1; c < 52; c++, bytes += 4 * 52);
		
		for (int a = 0; a < 49; a++)
			for (int b = a + 1; b < 50; b++)
				for (int c = b + 1; c < 51; c++)
					for (int d = c + 1; d < 52; d++, bytes += 4 * 52);
		
		for (int a = 0; a < 48; a++)
			for (int b = a + 1; b < 49; b++)
				for (int c = b + 1; c < 50; c++)
					for (int d = c + 1; d < 51; d++)
						for (int e = d + 1; e < 52; e++, bytes += 4 * 52);
		
		for (int a = 0; a < 47; a++)
			for (int b = a + 1; b < 48; b++)
				for (int c = b + 1; c < 49; c++)
					for (int d = c + 1; d < 50; d++)
						for (int e = d + 1; e < 51; e++)
							for (int f = e + 1; f < 52; f++, bytes += 2);
		
		System.out.println(bytes);
	}
	
	static int foo(ByteBuffer buffer, int... ints) {
		return IntStream.of(ints).reduce(0, (index, i) -> buffer.getInt(index + i));
	}
}