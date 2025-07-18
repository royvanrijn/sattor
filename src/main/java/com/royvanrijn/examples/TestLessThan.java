package com.royvanrijn.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Arithmetic;

public class TestLessThan {

    public static void main(String[] args) {
        new TestLessThan().run();
    }

    private Formula formula = Formula.create();

    private void run() {

        VariableSequence in1 = formula.newVariables(4);
        VariableSequence in2 = formula.newVariables(4);

        //0011
        formula.add(in1.get(0) + " 0");
        formula.add("-" + in1.get(1) + " 0");
        formula.add("-" + in1.get(2) + " 0");
        formula.add(in1.get(3) + " 0");

        //1011
//        formula.add(in2.get(0) + " 0");
//        formula.add("-" + in2.get(1) + " 0");
//        formula.add("-" + in2.get(2) + " 0");
//        formula.add(in2.get(3) + " 0");

        Arithmetic.lessThan(true, formula, in1, in2);

        String file = "dimacs/example.cnf";
        formula.writeToFile(file);
        runMinisat(file);

        List<String> minisatOutput = getMinisatOutput();

        if (minisatOutput.size() > 1) {
            // Turn the result of Minisat, a line of variable assignments, into a magic square.
            String varLine = minisatOutput.get(1);

//            Map<VariableSequence, String> sequenceToTest = Map.of(in1, "0011", in2, "1011");
//            checkVariables(varLine, sequenceToTest);

            System.out.println(minisatOutput);

        } else {
            System.out.println("UNSAT");
        }
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



