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

    public static void main(String[] args) {
        BitwiseFunctionSynthesizer.synthesize(
                SynthesizeReverseBits::reverse8,
                8,
                EnumSet.allOf(BitwiseFunctionSynthesizer.OpType.class));
    }
}
