package com.royvanrijn.examples;

import java.util.EnumSet;

/**
 * Small example that uses {@link BitwiseFunctionSynthesizer} to search for a
 * program that reverses the bits of an 8‑bit input.
 */
public class SynthesizeReverseBits {

    /** Compute the reversed bit pattern of an 8-bit value. */
    private static int reverse8(int x) {
        return Integer.reverse(x) >>> 24;
    }

    /** Compute a transformation of an 8-bit value. */
    private static int fun(int x) {
        int transformed = (x + 1) & 0xFF;
//        System.out.println(x + " " + transformed);
        return transformed;
    }

    public static void main(String[] args) {

        // Create file:
        BitwiseFunctionSynthesizer.synthesize(
                SynthesizeReverseBits::fun,
                16,
                EnumSet.allOf(BitwiseFunctionSynthesizer.OpType.class));

        // After running minisat: UNSATISFIABLE
    }
}
