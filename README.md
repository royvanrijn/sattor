# SATTOR

SATTOR is a Java library that helps you to encode problems into SAT encoding (DIMACS format).

For some reason I've found a lot of information about optimizing and writing actual SAT solvers, but there seems to be a gap in efficiently encoding a problem into SAT.

## How to use

```java
// Create a formula:
Formula formula = Formula.create();

String b1 = "10011";
String b2 = "110110";

// Create a couple of variables (in a sequence) 
VariableSequence in1 = formula.newVariables(b1.length());
VariableSequence in2 = formula.newVariables(b2.length());

// Encode a binary adder (part of the library)
VariableSequence sum = Arithmetic.add(formula, in1, in2);

//Next constraint the input sequences (as two binary number)
for(int i = 0;i < b1.length(); i++) {
    formula.add((b1.charAt(i)=='0'?"-":"") + in1.get(i) + " 0");
}

for(int i = 0;i < b2.length(); i++) {
    formula.add((b2.charAt(i)=='0'?"-":"") + in2.get(i) + " 0");
}

// And write the DIMACS file:
formula.writeToFile("add_numbers.cnf");
```

This resulting file can be fed into any SAT solver (MiniSAT, Glucose, etc) to obtain a result.

## WIP

This is just a hobby project, I'm learning more about SAT and it's encoding as I go along. The idea is to solve more (toy) problems and simultaneously expending the library of common tricks/algorithms used in encoding problem.

I've written some blogposts (a while ago) on my website (https://royvanrijn.com) on how SAT solvers work, including how to encode problems.

## Example 1: Peaceable queens

One of the problems I solved using my SAT encoder was "Peaceable queens": How many queens (black and white) can be on an NxN board without being able to see/attack the opponent.

To do this I needed to implement constraints like "LTseq" (counting sequence) from the paper `SAT Encodings of the At-Most-k Constraint`. I've created (fast) constraints like `atMostK`, `atLeastK`, `exactlyK`, etc.

I've even added a `combinedK(seq1, seq2, k)` that counts up to `k` but breaks a symmetry such that there will always be more true variables in `seq1` over `seq2`

The code for generating the encoded DIMACS SAT input for peaceable queens can be found in `com.royvanrijn.examples`.

More information about the problem can be found here:
https://royvanrijn.com/blog/2019/05/sat-solving-part-one/


