package edu.msudenver.gdejohn.poker;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkPositionIndex;
import static com.google.common.base.Stopwatch.createStarted;
//import static edu.msudenver.gdejohn.poker.Card.EIGHT_CLUBS;
//import static edu.msudenver.gdejohn.poker.Card.JACK_CLUBS;
//import static edu.msudenver.gdejohn.poker.Card.NINE_CLUBS;
//import static edu.msudenver.gdejohn.poker.Card.QUEEN_CLUBS;
//import static edu.msudenver.gdejohn.poker.Card.TEN_CLUBS;
//import static edu.msudenver.gdejohn.poker.Card.THREE_HEARTS;
//import static edu.msudenver.gdejohn.poker.Card.TWO_HEARTS;
import static java.lang.Integer.signum;
import static java.util.Arrays.asList;
import static java.util.Arrays.binarySearch;
import static java.util.Arrays.setAll;
import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Stopwatch;

import net.jpountz.lz4.LZ4BlockInputStream;

public class HoldEmSimulator {
    private static final short[][][][][][][] preflop;
    
    static {
        try (ObjectInput in = new ObjectInputStream(new LZ4BlockInputStream(new BufferedInputStream(new FileInputStream("preflop"))))) {
            preflop = (short[][][][][][][]) in.readObject();
        }
        catch (ClassNotFoundException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    
    /**
     * Given a player's hole cards, the board, and the number of opponents,
     * simulates every possible game unique up to ordering of opponents and
     * returns the outcomes as an array where the zeroth element is the number
     * of losses, the first element is the number of wins, and the
     * {@code n}-th element is the number of {@code n}-way ties.
     */
    public static long[] outcomes(int opponents, int[] holeCards, int[] board) {
        long[] outcomes = new long[opponents + 2];
        
        short[][][][] flop = board.length >= 3 ? preflop[board[0]][board[1]][board[2]] : null;
        short[][][] turn = board.length >= 4 ? flop[board[3]] : null;
        short[][] river = board.length == 5 ? turn[board[4]] : null;
        short hand = board.length == 5 ? river[holeCards[0]][holeCards[1]] : 0;
        
        for (int[] deck : decks(holeCards, board)) {
            switch (board.length) {
                case 0: flop = preflop[deck[0]][deck[1]][deck[2]];
                case 3: turn = flop[deck[3 - board.length]];
                case 4: river = turn[deck[4 - board.length]];
                        hand = river[holeCards[0]][holeCards[1]];
            }
            
            for (int[][] hands : hands(deck, board, opponents)) {
                int split = 1;
                
                loop: for (int[] opponent : hands) {
                    switch (signum(river[opponent[0]][opponent[1]] - hand)) {
                        case 1: split = 0;
                                break loop;
                        case 0: split++;
                    }
                }
                
                outcomes[split]++;
            }
        }
        
        return outcomes;
    }
    
    static Iterable<int[]> decks(int[] holeCards, int[] board) {
        return null;
    }
    
    static Iterable<int[][]> hands(int[] deck, int[] board, int opponents) {
        return null;
    }
    
    private static long[] enumerate(int opponents, int[] holeCards, int[] board, int[] deck) {
        long[] outcomes = new long[opponents + 2];
        
        short[][][][] flop = board.length >= 3 ? preflop[board[0]][board[1]][board[2]] : null;
        short[][][] turn = board.length >= 4 ? flop[board[3]] : null;
        short[][] river = board.length == 5 ? turn[board[4]] : null;
        short hand = board.length == 5 ? river[holeCards[0]][holeCards[1]] : 0;
        
        int[] partition = new int[deck.length];
        int[] combination = new int[opponents * 2]; // opponent cards, unassigned
        int[] hands = new int[combination.length]; // opponent hands
        
        setAll(partition, i -> i);
        while (true) {
            switch (board.length) {
                case 0: flop = preflop[deck[partition[0]]][deck[partition[1]]][deck[partition[2]]];
                case 3: turn = flop[deck[partition[3 - board.length]]];
                case 4: river = turn[deck[partition[4 - board.length]]];
                        hand = river[holeCards[0]][holeCards[1]]; // player hand value
            }
            
            setAll(combination, i -> i + 5 - board.length);
            while (true) {
                
                setAll(hands, i -> i);
                partitions: while (true) {
                    int split = 1;
                    for (int i = 0; split > 0 && i < hands.length; i += 2) {
                        int j = partition[combination[hands[i]]];
                        int k = partition[combination[hands[i + 1]]];
                        switch (signum(river[deck[j]][deck[k]] - hand)) {
                            case 1: split = 0;
                                    break;
                            case 0: split++;
                        }
                    }
                    outcomes[split]++;
                    
                    // next partition
                    int k = hands.length - 1;
                    NavigableSet<Integer> set = new TreeSet<>();
                    for (int index = k; index >= 1; index -= 2) {
                        int i = hands[index];
                        set.add(i);
                        NavigableSet<Integer> tail = set.tailSet(i, false);
                        if (tail.isEmpty()) {
                            set.add(hands[index - 1]);
                        }
                        else {
                            hands[index] = tail.pollFirst();
                            for (int j = k; j > index; j--) {
                                hands[j] = set.pollLast();
                            }
                            continue partitions;
                        }
                    }
                    break;
                }
                
                // next combination
                int i = combination.length - 1;
                while (i >= 0 && combination[i] == partition.length - combination.length + i) {
                    i--;
                }
                if (i < 0) {
                    break;
                }
                combination[i]++;
                for (int j = i; j < combination.length; j++) {
                    combination[j] = combination[i] + j - i;
                }
            }
            
            // next board
            int k = 5 - board.length;
            int i = k - 1;
            while (i >= 0 && partition[i] == partition.length - k + i) {
                i--;
            }
            if (i < 0) {
                break;
            }
            partition[i]++;
            for (int j = i; j < k; j++) {
                partition[j] = partition[i] + j - i;
            }
            i = binarySearch(partition, k, partition.length, partition[i]);
            partition[i]--;
            for (int j = i + 1; j < partition.length; j++) {
                partition[j] = j;
            }
        }
        
        return outcomes;
    }
    
    public static long[] enumerate(int opponents, Card[] holeCards, Card[] board) {
        checkPositionIndex(opponents, 22);
        checkNotNull(holeCards);
        checkNotNull(board);
        checkArgument(holeCards.length == 2);
        checkArgument(asList(0, 3, 4, 5).contains(board.length));
        
        Set<Card> set = EnumSet.allOf(Card.class);
        concat(stream(holeCards), stream(board)).forEach(
            card -> checkArgument(set.remove(checkNotNull(card)))
        );
        int[] deck = set.stream().mapToInt(Card::ordinal).toArray();
        
        return enumerate(opponents, stream(holeCards).mapToInt(Card::ordinal).toArray(), stream(board).mapToInt(Card::ordinal).toArray(), deck);
    }
    
    public static double equity(int opponents, Card[] holeCards, Card[] board) {
        long[] outcomes = enumerate(opponents, holeCards, board);
        double winnings = 0d;
        long games = outcomes[0];
        for (int i = 1; i < outcomes.length; i++) {
            winnings += (double) outcomes[i] / i;
            games += outcomes[i];
        }
        return winnings / games;
    }
    
    /**
     * Average winnings, expressed as a fraction of the raise.
     */
    public static double expectedValue(int pot, int raise, int opponents, Card[] holeCards, Card[] board) {
        return equity(opponents, holeCards, board) * (pot + raise) / raise;
    }
    
    public static long[] sample(int trials, int opponents, Card[] holeCards, Card[] board) {
        checkPositionIndex(opponents, 22);
        checkNotNull(holeCards);
        checkNotNull(board);
        checkArgument(holeCards.length == 2);
        checkArgument(asList(0, 3, 4, 5).contains(board.length));

        Set<Card> set = EnumSet.allOf(Card.class);
        concat(stream(holeCards), stream(board)).forEach(
            card -> checkArgument(set.remove(checkNotNull(card)))
        );
        int[] deck = set.stream().mapToInt(Card::ordinal).toArray();
        
        return sample(trials, opponents, stream(holeCards).mapToInt(Card::ordinal).toArray(), stream(board).mapToInt(Card::ordinal).toArray(), deck);
    }
    
    static class Deck {
        int[] deck;
        int i;
        Random random = new Random();
        
        Deck(int[] deck) {
            this.deck = deck;
            this.shuffle();
        }
        
        int deal() {
            int j = random.nextInt(i--);
            int k = deck[j];
            deck[j] = deck[i];
            deck[i] = k;
            return k;
        }
        
        void shuffle() {
            this.i = this.deck.length;
        }
    }
    
    private static long[] sample(int trials, int opponents, int[] holeCards, int[] board, int[] cards) {
        long[] outcomes = new long[opponents + 2];
        
        short[][][][] flop = board.length >= 3 ? preflop[board[0]][board[1]][board[2]] : null;
        short[][][] turn = board.length >= 4 ? flop[board[3]] : null;
        short[][] river = board.length == 5 ? turn[board[4]] : null;
        short hand = board.length == 5 ? river[holeCards[0]][holeCards[1]] : 0;
        
        for (Deck deck = new Deck(cards); trials > 0; deck.shuffle(), trials--) {
            switch (board.length) {
                case 0: flop = preflop[deck.deal()][deck.deal()][deck.deal()];
                case 3: turn = flop[deck.deal()];
                case 4: river = turn[deck.deal()];
                        hand = river[holeCards[0]][holeCards[1]];
            }
            
            int split = 1;
            
            for (int i = 0; split > 0 && i < opponents; i++) {
                switch (signum(river[deck.deal()][deck.deal()] - hand)) {
                    case 1: split = 0;
                            break;
                    case 0: split++;
                }
            }
            
            outcomes[split]++;
        }
        
        return outcomes;
    }
    
    public static double equity(int trials, int opponents, Card[] holeCards, Card[] board) {
        long[] outcomes = sample(trials, opponents, holeCards, board);
        double winnings = 0d;
        long games = outcomes[0];
        for (int i = 1; i < outcomes.length; i++) {
            winnings += (double) outcomes[i] / i;
            games += outcomes[i];
        }
        return winnings / games;
    }
    
    public static void main(String[] args) throws Exception {
        Map<String, Card> map = new HashMap<>();
        {
            int i = 0;
            for (String suit : new String[] {"d", "c", "h", "s"}) {
                for (String rank : new String[] {"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"}) {
                    map.put(rank + suit, Card.getCard(i++));
                }
            }
        }
        try (BufferedReader input = new BufferedReader(new InputStreamReader(System.in))) {
            NumberFormat format = NumberFormat.getIntegerInstance();
            System.out.print("Opponents? ");
            int opponents = format.parse(input.readLine()).intValue();
            System.out.print("Hole cards? ");
            String[] holeCardStrings = input.readLine().split(" ");
            System.out.print("Board? ");
            String[] boardStrings = input.readLine().split(" ");
            Card[] holeCards = new Card[2]; // {EIGHT_CLUBS, NINE_CLUBS};
            for (int i = 0; i < holeCardStrings.length; i++) {
                holeCards[i] = map.get(holeCardStrings[i]);
            }
            Card[] board = new Card[boardStrings.length]; // {TEN_CLUBS, JACK_CLUBS, QUEEN_CLUBS, TWO_HEARTS, THREE_HEARTS};
            for (int i = 0; i < boardStrings.length; i++) {
                board[i] = map.get(boardStrings[i]);
            }
            System.out.println("--------------------");
            Stopwatch stopwatch = createStarted();
            long[] outcomes = enumerate(opponents, holeCards, board);
            stopwatch.stop();
            double winnings = 0d;
            long games = outcomes[0];
            for (int i = 1; i < outcomes.length; i++) {
                winnings += (double) outcomes[i] / i;
                games += outcomes[i];
            }
            double equity = winnings / games;
            System.out.printf("     Board: %s%nHole cards: %s%n    Equity: %f%n      Time: %s%n     Games: %,d%n", Arrays.toString(board), Arrays.toString(holeCards), equity, stopwatch, games);
            for (int i = 0; i < outcomes.length; i++) {
                switch (i) {
                    case 0: System.out.printf("    Losses: %,d%n", outcomes[i]);
                            break;
                    case 1: System.out.printf("      Wins: %,d%n", outcomes[i]);
                            break;
                    default: System.out.printf("%d-way ties: %,d%n", i, outcomes[i]);
                }
            }
        }
        
//        class HoleCards {
//            Rank[] ranks;
//            boolean suited;
//            
//            HoleCards(Rank first, Rank second, boolean suited) {
//                this.ranks = new Rank[] {first, second};
//                this.suited = suited;
//            }
//            
//            HoleCards(Rank first, Rank second) {
//                this(first, second, false);
//            }
//            
//            @Override
//            public String toString() {
//                return ranks[0].toString() + ranks[1].toString() + (ranks[0] == ranks[1] ? "" : suited ? "s" : "o");
//            }
//        }
//        java.util.SortedMap<Double, HoleCards> map = new java.util.TreeMap<>(java.util.Comparator.reverseOrder());
//        Rank[] ranks = Rank.values();
//        Card[] cards = Card.values();
//        int trials = 1_000_000;
//        int opponents = 22;
//        for (int i = 0; i < 13; i++) {
//            map.put(equity(trials, opponents, new Card[] {cards[i], cards[13 + i]}, new Card[0]), new HoleCards(ranks[i], ranks[i]));
//            for (int j = i + 1; j < 13; j++) {
//                map.put(equity(trials, opponents, new Card[] {cards[i], cards[j]}, new Card[0]), new HoleCards(ranks[j], ranks[i], true));
//                map.put(equity(trials, opponents, new Card[] {cards[i], cards[13 + j]}, new Card[0]), new HoleCards(ranks[j], ranks[i], false));
//            }
//        }
//        map.entrySet().stream().forEachOrdered(entry -> System.out.println(entry.getValue() + ": " + entry.getKey()));
        
//        try (java.io.BufferedWriter file = java.nio.file.Files.newBufferedWriter(java.nio.file.Paths.get("foo3.txt"))) {
//            int[] partition = new int[10];
//            int[] combination = new int[4]; // opponent cards, unassigned
//            int[] hands = new int[4]; // opponent hands
//            setAll(partition, i -> i);
//            while (true) {
//                setAll(combination, i -> i + 3);
//                while (true) {
//                    setAll(hands, i -> i);
//                    partitions: while (true) {
//                        java.util.NavigableSet<Integer> s = new TreeSet<>(asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));
//                        s.remove(partition[0]);
//                        s.remove(partition[1]);
//                        s.remove(partition[2]);
//                        s.remove(partition[combination[hands[0]]]);
//                        s.remove(partition[combination[hands[1]]]);
//                        s.remove(partition[combination[hands[2]]]);
//                        s.remove(partition[combination[hands[3]]]);
//                        file.write(String.format("({%d,%d,%d},{{%d,%d},{%d,%d}},{%d,%d,%d})%n", partition[0], partition[1], partition[2], partition[combination[hands[0]]], partition[combination[hands[1]]], partition[combination[hands[2]]], partition[combination[hands[3]]], s.pollFirst(), s.pollFirst(), s.pollFirst()));
//                        int k = hands.length - 1;
//                        NavigableSet<Integer> set = new TreeSet<>();
//                        for (int index = k; index >= 1; index -= 2) {
//                            int i = hands[index];
//                            set.add(i);
//                            NavigableSet<Integer> tail = set.tailSet(i, false);
//                            if (tail.isEmpty()) {
//                                set.add(hands[index - 1]);
//                            }
//                            else {
//                                hands[index] = tail.pollFirst();
//                                for (int j = k; j > index; j--) {
//                                    hands[j] = set.pollLast();
//                                }
//                                continue partitions;
//                            }
//                        }
//                        break;
//                    }
//                    
//                    // next combination
//                    int i = combination.length - 1;
//                    while (i >= 0 && combination[i] == partition.length - combination.length + i) {
//                        i--;
//                    }
//                    if (i < 0) {
//                        break;
//                    }
//                    combination[i]++;
//                    for (int j = i; j < combination.length; j++) {
//                        combination[j] = combination[i] + j - i;
//                    }
//                }
//                
//                // next board
//                int k = 3;
//                int i = k - 1;
//                while (i >= 0 && partition[i] == partition.length - k + i) {
//                    i--;
//                }
//                if (i < 0) {
//                    break;
//                }
//                partition[i]++;
//                for (int j = i; j < k; j++) {
//                    partition[j] = partition[i] + j - i;
//                }
//                i = binarySearch(partition, k, partition.length, partition[i]);
//                partition[i]--;
//                for (int j = i + 1; j < partition.length; j++) {
//                    partition[j] = j;
//                }
//            }
//        }
        
//        long l = 5354228880L;
//        long[] ls = new long[23];
//        for (int i = 0; i < 23; i++) {
//            ls[i] = l / (i + 1);
//        }
//        new java.util.Random().ints(1_722_633_126L, 0, 23).forEach(
//            i -> {
//                x += ls[i];
//                y += l;
//                
//                d += 1.0d / (i + 1);
//                z++;
//            }
//        );
//        System.out.println(new java.math.BigDecimal(x).divide(new java.math.BigDecimal(y), java.math.MathContext.DECIMAL128));
//        System.out.println(d / z);
    }
    
//    static long x = 0L;
//    static long y = 0L;
//    static double d = 0.0d;
//    static long z = 0L;
}
