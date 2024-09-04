package com.royvanrijn.examples;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Logic;

/**
 * Uses the SATTOR code to build
 */
public class SudokuSolver {

    public static void main(String[] args) throws Exception {
        new SudokuSolver().run();
    }

    private Formula formula = Formula.create();

    private void run() {

        VariableSequence[] boards = new VariableSequence[9];
        VariableSequence[] cells = new VariableSequence[9*9];
        VariableSequence[][] rows = new VariableSequence[9][9];
        VariableSequence[][] cols = new VariableSequence[9][9];
        VariableSequence[][] blocks = new VariableSequence[9][9];

        // Create objects:
        for(int i = 0; i < 9; i++) {
            //Create a board for each possible value 1-9 on a 9x9 grid:
            boards[i] = formula.newVariables(9*9);
            for(int v = 0; v < 9; v++) {
                rows[v][i] = new VariableSequence();
                cols[v][i] = new VariableSequence();
                blocks[v][i] = new VariableSequence();
            }
        }

        for(int i = 0; i < 9*9; i++) {
            cells[i] = new VariableSequence();
            for(int value = 0; value < 9; value++) {
                cells[i].add(boards[value].get(i));
            }
        }

        // Extract row, column variables:
        for(int row = 0; row < 9; row++) {
            for(int col = 0; col < 9; col++) {
                for(int value = 0; value < 9; value++) {

                    int variable = boards[value].get(9*row + col);

                    rows[row][value].add(variable);
                    cols[col][value].add(variable);

                    int block = 3*(row/3) + (col/3);
                    blocks[block][value].add(variable);
                }
            }
        }

        // Define Sudoku rules, for each value, we have max one at each row, col, block:
        for(int cell = 0; cell < 9*9;cell++) {
            Logic.exactlyOne(formula, cells[cell]);
        }
        for(int i = 0; i < 9; i++) {
            for(int v = 0; v < 9; v++) {
                Logic.exactlyOne(formula, rows[i][v]);
                Logic.exactlyOne(formula, cols[i][v]);
                Logic.exactlyOne(formula, blocks[i][v]);
            }
        }

        // Fill the numbers here:
        String input =
                ". 1 . . 7 . . . 5\n" +
                "6 . . . 5 . . 4 .\n" +
                ". 5 . . 3 . . . .\n" +
                ". . 8 . . 6 . . 2\n" +
                ". . . . . . . . 7\n" +
                ". . 9 1 . 3 . . .\n" +
                ". . . . . . . . .\n" +
                ". . 6 . . 4 1 . .\n" +
                "8 . . . . 5 3 . 4";


        // Process the string above:
        String[] lines = input.split("\n");
        for(int row = 0; row < 9; row++) {
            String[] line = lines[row].split(" ");
            for(int col = 0; col < 9; col++) {
                if(!line[col].equals(".")) {
                    int value = line[col].charAt(0) - '1';

                    int variable = boards[value].get(9*row + col);
                    System.out.println(row+" "+col+" "+value+" = "+variable);
                    // Set to true:
                    formula.add(variable + " 0");
                }
            }
        }

        formula.writeToFile("dimacs/example.cnf");


    }

}
