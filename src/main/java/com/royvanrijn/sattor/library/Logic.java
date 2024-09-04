package com.royvanrijn.sattor.library;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;

/**
 * When encoding problems you often encounter the same kind of logic, I'm placing those here so I'm able to re-use it.
 *
 */
public class Logic {

    /**
     * Given a sequence, only one can be true.
     * @param formula
     */
    public static void exactlyOne(final Formula formula, VariableSequence seq) {

        // On of these must be true:
        String clause = "";
        for(int variable : seq.variables()) {
            clause += variable + " ";
        }
        formula.add(clause + "0");

        // And pairwise, both can't be true:
        for(int s1 = 0; s1 < seq.length() - 1; s1++) {
            for(int s2 = s1+1; s2 < seq.length(); s2++) {
                formula.add(-seq.get(s1) +" " + -seq.get(s2)+" 0");
            }
        }
    }

    /**
     * Given two sequences, for each index, only one or zero of the sequences can be true.
     * @param formula
     * @param seq1
     * @param seq2
     */
    public static void zeroOrOne(final Formula formula, VariableSequence seq1, VariableSequence seq2) {
        if(seq1.length() != seq2.length()) {
            throw new IllegalArgumentException("Not same size");
        }

        for(int i = 0; i < seq1.length(); i++) {
            formula.add(-seq1.get(i) +" " + -seq2.get(i)+" 0");
        }
    }
}
