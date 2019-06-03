package com.royvanrijn.boolish;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class PeaceableQueenGenerator {

    public static void main(String[] args) throws Exception {
        new PeaceableQueenGenerator().run();
    }

    private PrintWriter writer;

    PeaceableQueenGenerator() {
        try {
            writer = new PrintWriter(new FileWriter("dimacs/peaceablequeens.cnf"));
        } catch (IOException e) {
            writer = null;
        }
    }

    private void run() throws Exception {
        // Board NxN:
        int N = 10;
        // Amount of white and black queens we search for:
        int k = 14;

        int[] boardP1 = new int[N*N];
        int[] boardP2 = new int[N*N];

        int nextFreeVariable = 1;
        // Reserve variables that represents the board:
        for(int i = 0; i<boardP1.length;i++) boardP1[i] = nextFreeVariable++;
        for(int i = 0; i<boardP2.length;i++) boardP2[i] = nextFreeVariable++;

        // Make sure we have at least k pieces in board 1:
        nextFreeVariable = ltSeq(boardP1, N*N - k, nextFreeVariable, false);
        nextFreeVariable = ltSeq(boardP2, N*N - k, nextFreeVariable, false);

        // Make sure board 1 and board 2 pieces do not overlap:
        for(int i = 0; i < boardP1.length; i++) {
            writer.println(-boardP1[i] +" " + -boardP2[i]+" 0");
        }

        // Add rules for rows
        // and cols
        for(int x = 0; x < N; x++) {
            int rowVar1 = nextFreeVariable++;
            int rowVar2 = nextFreeVariable++;
            int colVar1 = nextFreeVariable++;
            int colVar2 = nextFreeVariable++;
            for(int i = 0; i < N; i++) {
                writer.println(-boardP1[(N*x)+i]+" "+ rowVar1+" 0");
                writer.println(-boardP2[(N*x)+i]+" "+ rowVar2+" 0");
                writer.println(-boardP1[(N*i)+x]+" "+ colVar1+" 0");
                writer.println(-boardP2[(N*i)+x]+" "+ colVar2+" 0");
            }
            writer.println(-rowVar1+" "+-rowVar2+" 0");
            writer.println(-colVar1+" "+-colVar2+" 0");
        }

        // Add rules for diagonals
        for(int x = 0; x < N-1; x++) {
            int dia1Var1 = nextFreeVariable++;
            int dia1Var2 = nextFreeVariable++;
            int dia2Var1 = nextFreeVariable++;
            int dia2Var2 = nextFreeVariable++;

            for(int step = 0; step < N-x; step++) {
                writer.println(-boardP1[(N*(x+step))+step] + " " + dia1Var1 + " 0");
                writer.println(-boardP2[(N*(x+step))+step] + " " + dia1Var2 + " 0");
                writer.println(-boardP1[(N*(N-1-x-step))+step] + " " + dia2Var1 + " 0");
                writer.println(-boardP2[(N*(N-1-x-step))+step] + " " + dia2Var2 + " 0");
            }

            writer.println(-dia1Var1+" "+-dia1Var2+" 0");
            writer.println(-dia2Var1+" "+-dia2Var2+" 0");


            if(x > 0) {
                int dia3Var1 = nextFreeVariable++;
                int dia3Var2 = nextFreeVariable++;
                int dia4Var1 = nextFreeVariable++;
                int dia4Var2 = nextFreeVariable++;
                for(int step = 0; step < N-x; step++) {
                    writer.println(-boardP1[(N * step) + step + x] + " " + dia3Var1 + " 0");
                    writer.println(-boardP2[(N * step) + step + x] + " " + dia3Var2 + " 0");
                    writer.println(-boardP1[(N * (N-1-step))+step+x] + " " + dia4Var1 + " 0");
                    writer.println(-boardP2[(N * (N-1-step))+step+x] + " " + dia4Var2 + " 0");
                }
                writer.println(-dia3Var1+" "+-dia3Var2+" 0");
                writer.println(-dia4Var1+" "+-dia4Var2+" 0");

            }
        }

        writer.flush();
        writer.close();
    }

    /**
     * LTseq from: http://www.cs.toronto.edu/~fbacchus/csc2512/at_most_k.pdf
     */
    private int ltSeq(final int[] x, final int k, int nextFreeVariable, boolean state) {

        // Build registry table:
        final int[][] s = new int[x.length-1][k];
        for(int i = 0; i < x.length-1; i++) {
            for(int j = 0; j < k; j++) {
                s[i][j] = nextFreeVariable++;
            }
        }

        //(¬x1 ∨ s1,1)
        writer.println((state?-1:1)*x[0]+" "+s[0][0]+" 0");
        for(int j = 1; j<k;j++) {
            //(¬s1,j )
            writer.println(-s[0][j]+" 0");
        }
        for(int i = 1; i < x.length - 1; i++) {
            //(¬xi ∨ si,1)
            //(¬si−1,1 ∨ si,1)
            writer.println((state?-1:1)*x[i]+" "+s[i][0]+" 0");
            writer.println(-s[i-1][0]+" "+s[i][0]+" 0");
            for(int j = 1; j<k;j++) {
                //(¬xi ∨ ¬si−1,j−1 ∨ si,j )
                //(¬si−1,j ∨ si,j )
                writer.println((state?-1:1)*x[i]+" " + -s[i-1][j-1]+" "+s[i][j]+" 0");
                writer.println(-s[i-1][j]+" "+s[i][j]+" 0");
            }
            //¬xi ∨ ¬si−1,k)
            writer.println((state?-1:1)*x[i]+" "+-s[i-1][k-1]+" 0");
        }
        //(¬xn ∨ ¬sn−1,k)
        writer.println((state?-1:1)*x[x.length-1]+" "+-s[x.length-2][k-1]+" 0");
        return nextFreeVariable;
    }
}
