package com.royvanrijn.sattor;

import java.util.List;

public class Matrix {

    private final int size;
    private final VariableSequence matrix;

    public Matrix(Formula formula, int n) {
        this.size = n;
        this.matrix = formula.newVariables(n*n);
    }

    public VariableSequence row(int i) {
        if(i >= size) throw new IllegalArgumentException("Out of bounds");

        return matrix.range(size*i, size*i + size);
    }

    public VariableSequence col(int i) {
        if (i < 0 || i >= size) {
            throw new IllegalArgumentException("Out of bounds");
        }
        VariableSequence column = new VariableSequence();
        for (int row = 0; row < size; row++) {
            column.add(matrix.get(row * size + i));
        }
        return column;
    }

    public List<Integer> variables() {
        return matrix.variables();
    }

    public int size() {
        return size;
    }
}
