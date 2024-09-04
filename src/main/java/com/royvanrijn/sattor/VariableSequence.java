package com.royvanrijn.sattor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapper around a list of Integers.
 *
 * Makes the entire thing a little more "complete", avoids confusion with List<Integer> for variables.
 */
public class VariableSequence implements Iterable<Integer> {

    private final List<Integer> variables = new ArrayList<>();

    public VariableSequence() {

    }

    public VariableSequence(List<Integer> sequence) {
        append(sequence);
    }

    public static VariableSequence of(List<Integer> sequence) {
        return new VariableSequence(sequence);
    }

    public void append(VariableSequence sequence) {
        variables.addAll(sequence.variables);
    }

    public void append(List<Integer> variableSequence) {
        variables.addAll(variableSequence);
    }

    public int length() {
        return variables.size();
    }

    public List<Integer> variables() {
        return Collections.unmodifiableList(variables);
    }

    public VariableSequence range(int from, int to) {
        return new VariableSequence(variables.subList(from, to));
    }

    public int get(int index) {
        return variables.get(index);
    }

    public void add(int variable) {
        variables.add(variable);
    }

    @Override
    public Iterator<Integer> iterator() {
        return variables.iterator();
    }
}
