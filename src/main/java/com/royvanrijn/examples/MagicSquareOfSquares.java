package com.royvanrijn.examples;

import static com.royvanrijn.sattor.library.Arithmetic.add;
import static com.royvanrijn.sattor.library.Arithmetic.subtract;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Arithmetic;
import com.royvanrijn.sattor.library.Logic;

public class MagicSquareOfSquares {

    private Formula formula = Formula.create();

    public static void main(String[] args) {

        for(int i = 10; i < 100; i++) {
            new MagicSquareOfSquares().run(i);
        }
    }

    private void run(int amountBits) {

        // Create the 9 cells for the magic square.
        List<VariableSequence> square = IntStream.range(0, 9)
                .mapToObj(i -> formula.newVariables(amountBits))
                .toList();

        for (int i = 0; i < 9; i++) {
            // The cell must be nonzero.
            Logic.notAllFalse(formula, square.get(i));

            // --- Witness function: enforce the cell is a perfect square ---
            Arithmetic.isSquare(formula, square.get(i));
        }

        // TODO: Currently checked up to 17, took: 8080ms
        // TODO: Currently checked up to 18, took: 16569ms
        // TODO: Currently checked up to 23, took: 14293985ms

        // All numbers must be distinct.
        for (int i = 0; i < 8; i++) {
            for (int j = i + 1; j < 9; j++) {
                Logic.notEquals(formula, square.get(i), square.get(j));
            }
        }

        // The magic sum is a little larger than the cell bit-length.
        VariableSequence magicSum = formula.newVariables(amountBits + 2);

        // --- Pivot constraints using three pivot points ---
        VariableSequence t1 = subtract(formula, magicSum, square.get(4));
        Logic.equals(formula, t1, add(formula, square.get(0), square.get(8)));
        Logic.equals(formula, t1, add(formula, square.get(2), square.get(6)));
        Logic.equals(formula, t1, add(formula, square.get(3), square.get(5)));
        Logic.equals(formula, t1, add(formula, square.get(1), square.get(7)));

        VariableSequence t2 = subtract(formula, magicSum, square.get(0));
        Logic.equals(formula, t2, add(formula, square.get(1), square.get(2)));
        Logic.equals(formula, t2, add(formula, square.get(3), square.get(6)));

        VariableSequence t3 = subtract(formula, magicSum, square.get(8));
        Logic.equals(formula, t3, add(formula, square.get(2), square.get(5)));
        Logic.equals(formula, t3, add(formula, square.get(6), square.get(7)));

        // --- Symmetry breaking ---
        // In a 3x3 magic square, the magic sum equals 3 times the center cell.
        VariableSequence tripleCenter = add(formula, square.get(4), add(formula, square.get(4), square.get(4)));
        Logic.equals(formula, magicSum, tripleCenter);

        String file = "dimacs/magic.cnf";
        formula.writeToFile(file);

        long before = System.currentTimeMillis();
        System.out.println("Starting run with bit length: " + amountBits);
        runMinisat(file);
        System.out.println("Running took: " + (System.currentTimeMillis() - before) + "ms");

        List<String> minisatOutput = getMinisatOutput();

        if (minisatOutput.size() > 1) {
            // Turn the result of Minisat, a line of variable assignments, into a magic square.
            String varLine = minisatOutput.get(1);
            List<Integer> magic = new ArrayList<>();
            for (int x = 0; x < 3; x++) {
                for (int y = 0; y < 3; y++) {
                    int index = x * 3 + y;
                    String number = square.get(index).variables().stream()
                            .map(i -> varLine.charAt(varLine.indexOf(i + " ") - 1) == '-' ? "0" : "1")
                            .collect(Collectors.joining(""));
                    System.out.print(number + " (" + Integer.parseInt(number, 2) + ") ");
                    magic.add(Integer.parseInt(number, 2));
                }
                System.out.println();
            }

            System.out.println("Row 1: " + (magic.get(0) + magic.get(1) + magic.get(2)));
            System.out.println("Row 2: " + (magic.get(3) + magic.get(4) + magic.get(5)));
            System.out.println("Row 3: " + (magic.get(6) + magic.get(7) + magic.get(8)));

            System.out.println("Col 1: " + (magic.get(0) + magic.get(3) + magic.get(6)));
            System.out.println("Col 2: " + (magic.get(1) + magic.get(4) + magic.get(7)));
            System.out.println("Col 3: " + (magic.get(2) + magic.get(5) + magic.get(8)));

            System.out.println("Dia 1: " + (magic.get(0) + magic.get(4) + magic.get(8)));
            System.out.println("Dia 2: " + (magic.get(2) + magic.get(4) + magic.get(6)));
        } else {
            System.out.println("UNSAT");
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
