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

public class TestBinaryAddition {

    private Formula formula = Formula.create();

    public static void main(String[] args) {
        new TestBinaryAddition().run();
    }


    Map<VariableSequence, String> expectedOutputs = new HashMap<>();

    private void run() {

        for(int i = 0; i < 80; i++) {
            for(int j = 0; j < 80; j++) {
                //Trying all options:
                encode(Integer.toBinaryString(i), Integer.toBinaryString(j), Integer.toBinaryString(i+j));
            }
        }

        //TODO For performance measurements (once working): Create one big SAT file with all calculations and check here.

        formula.writeToFile("dimacs/example.cnf");
        runMinisat();

        String minisatOutput = getMinisatOutput();

        for(VariableSequence sequenceToCheck : expectedOutputs.keySet()) {
            String actualOutput = sequenceToCheck.variables().stream().map(i->minisatOutput.charAt(minisatOutput.indexOf(i+" ")-1)=='-'?"0":"1").collect(Collectors.joining(""));

            String sum = expectedOutputs.get(sequenceToCheck);
            String expectedOutput = sum.split(" ")[4];

            if(!(expectedOutput.equals(actualOutput) || ("0"+expectedOutput).equals(actualOutput))) {
                throw new IllegalArgumentException("WRONG! Expected: "+sum+" and got "+ actualOutput);
            }
        }

    }

    //29696
    //78848

    public void encode(String b1, String b2, String expectedOutput) {
        // Create the circuit:
        VariableSequence in1 = formula.newVariables(b1.length());
        VariableSequence in2 = formula.newVariables(b2.length());

        VariableSequence sum = Arithmetic.add(formula, in1, in2);

        for(int i = 0;i < b1.length(); i++) {
            formula.add((b1.charAt(i)=='0'?"-":"") + in1.get(i) + " 0");
        }
        for(int i = 0;i < b2.length(); i++) {
            formula.add((b2.charAt(i)=='0'?"-":"") + in2.get(i) + " 0");
        }

        expectedOutputs.put(sum, b1+" + "+b2+" = "+expectedOutput);
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



