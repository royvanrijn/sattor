package com.royvanrijn.hexomino;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Shapes {

    public static Collection<List<Point>> buildAllCoversNoWrap() {

        Collection<List<Point>> allCovers = new HashSet<>();

        // Created all rotations/directions
        Point[][] shapes = new Point[][] {
                {Point.of(0,0), Point.of(0,1), Point.of(0,2), Point.of(0,3), Point.of(1,3), Point.of(1,4)},
                {Point.of(1,0), Point.of(1,1), Point.of(1,2), Point.of(1,3), Point.of(0,3), Point.of(0,4)},
                {Point.of(0,0), Point.of(0,1), Point.of(1,1), Point.of(1,2), Point.of(1,3), Point.of(1,4)},
                {Point.of(1,0), Point.of(1,1), Point.of(0,1), Point.of(0,2), Point.of(0,3), Point.of(0,4)},

                {Point.of(0,0), Point.of(1,0), Point.of(2,0), Point.of(3,0), Point.of(3,1), Point.of(4,1)},
                {Point.of(0,1), Point.of(1,1), Point.of(2,1), Point.of(3,1), Point.of(3,0), Point.of(4,0)},
                {Point.of(0,0), Point.of(1,0), Point.of(1,1), Point.of(2,1), Point.of(3,1), Point.of(4,1)},
                {Point.of(0,1), Point.of(1,1), Point.of(1,0), Point.of(2,0), Point.of(3,0), Point.of(4,0)},
        };

        // For each starting position:
        for(int x = 0; x < Point.SIZE; x++) {
            for(int y = 0; y < Point.SIZE; y++) {

                // And each shape:
                for (Point[] shape : shapes) {

                    boolean isValid = true;

                    List<Point> placement = new ArrayList<>();
                    Point base = Point.of(x, y);
                    for(Point s : shape) {
                        if(x + s.x() >= Point.SIZE) isValid = false;
                        if(y + s.y() >= Point.SIZE) isValid = false;
                        placement.add(base.from(s));
                    }

                    if(isValid) {
                        Collections.sort(placement);
                        allCovers.add(placement);
                    }

                }
            }

        }

        allCovers = getSortedCovers(allCovers);

        System.out.println("Found in total:" + allCovers.size());

        return allCovers;


    }


    public static Collection<List<Point>> buildAllCovers() {

        Collection<List<Point>> allCovers = new HashSet<>();

        // Created all rotations/directions
        Point[][] shapes = new Point[][] {
                {Point.of(0,0), Point.of(0,1), Point.of(0,2), Point.of(0,3), Point.of(1,3), Point.of(1,4)},
                {Point.of(0,0), Point.of(0,1), Point.of(0,2), Point.of(0,3), Point.of(-1,3), Point.of(-1,4)},
                {Point.of(0,0), Point.of(0,1), Point.of(1,1), Point.of(1,2), Point.of(1,3), Point.of(1,4)},
                {Point.of(0,0), Point.of(0,1), Point.of(-1,1), Point.of(-1,2), Point.of(-1,3), Point.of(-1,4)},

                {Point.of(0,0), Point.of(1,0), Point.of(2,0), Point.of(3,0), Point.of(3,1), Point.of(4,1)},
                {Point.of(0,0), Point.of(1,0), Point.of(2,0), Point.of(3,0), Point.of(3,-1), Point.of(4,-1)},
                {Point.of(0,0), Point.of(1,0), Point.of(1,1), Point.of(2,1), Point.of(3,1), Point.of(4,1)},
                {Point.of(0,0), Point.of(1,0), Point.of(1,-1), Point.of(2,-1), Point.of(3,-1), Point.of(4,-1)},
        };

        // For each starting position:
        for(int x = 0; x < Point.SIZE; x++) {
            for(int y = 0; y < Point.SIZE; y++) {

                // And each shape:
                for (Point[] shape : shapes) {

                    List<Point> placement = new ArrayList<>();
                    Point base = Point.of(x, y);
                    for(Point s : shape) {
                        placement.add(base.from(s));
                    }

                    Collections.sort(placement);

                    allCovers.add(placement);

                }
            }

        }

        allCovers = getSortedCovers(allCovers);

        System.out.println("Found in total:" + allCovers.size());

        return allCovers;


    }

    private static Collection<List<Point>> getSortedCovers(final Collection<List<Point>> allCovers) {
        List<List<Point>> uniqCovers = new ArrayList<>();
        uniqCovers.addAll(allCovers);

        uniqCovers.sort((list1, list2) -> {
            for (int i = 0; i < Math.min(list1.size(), list2.size()); i++) {
                int cmp = list1.get(i).compareTo(list2.get(i));
                if (cmp != 0) {
                    return cmp; // If there's a difference, return it.
                }
            }
            // If all compared points are equal, compare based on size.
            return Integer.compare(list1.size(), list2.size());
        });

        return uniqCovers;
    }

    public static void printShape(List<Point> cover) {
        boolean[][] lines = new boolean[Point.SIZE][Point.SIZE];
        for(Point p : cover) {
            lines[p.x()][p.y()] = true;
        }

        for(boolean[] a : lines) {
            System.out.println(Arrays.toString(a));
        }
        System.out.println("---");
    }
}
