package com.royvanrijn.hexomino;

public record Point(int x, int y) implements Comparable<Point> {

    public static final int SIZE = 4;

    static Point of(int inX, int inY) {
        return new Point((SIZE + inX) % SIZE, (SIZE + inY) % SIZE);
    }

    Point from(Point add) {
        return new Point((SIZE + (x + add.x)) % SIZE, (SIZE + (y + add.y)) % SIZE);
    }

    Point from(int addX, int addY) {
        return new Point((SIZE + (x + addX)) % SIZE, (SIZE + (y + addY)) % SIZE);
    }

    int id() {
        return x * SIZE + y;
    }

    @Override
    public int compareTo(final Point o) {
        if(x == o.x) {
            return y - o.y;
        }
        return x - o.x;
    }
}
