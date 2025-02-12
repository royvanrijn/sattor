package com.royvanrijn.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Arithmetic;
import com.royvanrijn.sattor.library.Logic;

public class TestArithmetic {

    private Formula formula = Formula.create();

    public static void main(String[] args) {
        new TestArithmetic().run();
    }

    private void run() {


        VariableSequence in1 = formula.newVariables(10);

        // For each cell 'num' that must be a perfect square:
        int sqrtLen = (in1.length() + 1) / 2;
        VariableSequence sqrtCandidate = formula.newVariables(sqrtLen);
        VariableSequence squareComputed = Arithmetic.mul(formula, sqrtCandidate, sqrtCandidate);

        // Enforce that squareComputed equals num bitwise:
        Logic.equals(formula, squareComputed, in1);

        formula.add(in1.get(5) + " 0");
        formula.add(in1.get(3) + " 0");



        String file = "dimacs/example.cnf";
        formula.writeToFile(file);
        runMinisat(file);

        String minisatOutput = getMinisatOutput();

        Map<VariableSequence, String> sequenceToTest = Map.of(in1, "1001");

        checkVariables(minisatOutput, sequenceToTest);

        System.out.println(minisatOutput);
    }

    private void checkVariables(final String minisatOutput, Map<VariableSequence, String> sequences) {
        for(VariableSequence encodedSequence : sequences.keySet()) {
            String actualOutput = encodedSequence.variables().stream().map(i-> minisatOutput.charAt(minisatOutput.indexOf(i+" ")-1)=='-'?"0":"1").collect(Collectors.joining(""));

            String expectedOutput = sequences.get(encodedSequence);

            if(!(expectedOutput.equals(actualOutput))) {
                throw new IllegalArgumentException("WRONG! Expected: "+expectedOutput+" and got "+ actualOutput);
            } else {
                System.out.println("Correct: " + expectedOutput);
            }
        }
    }

    public String getMinisatOutput() {
        try {
            return Files.readAllLines(Path.of("output.cnf")).get(1);
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



