package com.royvanrijn.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Counting;

public class TestClauseCounting {

    private Formula formula = Formula.create();

    public static void main(String[] args) {
        new TestClauseCounting().run();
    }

    private void run() {

        // Create the circuit:
        VariableSequence seq1 = formula.newVariables(5);
        VariableSequence seq2 = formula.newVariables(5);

        Counting.combinedK(formula, seq1, seq2, 5);

        formula.add("-4 0");
        formula.add("-5 0");
        formula.add("-6 0");

        System.out.println(seq1.variables());
        System.out.println(seq2.variables());

        formula.writeToFile("dimacs/example.cnf");
        runMinisat();

        String output = getMinisatOutput();

        System.out.println(output);
    }


    public String getMinisatOutput() {
        try {
            return Files.readAllLines(Path.of("output.cnf")).get(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void runMinisat() {
        try {
            // Command to run MiniSat
            String[] cmd = {"minisat", "dimacs/example.cnf", Path.of("output.cnf").toString()};

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // Merge stdout and stderr
            Process process = pb.start();

            // Capture the output
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
