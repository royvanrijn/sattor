package com.royvanrijn.sattor.library;

import java.util.ArrayList;
import java.util.List;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;

/**
 * Inspiration: http://www.cs.toronto.edu/~fbacchus/csc2512/at_most_k.pdf
 *
 * Slightly optimized, omitting some variables/rules, making it even faster.
 *
 * I've optimized the algorithm and rewrote it to support <, ==, >.
 *
 * Very handy for symmetry breaking: combinedK, where you can have two groups that sum up to k-true values, where one is automatically larger than the other.
 */
public class Counting {

    /**
     * Ensure there are exactly k true values in seq
     * @param formula
     * @param seq Sequence to test
     * @param k Amount we're looking for
     */
    public static void atLeastK(final Formula formula, final VariableSequence seq, final int k) {
        count(formula, seq, k, false, true);
    }

    /**
     * Ensure there are exactly k true values in seq
     * @param formula
     * @param seq Sequence to test
     * @param k Amount we're looking for
     */
    public static void exactlyK(final Formula formula, final VariableSequence seq, final int k) {
        count(formula, seq, k, true, true);
    }

    /**
     * Ensure there are exactly k true values in seq
     * @param formula
     * @param seq Sequence to test
     * @param k Amount we're looking for
     */
    public static void atMostK(final Formula formula, final VariableSequence seq, final int k) {
        count(formula, seq, k, true, false);
    }

    /**
     * Main algorithm, supports all three <, ==, >
     * @param formula
     * @param seq
     * @param k
     * @param overflow
     * @param inverseRelations
     */
    private static int[] count(final Formula formula, final VariableSequence seq, final int k, final boolean overflow, final boolean inverseRelations) {

        int[] counter = new int[k];
        counter[0] = seq.get(0);

        for(int i = 1; i < seq.length() - 1; i++) {
            int[] bits = new int[k];
            bits[0] = formula.tempVariable();

            // Encode the first counter:
            formula.add(-seq.get(i) + " " + bits[0] + " 0");
            formula.add(-counter[0] + " " + bits[0] + " 0");

            if(inverseRelations) {
                formula.add(-bits[0] + " " + counter[0] + " " + seq.get(i) + " 0"); // inverse
            }

            for(int j = 1; j < Math.min(i+1, k); j++) {
                bits[j] = formula.tempVariable();
                formula.add(-seq.get(i) + " " + -counter[j-1] + " " + bits[j] + " 0");

                if(i > j) {
                    formula.add(-counter[j] + " " + bits[j] + " 0");
                    if(inverseRelations) {
                        formula.add(-bits[j] + " " + seq.get(i) + " " + counter[j] + " 0");
                        formula.add(-bits[j] + " " + counter[j-1] + " " + counter[j] + " 0");
                        formula.add(-bits[j] + " " + seq.get(i) + " " + counter[j-1] + " " + counter[j] + " 0");
                    }
                } else {
                    if(inverseRelations) {
                        formula.add(-bits[j] + " " + seq.get(i) + " 0");
                        formula.add(-bits[j] + " " + counter[j - 1] + " 0");
                    }
                }
            }
            if(overflow && i+1 > k) { // overflow protection:
                formula.add(-seq.get(i) + " " + -counter[k - 1] + " 0");
            }
            counter = bits;
        }

        if(overflow) {
            // final overflow
            formula.add(-seq.get(seq.length() - 1) + " " + -counter[k - 1] + " 0");
        }
        if(inverseRelations) {
            // add final inverse for omitted last bits, force them true:
            formula.add(seq.get(seq.length() - 1) + " " + counter[0] + " 0");
            for(int j = 1; j < k; j++) {
                // add larger inverse:
                formula.add(counter[j] + " " + seq.get(seq.length() - 1) + " 0");
                formula.add(counter[j] + " " + counter[j - 1] + " 0");
                formula.add(counter[j] + " " + seq.get(seq.length() - 1) + " " + counter[j - 1] + " 0");
            }
        }

        return counter;
    }

    private static int[] combinedInnerLoop(final Formula formula, final VariableSequence seq, final int k) {
        int[] counter = new int[k];
        counter[0] = seq.get(0);

        for(int i = 1; i < seq.length(); i++) { // Doesn't optimize the last loop, one more iteration using this
            int[] bits = new int[k];
            bits[0] = formula.tempVariable();

            // Encode the first counter:
            formula.add(-seq.get(i) + " " + bits[0] + " 0");
            formula.add(-counter[0] + " " + bits[0] + " 0");
            formula.add(-bits[0] + " " + counter[0] + " " + seq.get(i) + " 0"); // inverse

            for(int j = 1; j < Math.min(i+1, k); j++) {
                bits[j] = formula.tempVariable();
                formula.add(-seq.get(i) + " " + -counter[j-1] + " " + bits[j] + " 0");

                if(i > j) {
                    formula.add(-counter[j] + " " + bits[j] + " 0");
                    formula.add(-bits[j] + " " + seq.get(i) + " " + counter[j] + " 0");
                    formula.add(-bits[j] + " " + counter[j-1] + " " + counter[j] + " 0");
                    formula.add(-bits[j] + " " + seq.get(i) + " " + counter[j-1] + " " + counter[j] + " 0");
                } else {
                    formula.add(-bits[j] + " " + seq.get(i) + " 0");
                    formula.add(-bits[j] + " " + counter[j - 1] + " 0");
                }
            }
            if(i+1 > k) { // overflow protection:
                formula.add(-seq.get(i) + " " + -counter[k - 1] + " 0");
            }
            counter = bits;
        }
        return counter;
    }


    /**
     * Symmetry breaking counter.
     *
     * Combine atMost/atLeast into two groups, seq1 (largest) and seq2 (smallest)
     * Enforce that the total bits true is k.
     * But also enforce that group seq2 can't have more than ceil(k/2)
     *
     * @param formula
     * @param seq1
     * @param seq2
     * @param k
     * @return
     */
    public static void combinedK(final Formula formula, final VariableSequence seq1, final VariableSequence seq2, final int k) {

        int kMax = k;
        int kMin = kMax/2;

        int[] counter1 = combinedInnerLoop(formula, seq1, kMax);
        int[] counter2 = combinedInnerLoop(formula, seq2, kMin);

        // Force the first N-bits of counter1 (the largest) to be TRUE
        for(int i = 0; i < counter1.length - kMin; i++) {
            formula.add(counter1[i] + " 0");
        }

        List<Integer> oneOfThese = new ArrayList<>();
        // Encode the other combinations of 1 and 0 that we allow (faster than exactlyN because sorted):
        for(int amountInSeq2 = 0; amountInSeq2 <= kMin; amountInSeq2++) {
            int amountInSeq1 = kMin - amountInSeq2;
            int rule = formula.tempVariable();

            for(int count = 0; count < kMin; count++) {
                int i1 = counter1[counter1.length - kMin + count];
                int i2 = counter2[count];
                formula.add((count < amountInSeq1?i1:-i1) + " " + rule + " 0");
                formula.add((count < amountInSeq2?i2:-i2) + " " + rule + " 0");
            }
            oneOfThese.add(-rule);
        }
        String finalRule = "";
        for(int i : oneOfThese) {
            finalRule += (i+" ");
        }
        formula.add(finalRule + "0");
    }

}
