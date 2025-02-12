package com.royvanrijn.sattor.library;

import java.util.ArrayList;
import java.util.List;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;

public class Helper {

    /**
     * Pads the given sequence to have length n by prepending false (0) bits.
     */
    public static VariableSequence padToLength(Formula formula, VariableSequence seq, int n) {
        if (seq.length() >= n) return seq;
        List<Integer> padded = new ArrayList<>();
        int padCount = n - seq.length();
        for (int i = 0; i < padCount; i++) {
            int var = formula.newVariables(1).get(0);
            // Fix the variable to 0.
            formula.add("-" + var + " 0");
            padded.add(var);
        }
        padded.addAll(seq.variables());
        return new VariableSequence(padded);
    }
}
