package com.royvanrijn.sattor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * The classes in this package can be used to encode problems into SAT.
 *
 * Start with a Formula (Formula.create()).
 * Next you can use the static methods in Arithmetic/Counting/Gates/Logic to build circuits.
 *
 * Next a DIMACS file can be created, which can be checked using tools like MiniSAT/Glucose/etc.
 */
public class Formula {

    private List<String> clauses = new ArrayList<>();

    private int variableIdGenerator = 1;

    private Formula() {}

    public static Formula create() {
        return new Formula();
    }

    public VariableSequence newVariables(int amount) {

        if(amount < 1) {
            throw new IllegalArgumentException("No valid input: " + amount);
        }

        return new VariableSequence(IntStream.range(variableIdGenerator, variableIdGenerator += amount).mapToObj(i->i).toList());
    }

    public int tempVariable() {
        return variableIdGenerator++;
    }

    public Formula add(String clause) {
        this.clauses.add(clause);
        return this;
    }

    public Formula print() {
        for(String clause : clauses) {
            System.out.println(clause);
        }
        return this;
    }

    public void writeToFile(final String outputFile) {
        try {
            Files.write(Path.of(outputFile), clauses);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
