package com.royvanrijn.hexomino;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Counting;

public class Hexomino {


    public static void main(String[] args) throws Exception {
        new Hexomino().run();
    }

    private Formula formula = Formula.create();

    private void run() {


        Collection<List<Point>> allCovers = Shapes.buildAllCovers();

        int amountPositions = Point.SIZE * Point.SIZE;
        int amountGroups = amountPositions / 2;
        VariableSequence[] positionGroupVars = new VariableSequence[amountPositions];
        for(int i = 0; i < amountPositions; i++) {
            positionGroupVars[i] = formula.newVariables(amountGroups);
        }

        System.out.println("Defining "+amountGroups+" groups of exactly 2:");

        List<List<Integer>> groups = new ArrayList<>();
        for(int group = 0; group < amountGroups; group++) {

            List<Integer> groupVars = new ArrayList<>();
            for(int i = 0; i < amountPositions; i++) {
                if(i == amountPositions/2) continue;//skip mid
                groupVars.add(positionGroupVars[i].get(group));
            }
            groups.add(groupVars);
            Counting.exactlyK(formula, VariableSequence.of(groupVars), 2);
        }

        System.out.println("Adding constraints for placement:");

        // So... for group 1, get the covered point-ids in group 1
        // Force that at least two are true
        for(List<Point> cover : allCovers) {

            boolean hasMid = false;
            for(Point p : cover) {
                hasMid |= p.id() == amountPositions/2;
            }
            if(!hasMid)continue;

            Shapes.printShape(cover);
            for(List<Integer> groupVars : groups) {

                List<Integer> shapeInGroup = new ArrayList<>();
                for(Point covered : cover) {
                    if(covered.id() == amountPositions/2) continue;//skip mid
                    shapeInGroup.add(groupVars.get((covered.id() > amountPositions/2)?covered.id()-1:covered.id()));
                }
                Counting.atLeastK(formula, VariableSequence.of(shapeInGroup), 2);
            }
        }

        formula.print();
        formula.writeToFile("dimacs/groups.cnf");


    }

}
