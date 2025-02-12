package com.royvanrijn.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Arithmetic;

public class TestBinarySubtraction {

    private Formula formula = Formula.create();

    public static void main(String[] args) {
        new TestBinarySubtraction().run();
    }


    Map<VariableSequence, String> sequencesToTest = new HashMap<>();

    private void run() {

        for(int i = 2; i < 10; i++) {
            for(int j = 1; j < i-1; j++) {
                //Trying all options:
                encode(Integer.toBinaryString(i), Integer.toBinaryString(j), Integer.toBinaryString(i-j));
            }
        }

        formula.writeToFile("dimacs/example.cnf");
        runMinisat();

        String minisatOutput = getMinisatOutput();

        checkVariables(minisatOutput, sequencesToTest);

    }

    private void checkVariables(final String minisatOutput, Map<VariableSequence, String> sequences) {
        for(VariableSequence encodedSequence : sequences.keySet()) {
            String actualOutput = encodedSequence.variables().stream().map(i-> minisatOutput.charAt(minisatOutput.indexOf(i+" ")-1)=='-'?"0":"1").collect(Collectors.joining(""));

            String sum = sequences.get(encodedSequence);
            String expectedOutput = sum.split(" ")[4];

            if(!(expectedOutput.equals(actualOutput))) {
                throw new IllegalArgumentException("WRONG! Expected: "+sum+" and got "+ actualOutput);
            } else {
                System.out.println("Correct: " + sum);
            }
        }
    }

    public void encode(String b1, String b2, String expectedOutput) {
        // Create the circuit:

        int len = Math.max(b1.length(), b2.length());
        System.out.println("Encoding: " + b1+" "+b2);
        VariableSequence in1 = formula.newVariables(b1.length());
        VariableSequence in2 = formula.newVariables(b2.length());

        VariableSequence result = Arithmetic.subtract(formula, in1, in2);

        for(int i = 0;i < b1.length(); i++) {
            formula.add((b1.charAt(i)=='0'?"-":"") + in1.get(i) + " 0");
        }
        for(int i = 0;i < b2.length(); i++) {
            formula.add((b2.charAt(i)=='0'?"-":"") + in2.get(i) + " 0");
        }

        String output = ("0000000000"+expectedOutput);
        output = output.substring(output.length()-len);

        sequencesToTest.put(result, b1+" - "+b2+" = "+ output);
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



