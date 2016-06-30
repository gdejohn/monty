package edu.msudenver.gdejohn.poker;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.EnumSet.noneOf;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Deck
{
	private final Card[] deck;
	
	private int index;
	
	private Deck(Card[] deck)
	{
		this.deck = deck;
		
		this.index = deck.length;
	}
	
	public Deck()
	{
		this(Card.values());
	}
	
	public Deck(Set<Card> cards)
	{
		this(cards.toArray(new Card[cards.size()]));
	}
	
	public int remaining()
	{
		return index;
	}
	
	public Card deal()
	{
		int randomIndex = ThreadLocalRandom.current().nextInt(index--);
		
		Card randomCard = deck[randomIndex];
		
		deck[randomIndex] = deck[index];
		
		deck[index] = randomCard;
		
		return randomCard;
	}
	
	public Set<Card> deal(int n)
	{
		Set<Card> cards = noneOf(Card.class);
		
		deal(n, cards);
		
		return cards;
	}
	
	public void deal(int n, Set<Card> cards)
	{
		for (int i = 0; i < n; i++)
		{
			cards.add(deal());
		}
	}
	
	public void rollBack(int n)
	{
		checkArgument((index += n) <= deck.length);
	}
}