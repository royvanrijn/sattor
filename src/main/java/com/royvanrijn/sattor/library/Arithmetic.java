package com.royvanrijn.sattor.library;

import static com.royvanrijn.sattor.library.Gates.xor;
import static com.royvanrijn.sattor.library.Helper.padToLength;

import java.util.ArrayList;
import java.util.List;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;

/**
 * Convenience class which implements binary additions and multiplication.
 *
 * Made as proof of concept because SAT programs can run in reverse, build the circuit, fix the output, factor a number.
 */
public class Arithmetic {


    public static void fullAdder(Formula formula, int in1, int in2, int in3, int out, int carry) {
        if(in1 == 0 || in2 == 0 || in3 == 0 || out == 0 || carry == 0) {
            throw new IllegalArgumentException("Invalid variable");
        }

        int temp = formula.tempVariable();

        xor(formula, in1, in2, temp);
        xor(formula, temp, in3, out);

        formula.add(-in1 + " " + -in2 + " " + carry + " 0");
        formula.add(-in2 + " " + -in3 + " " + carry + " 0");
        formula.add(-in1 + " " + -in3 + " " + carry + " 0");

        formula.add(in1 + " " + in2 + " " + -carry + " 0");
        formula.add(in2 + " " + in3 + " " + -carry + " 0");
        formula.add(in1 + " " + in3 + " " + -carry + " 0");
    }

    public static void halfAdder(Formula formula, int in1, int in2, int out, int carry) {
        if(in1 == 0 || in2 == 0 || out == 0 || carry == 0) {
            throw new IllegalArgumentException("Invalid variable");
        }
        xor(formula, in1, in2, out);
        Gates.and(formula, in1, in2, carry);
    }

    /**
     * Add two numbers together (in binary representation)
     * @param formula
     * @param seq1
     * @param seq2
     * @return sum
     */
    public static VariableSequence add(Formula formula, VariableSequence seq1, VariableSequence seq2) {

        //REMARK: I've also successfully tried CLA (carry look-ahead addition) but it needed too much clauses, making it slower.
        int length = Math.max(seq1.length(), seq2.length());
        int coverLength = Math.min(seq1.length(), seq2.length());

        VariableSequence sum = formula.newVariables(length+1);

        // Initialize carry variable, start with 0 (no carry for the first bit)
        int previousCarry = 0;

        // Loop through each bit position
        for (int i = 0; i < length; i++) {

            int currentCarry;
            if(i < length-1) {
                currentCarry = formula.tempVariable(); // Temporary variable for the carry output
            } else {
                //Final carry:
                currentCarry = sum.get(0); // For the last iteration, the carry is the sum msb
            }

            int targetSum = sum.get(sum.length()-1-i);

            if(i < coverLength) {
                int in1 = seq1.get(seq1.length()-1-i);
                int in2 = seq2.get(seq2.length()-1-i);
                if (i == 0) {
                    // Use a half adder for the first bit, as there's no carry-in
                    halfAdder(formula, in1, in2, targetSum, currentCarry);
                } else {
                    // For subsequent bits, use a full adder with the carry from the previous stage
                    fullAdder(formula, in1, in2, previousCarry, targetSum, currentCarry);
                }
            } else {
                // If one of the inputs is missing (i.e., zero), use a half adder
                int input = i < seq1.length() ? seq1.get(seq1.length()-1-i) : seq2.get(seq2.length()-1-i);
                halfAdder(formula, input, previousCarry, targetSum, currentCarry);
            }
            // Update the carry for the next iteration
            previousCarry = currentCarry;
        }

        return sum;
    }

