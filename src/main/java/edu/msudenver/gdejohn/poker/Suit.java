package edu.msudenver.gdejohn.poker;

public enum Suit {
	DIAMONDS('♦'),
	CLUBS('♣'),
	HEARTS('♥'),
	SPADES('♠');
	
	private final char suit;
	
	Suit(char suit) {
		this.suit = suit;
	}
	
	@Override
	public String toString() {
		return String.valueOf(suit);
	}
}
