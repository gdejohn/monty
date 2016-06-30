package edu.msudenver.gdejohn.poker;

public enum Rank {
	TWO('2'),
	THREE('3'),
	FOUR('4'),
	FIVE('5'),
	SIX('6'),
	SEVEN('7'),
	EIGHT('8'),
	NINE('9'),
	TEN('T'),
	JACK('J'),
	QUEEN('Q'),
	KING('K'),
	ACE('A');
	
	private final char rank;
	
	Rank(char rank) {
		this.rank = rank;
	}
	
	@Override
	public String toString() {
		return String.valueOf(rank);
	}
}