    public static VariableSequence subtract(Formula formula, VariableSequence seq1, VariableSequence seq2) {
        int n = Math.max(seq1.length(), seq2.length());

        // Pad seq1 to n bits and then further pad it to n+1 bits (with extra false at front).
        VariableSequence paddedA = padToLength(formula, seq1, n);
        paddedA = padToLength(formula, paddedA, n+1); // Prepend an extra 0.

        // Pad seq2 to n bits.
        VariableSequence paddedB = padToLength(formula, seq2, n);

        // Our unknown result (C) will be n bits.
        VariableSequence result = formula.newVariables(n);

        // Compute B + result; add() returns n+1 bits.
        VariableSequence temp = add(formula, result, paddedB);

        // Constrain that B + result equals paddedA.
        for (int i = 0; i < paddedA.length(); i++) {
            eq(formula, temp.get(i), paddedA.get(i));
        }

        return result;
    }

    public static void isSquare(Formula formula, VariableSequence sequence) {

        // 'sequence' must be a perfect square:
        int sqrtLen = (sequence.length() + 1) / 2;
        VariableSequence sqrtCandidate = formula.newVariables(sqrtLen);
        VariableSequence squareComputed = Arithmetic.mul(formula, sqrtCandidate, sqrtCandidate);

        // Enforce that squareComputed equals sequence bitwise:
        Logic.equals(formula, squareComputed, sequence);
    }


    /**
     * Enforces logical equivalence between two variables.
     */
    private static void eq(Formula formula, int var1, int var2) {
        // var1 == var2 can be enforced with two clauses:
        formula.add("-" + var1 + " " + var2 + " 0");
        formula.add(var1 + " -" + var2 + " 0");
    }


    public static VariableSequence mul(Formula formula, VariableSequence in1, VariableSequence in2) {

        // Swap if the second is larger:
        if (in2.length() > in1.length()) {
            return mul(formula, in2, in1);
        }

        // Location of final result:
        List<Integer> outputVariables = new ArrayList<>();

        VariableSequence previousPartialSum = null;
        for(int j = 0; j < in2.length(); j++) {

            VariableSequence partialSum = formula.newVariables(in1.length());
            for (int i = 0; i < in1.length(); i++) {
                Gates.and(formula, in2.get(in2.length() - 1 - j), in1.get(i), partialSum.get(i));
            }

            if(previousPartialSum != null) {
                // Add with last step
                partialSum = add(formula, previousPartialSum.range(0, previousPartialSum.length() - 1), partialSum);
            }

            // Output last digit (it won't change after this ripple)
            outputVariables.add(0, partialSum.get(partialSum.length() - 1));
            previousPartialSum = partialSum;
        }

        // No further partial sums, output everything:
        outputVariables.addAll(0, previousPartialSum.range(0, previousPartialSum.length() - 1).variables());

        return new VariableSequence(outputVariables);
    }

    public static void lessThan(boolean allowEqual, Formula formula, VariableSequence seq1, VariableSequence seq2) {
        int n = Math.max(seq1.length(), seq2.length());
        VariableSequence a = Helper.padToLength(formula, seq1, n);
        VariableSequence b = Helper.padToLength(formula, seq2, n);

        VariableSequence allEqualUpTo = formula.newVariables(n+1);
        formula.add(allEqualUpTo.get(0) + " 0");

        VariableSequence equalPairwise = formula.newVariables(n);

        for(int i = 0; i < n; i++) {

            // Compare the direct pairs:
            formula.add(equalPairwise.get(i) + " " + a.get(i) + " " + b.get(i) + " 0");
            formula.add(equalPairwise.get(i) + " -" + a.get(i) + " -" + b.get(i) + " 0");
            formula.add("-" + equalPairwise.get(i) + " -" + a.get(i) + " " + b.get(i) + " 0");
            formula.add("-" + equalPairwise.get(i) + " " + a.get(i) + " -" + b.get(i) + " 0");

            // AND together the single previous and the pair:
            Gates.and(formula, allEqualUpTo.get(i), equalPairwise.get(i), allEqualUpTo.get(i+1));

            // Finally forbid that the previous as still all equal and the current is 1<0
            formula.add("-" + allEqualUpTo.get(i) + " -" + a.get(i) + " " + b.get(i) + " 0");
        }

        if(!allowEqual) {
            formula.add("-" + allEqualUpTo.get(n-1) + " 0");
        }
    }


}
