# monty

A Java 25 library for estimating equity and expected value in Texas hold 'em using Monte Carlo
simulation.

### Usage

Tell `Monty` your hole cards, the community cards, and the number of players including yourself:

```java
Monty monty = Monty.pocket(EIGHT.of(CLUBS), NINE.of(CLUBS))
                   .flop(SEVEN.of(CLUBS), TEN.of(CLUBS), ACE.of(HEARTS))
                   .players(4);
```

Get a lazy, parallel stream that pseudorandomly samples the possible outcomes given those
constraints, running independent trials on every available thread:

```java
IntStream trials = monty.limit(1_000_000);
```

The result of a trial is a nonnegative integer indicating the number of players that you split the
pot with, including yourself: 0 means you lost, 1 means you won, and n > 1 means an n-way tie. Or
`Monty` can determine how many trials to run and summarize the outcomes for you given an error
magnitude and confidence level:

```java
double error = 0.001d;
double confidence = 0.999_999d;
Equity equity = monty.equity(error, confidence);
```

Your equity is the fraction of the pot that you won on average across every trial, which converges
to the true average across every possible outcome. The magnitude of the difference between the true
equity and the estimate is less than the given `error` with the given `confidence` level:

```java
assert Math.abs(0.52279d - equity.estimate()) < error;
```

The expected value of a call is the ratio of your estimated winnings to the size of the raise:

```java
int pot = 100;
int raise = 50;
assert equity.expectedValue(pot, raise) > 1.0d;
```

Accuracy tends to increase with further trials, subject to diminishing returns.

### Monte Carlo simulation

The Monte Carlo simulation uses the `Deck` class to represent a deck of cards that can be lazily
shuffled (dealing and shuffling are interleaved, producing each card on demand, drawn uniformly at
random from the remaining cards in the deck with fast bounded random number generation using
[Lemire's method][1]). A deck can be split to run independent simulations in a parallel stream,
optionally providing a custom [`SplittableGenerator`][2] implementation. Throughput scales linearly
with the number of threads.

### Hand evaluation

The evaluator directly computes the value of the best five-card hand made from seven given cards
(without checking each of the 21 combinations) using bitwise logical operators, shifts, some
integer arithmetic (no multiplication, division, or modulo), and five reads from a 16KB lookup
table (comfortably fits in L1 cache). It is garbage free, does not use the standard library,
performs no pairwise card comparisons, and has no loops or conditional statements. The only
unpredictable branch is a tableswitch, indexed by a hash value to flatten what would otherwise be a
dense tree of unpredictable conditional branches into one small jump table.

Partial evaluations are represented with a persistent data structure (two `long` bit vectors in a
value class, built up one card at a time) that can be reused in the evaluation of each player's
hand for the community cards they all have in common.

### Benchmarks

On an AMD Ryzen 9 9950X3D at 5.7GHz, `Monty` can evaluate over 60 million random seven-card hands
per second per core (less than 100 CPU cycles per hand).

```bash
./mvnw clean package && java -jar benchmarks/target/benchmarks.jar -prof perfnorm -prof gc
```

| Benchmark                                      |  Mode |        Score |        Error |     Units |
|:-----------------------------------------------|------:|-------------:|-------------:|----------:|
| Monty.evaluateRandom                           | thrpt | 61340979.020 | ± 398049.638 |     ops/s |
| Monty.evaluateRandom:instructions:u            | thrpt |      238.167 | ±      0.151 |      #/op |
| Monty.evaluateRandom:cycles:u                  | thrpt |       92.897 | ±      3.329 |      #/op |
| Monty.evaluateRandom:stalled-cycles-frontend:u | thrpt |        9.308 | ±      3.170 |      #/op |
| Monty.evaluateRandom:IPC                       | thrpt |        2.564 | ±      0.093 | insns/clk |
| Monty.evaluateRandom:CPI                       | thrpt |        0.390 | ±      0.014 | clks/insn |
| Monty.evaluateRandom:branches:u                | thrpt |        9.565 | ±      0.002 |      #/op |
| Monty.evaluateRandom:branch-misses:u           | thrpt |        0.742 | ±      0.005 |      #/op |
| Monty.evaluateRandom:L1-dcache-loads:u         | thrpt |       56.644 | ±      0.174 |      #/op |
| Monty.evaluateRandom:L1-dcache-load-misses:u   | thrpt |        0.001 | ±      0.001 |      #/op |
| Monty.evaluateRandom:L1-icache-loads:u         | thrpt |        0.008 | ±      0.022 |      #/op |
| Monty.evaluateRandom:L1-icache-load-misses:u   | thrpt |        0.001 | ±      0.002 |      #/op |
| Monty.evaluateRandom:dTLB-loads:u              | thrpt |       ≈ 10⁻⁴ |              |      #/op |
| Monty.evaluateRandom:dTLB-load-misses:u        | thrpt |       ≈ 10⁻⁵ |              |      #/op |
| Monty.evaluateRandom:iTLB-loads:u              | thrpt |       ≈ 10⁻⁴ |              |      #/op |
| Monty.evaluateRandom:iTLB-load-misses:u        | thrpt |       ≈ 10⁻⁵ |              |      #/op |
| Monty.evaluateRandom:gc.alloc.rate             | thrpt |        0.001 | ±      0.001 |    MB/sec |
| Monty.evaluateRandom:gc.alloc.rate.norm        | thrpt |       ≈ 10⁻⁵ |              |      B/op |
| Monty.evaluateRandom:gc.count                  | thrpt |          ≈ 0 |              |    counts |

[1]: https://arxiv.org/abs/1805.10941
[2]: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/random/RandomGenerator.SplittableGenerator.html
