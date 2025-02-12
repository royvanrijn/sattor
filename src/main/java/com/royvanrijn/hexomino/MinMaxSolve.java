package com.royvanrijn.hexomino;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MinMaxSolve {

    public static void main(String[] args) {
        new MinMaxSolve().solve();
    }

    private void solve() {

        // Build all the coverings as points:
        Collection<List<Point>> pointsOfShapes =  Shapes.buildAllCoversNoWrap();

        // Turn these into bitmaps, where the shape is highlighted with 1s
        final List<Long> bitmapShapes = generateShapeCoverBitmaps(pointsOfShapes);

        System.out.println("Generated: " + bitmapShapes.size() + " shapes");

        long boardMask = 0L;
        for(int x = 0; x < Point.SIZE; x++) {
            for(int y = 0; y < Point.SIZE; y++) {
                boardMask |= 1L << (x*8 + y);
            }
        }

        printBitmap(boardMask);

        long before = System.currentTimeMillis();
        // The bitmap boards are 8x8, they fit neatly in a single long:
        int result = alphaBeta(0L, 0L, -1, 1, 1, boardMask, bitmapShapes);

        System.out.println("After:" + (System.currentTimeMillis() - before));
        System.out.println(result);

    }

    private int alphaBeta(
            long currentBoard, long opponentBoard,
            int alpha, int beta,
            int multiplier, // +1 for maximizing, -1 for minimizing
            long boardMask, List<Long> bitmapShapes
    ) {
        // Check for a win/loss on the current board first
        int eval = evaluate(currentBoard, opponentBoard, bitmapShapes);
        if (eval != 0) {
            return eval * multiplier; // Return perspective-adjusted score
        }

        long empties = ~(currentBoard | opponentBoard) & boardMask;
        if (empties == 0) {
            return 0; // tie
        }

        int bestVal = -multiplier; // Start with worst possible value for this player
        long moves = empties;
        while (moves != 0) {
            long move = Long.lowestOneBit(moves);
            moves ^= move;

            // Place the piece on the current board, then swap roles
            int val = alphaBeta(
                    opponentBoard,                      // new "current"
                    currentBoard | move,               // new "opponent"
                    -beta, -alpha,                     // swap alpha and beta (negate for perspective)
                    -multiplier,                       // swap perspective
                    boardMask, bitmapShapes
            );

            if (multiplier == 1) {
                bestVal = Math.max(bestVal, val);
                alpha = Math.max(alpha, bestVal);
                if (alpha >= beta) break; // Beta cutoff
            } else {
                bestVal = Math.min(bestVal, val);
                beta = Math.min(beta, bestVal);
                if (alpha >= beta) break; // Alpha cutoff
            }
        }

        return bestVal * multiplier; // Return perspective-adjusted result
    }

    private int evaluate(long boardP1, long boardP2, List<Long> bitmapShapes) {
        // Check if Player 1 or Player 2 has a winning shape
        for (long shape : bitmapShapes) {
            if ((boardP1 & shape) == shape) return +1;  // P1 wins
            if ((boardP2 & shape) == shape) return -1;  // P2 wins
        }
        return 0;  // No winner
    }

    private List<Long> generateShapeCoverBitmaps(final Collection<List<Point>> pointsOfShapes) {
        List<Long> bitmapShapes = new ArrayList<>();
        for(List<Point> pointsOfShape : pointsOfShapes) {
            long coverBitMap = 0L;
            System.out.println(pointsOfShape);

            // Turn shape into bitmap:
            for(Point p : pointsOfShape) {
                coverBitMap |= 1L << p.x()*8 + p.y();
            }
            bitmapShapes.add(coverBitMap);
            printBitmap(coverBitMap);
        }
        return bitmapShapes;
    }

    private void printBitmap(final long coverBitMap) {
        for(int x = 0; x < 8; x++) {
            for(int y = 0; y < 8; y++) {
                int id = x*8 + y;
                System.out.print(" " + (coverBitMap>>id & 1L));
            }
            System.out.println();
        }
        System.out.println();
    }


}
