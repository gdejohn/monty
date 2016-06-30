package edu.msudenver.gdejohn.poker;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Range.closed;
import static com.google.common.collect.Sets.complementOf;
import static com.google.common.collect.Sets.immutableEnumSet;
import static com.google.common.collect.Sets.union;
import static edu.msudenver.gdejohn.poker.Card.deck;
import static edu.msudenver.gdejohn.poker.LookupTables.rankSeven;
import static java.util.EnumSet.copyOf;
import static java.util.EnumSet.noneOf;

import java.util.EnumSet;
import java.util.Set;

public class Player
{
	public static double equity(Set<Card> holeCards, Set<Card> board, int opponents, int samples)
	{
		holeCards = immutableEnumSet(holeCards);
		
		Set<Card> actualBoard = immutableEnumSet(board);
		
		checkArgument(holeCards.size() == 2);
		
		checkArgument(actualBoard.size() <= 5);
		
		checkArgument(closed(1, 10).contains(opponents));
		
		checkArgument(samples > 0);
		
		board = actualBoard.isEmpty() ? noneOf(Card.class) : copyOf(actualBoard);
		
		Set<Card> opponentCards = noneOf(Card.class);
		
		int cardsToCome = 5 - board.size();
		
		Deck deck = new Deck(complementOf(union(holeCards, board)));
		
		double winnings = 0d;
		
		for (int sample = 0; sample < samples; sample++)
		{
			deck.deal(cardsToCome, board);
			
			short equivalenceClass = rankSeven(copyOf(union(holeCards, board)));
			
			double equity = 1d;
			
			int opponent = 0;
			
			while (opponent < opponents)
			{
				opponent++;
				
				deck.deal(2, opponentCards);
				
				int comparison = equivalenceClass - rankSeven(copyOf(union(opponentCards, board)));
				
				opponentCards.clear();
				
				if (comparison < 0)
				{
					equity = 0;
					
					break;
				}
				else if (comparison == 0)
				{
					equity -= 1d / (opponents + 1);
				}
			}
			
			winnings += equity;
			
			board.retainAll(actualBoard);
			
			deck.rollBack(cardsToCome + (opponent * 2));
		}
		
		return winnings / samples;
	}
	
	public static double equity(Set<Card> holeCards, Set<Card> board, int opponents)
	{
		return equity(holeCards, board, opponents, 1_000_000);
	}
	
	public static double equity(Set<Card> holeCards, int opponents)
	{
		return equity(holeCards, EnumSet.noneOf(Card.class), opponents);
	}
	
	public static int[][] foo(Set<Card> live, Set<Card> dead)
	{
		int[][] array = new int[2][];
		
		array[0] = new int[live.size()];
		
		array[1] = new int[52 - live.size() - dead.size()];
		
		int i = 0;
		
		int j = 0;
		
		for (Card card : deck)
		{
			if (!dead.contains(card))
			{
				if (live.contains(card))
				{
					array[0][i++] = card.ordinal();
				}
				else
				{
					array[1][j++] = card.ordinal();
				}
			}
		}
		
		return array;
	}
}