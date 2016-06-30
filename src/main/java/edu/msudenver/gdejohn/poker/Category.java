package edu.msudenver.gdejohn.poker;

public enum Category
{
	HIGH_CARD(1_302_540),
	ONE_PAIR(1_098_240),
	TWO_PAIR(123_552),
	THREE_OF_A_KIND(54_912),
	STRAIGHT(10_200),
	FLUSH(5_108),
	FULL_HOUSE(3_744),
	FOUR_OF_A_KIND(624),
	STRAIGHT_FLUSH(40);
	
	/**
	 * The number of distinct hands in this category.
	 */
	public final int hands;
	
	Category(int hands)
	{
		this.hands = hands;
	}
}