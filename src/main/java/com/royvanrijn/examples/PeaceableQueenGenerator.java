package com.royvanrijn.examples;

import java.util.ArrayList;
import java.util.List;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Counting;
import com.royvanrijn.sattor.library.Logic;

public class PeaceableQueenGenerator {

    public static void main(String[] args) throws Exception {
        new PeaceableQueenGenerator().run();
    }

    private Formula formula = Formula.create();

    private void run() {
        // Board NxN:
        int N = 12;
        // Amount of white and black queens we search for:
        int k = 21;
        int kagg = 21;

        // Peaceable coexisting armies of queens
        // Unknown yet:
        // 12 21-21 > 12 21-22?
        // 13 24-25 > 13 24-26?

        VariableSequence boardPlayer1 = formula.newVariables(N*N);
        VariableSequence boardPlayer2 = formula.newVariables(N*N);

        // Create some convenient groups for symmetry breaking:
        List<Integer> player1Top = new ArrayList<>();
        List<Integer> player1TopLeft = new ArrayList<>();
        List<Integer> player1TopRight = new ArrayList<>();
        List<Integer> player1Bottom = new ArrayList<>();
        List<Integer> player2Left = new ArrayList<>();
        List<Integer> player2Right = new ArrayList<>();

        fillGroups(N, boardPlayer1, boardPlayer2, player1Top, player1TopLeft, player1TopRight, player1Bottom, player2Left, player2Right);

        // No overlap between boolean pair, one or the other is true, not both:
        Logic.zeroOrOne(formula, boardPlayer1, boardPlayer2);

        // Break symmetries:
        Counting.atLeastK(formula, VariableSequence.of(player1TopLeft), k/4); // top left has at least k/4
        Counting.atMostK(formula, VariableSequence.of(player1Bottom), k/2); // bottom has as most k/2 (never more, there go into the top)

        Counting.combinedK(formula, VariableSequence.of(player1Top), VariableSequence.of(player1Bottom), k);
        Counting.combinedK(formula, VariableSequence.of(player2Left), VariableSequence.of(player2Right), kagg);


        // no overlap in columns and rows, if one player claims it, done.
        for(int x = 0; x < N; x++) {
            int rowVar1 = formula.tempVariable();
            int rowVar2 = formula.tempVariable();
            int colVar1 = formula.tempVariable();
            int colVar2 = formula.tempVariable();
            for(int i = 0; i < N; i++) {
                formula.add(-boardPlayer1.get((N*x)+i) + " " + rowVar1 + " 0");
                formula.add(-boardPlayer2.get((N*x)+i) + " " + rowVar2 + " 0");
                formula.add(-boardPlayer1.get((N*i)+x) + " " + colVar1 + " 0");
                formula.add(-boardPlayer2.get((N*i)+x) + " " + colVar2 + " 0");
            }
            formula.add(-rowVar1+" "+-rowVar2+" 0");
            formula.add(-colVar1+" "+-colVar2+" 0");
        }

        // add rules for diagonals
        for(int x = 0; x < N-1; x++) {
            int dia1Var1 = formula.tempVariable();
            int dia1Var2 = formula.tempVariable();
            int dia2Var1 = formula.tempVariable();
            int dia2Var2 = formula.tempVariable();

            for(int step = 0; step < N-x; step++) {
                formula.add(-boardPlayer1.get((N*(x+step))+step) + " " + dia1Var1 + " 0");
                formula.add(-boardPlayer2.get((N*(x+step))+step) + " " + dia1Var2 + " 0");
                formula.add(-boardPlayer1.get((N*(N-1-x-step))+step) + " " + dia2Var1 + " 0");
                formula.add(-boardPlayer2.get((N*(N-1-x-step))+step) + " " + dia2Var2 + " 0");
            }

            formula.add(-dia1Var1+" "+-dia1Var2+" 0");
            formula.add(-dia2Var1+" "+-dia2Var2+" 0");

            if(x > 0) {
                int dia3Var1 = formula.tempVariable();
                int dia3Var2 = formula.tempVariable();
                int dia4Var1 = formula.tempVariable();
                int dia4Var2 = formula.tempVariable();
                for(int step = 0; step < N-x; step++) {
                    formula.add(-boardPlayer1.get((N * step) + step + x) + " " + dia3Var1 + " 0");
                    formula.add(-boardPlayer2.get((N * step) + step + x) + " " + dia3Var2 + " 0");
                    formula.add(-boardPlayer1.get((N * (N-1-step))+step+x) + " " + dia4Var1 + " 0");
                    formula.add(-boardPlayer2.get((N * (N-1-step))+step+x) + " " + dia4Var2 + " 0");
                }
                formula.add(-dia3Var1+" "+-dia3Var2+" 0");
                formula.add(-dia4Var1+" "+-dia4Var2+" 0");

            }
        }

        formula.writeToFile("dimacs/example.cnf");
    }

    private static void fillGroups(final int N, final VariableSequence boardPlayer1, final VariableSequence boardPlayer2, final List<Integer> player1Top, final List<Integer> player1TopLeft, final List<Integer> player1TopRight, final List<Integer> player1Bottom, final List<Integer> player2Left, final List<Integer> player2Right) {
        for(int i = 0; i < N; i++) {
            for(int j = 0; j < N; j++) {

                int index = j+ N *i;

                int p1 = boardPlayer1.get(index);
                int p2 = boardPlayer2.get(index);

                if((i<= N /2)) {
                    player1Top.add(p1);
                    if((j<= N /2)) {
                        player1TopLeft.add(p1);
                    } else {
                        player1TopRight.add(p1);
                    }
                } else {
                    player1Bottom.add(p1);
                }
                if((j<= N /2)) {
                    player2Left.add(p2);
                } else {
                    player2Right.add(p2);
                }
            }
        }
    }

}
