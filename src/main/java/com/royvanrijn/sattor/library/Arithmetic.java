package com.royvanrijn.sattor.library;

import static com.royvanrijn.sattor.library.Gates.xor;

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
}
