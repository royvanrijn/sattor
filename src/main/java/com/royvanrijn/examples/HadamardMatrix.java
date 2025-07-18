package com.royvanrijn.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.Matrix;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Arithmetic;
import com.royvanrijn.sattor.library.Counting;
import com.royvanrijn.sattor.library.Logic;

public class HadamardMatrix {

    public static void main(String[] args) {
        new HadamardMatrix().construct(100);
/*
        int i = 0;
        int n = 12;
        int matched = 0;
        while( i < 1<<n) {

            if(Integer.bitCount(i)==n/2) {
                String bin = Integer.toBinaryString(i);
                while(bin.length() < n) bin = "0"+bin;
                System.out.println(bin +" "+i);
                matched++;

            }
            i++;
        }
        System.out.println(i+" "+matched);

*/
    }

    private Formula formula = Formula.create();

    /**
     * Encode the construction of a n x n Hadamard matrix.
     * This is a matrix with only +1 and -1 (true or false) values where every pair of distinct rows/columns has a dot product of zero.
     *
     * @param n
     */
    private void construct(final int n) {

        Matrix matrix = new Matrix(formula, n);

        Logic.allTrue(formula, matrix.row(0));
        Logic.allTrue(formula, matrix.col(0));

        for(int i = 1; i < n; i++) {
            // Remove symmetries:
            Arithmetic.lessThan(false, formula, matrix.col(i), matrix.col(i-1));
            Arithmetic.lessThan(false, formula, matrix.row(i), matrix.row(i-1));
            Counting.exactlyK(formula, matrix.row(i), n/2);
            Counting.exactlyK(formula, matrix.col(i), n/2);
        }

        for(int i = 1; i < n-1; i++) {
            for(int j = i+1; j < n; j++) {
                // Encode hadamard rule for:
//                halfMismatch(formula, matrix.row(i), matrix.row(j));
                // Encode hadamard rule for:
//                halfMismatch(formula, matrix.col(i), matrix.col(j));
            }
        }


        String file = "dimacs/example.cnf";
        formula.writeToFile(file);

        System.out.println("Written file.");
        runMinisat(file);

        List<String> minisatOutput = getMinisatOutput();

        if (minisatOutput.size() > 1) {
            // Turn the result of Minisat, a line of variable assignments, into a magic square.
            String varLine = minisatOutput.get(1);

            printMatrix(" " + varLine, matrix);

        } else {
            System.out.println("UNSAT");
        }
    }

    private void halfMismatch(final Formula formula, final VariableSequence s1, final VariableSequence s2) {

        int n = s1.length();
        VariableSequence areEqual = formula.newVariables(n);

        for(int i = 0; i < n; i++) {

            // Compare the direct pairs:
            formula.add(areEqual.get(i) + " " + s1.get(i) + " " + s2.get(i) + " 0");
            formula.add(areEqual.get(i) + " -" + s1.get(i) + " -" + s2.get(i) + " 0");
            // Complete:
            formula.add("-" + areEqual.get(i) + " -" + s1.get(i) + " " + s2.get(i) + " 0");
            formula.add("-" + areEqual.get(i) + " " + s1.get(i) + " -" + s2.get(i) + " 0");
        }

        int half = n/2;
        Counting.exactlyK(formula, areEqual, half);
    }

    private void printMatrix(final String minisatOutput, final Matrix matrix) {
        System.out.println(minisatOutput);
        System.out.println(matrix.variables());
        List<String> actualOutput = matrix.variables().stream().map(i-> minisatOutput.charAt(minisatOutput.indexOf(i+" ")-1)=='-'?"0":"1").collect(Collectors.toList());

        int n = matrix.size();
        for(int x = 0; x < n; x++) {
            for(int y = 0; y < n; y++) {
                System.out.print(actualOutput.get(n*x + y)+" ");
            }
            System.out.println();
        }
    }


    public List<String> getMinisatOutput() {
        try {
            return Files.readAllLines(Path.of("output.cnf"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void runMinisat(String file) {
        try {
            // Command to run MiniSat
            String[] cmd = {"minisat", file, Path.of("output.cnf").toString()};
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // Merge stdout and stderr
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
