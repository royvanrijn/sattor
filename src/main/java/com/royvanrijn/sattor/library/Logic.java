package com.royvanrijn.sattor.library;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;

/**
 * When encoding problems you often encounter the same kind of logic, I'm placing those here so I'm able to re-use it.
 *
 */
public class Logic {

    public static void noOverlap(final Formula formula, VariableSequence seq1, VariableSequence seq2) {
        if(seq1.length() != seq2.length()) {
            throw new IllegalArgumentException("Not same size");
        }

        for(int i = 0; i < seq1.length(); i++) {
            formula.add(-seq1.get(i) +" " + -seq2.get(i)+" 0");
        }
    }
}
