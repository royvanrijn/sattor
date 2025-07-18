package com.royvanrijn.examples;

import java.math.BigInteger;

public class BalancedBits {

    public static void main(String[] args) {
        int n = 100; // Large bit count, must be even
        generateBalancedNumbers(n);
    }

    public static void generateBalancedNumbers(int n) {
        int ones = n / 2;  // Number of ones in the bitmask

        // Smallest BigInteger with `ones` ones (e.g., "000111" for n=6)
        BigInteger bitmask = BigInteger.ZERO.setBit(ones).subtract(BigInteger.ONE);

        BigInteger limit = BigInteger.ONE.shiftLeft(n); // 2^n (upper bound)

        while (bitmask.compareTo(limit) < 0) {

            System.out.println(bitmask.toString(2));
            bitmask = nextCombination(bitmask);
        }
    }

    // Gets the next lexicographical bitmask with the same number of ones
    private static BigInteger nextCombination(BigInteger x) {
        BigInteger smallest = x.and(x.negate());  // Rightmost set bit
        BigInteger ripple = x.add(smallest);      // Add it to x
        BigInteger newSmallest = ripple.and(ripple.negate());
        return ripple.or(newSmallest.divide(smallest).shiftRight(1).subtract(BigInteger.ONE));
    }
}
