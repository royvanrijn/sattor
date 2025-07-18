package com.royvanrijn.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.stream.Collectors;

/**
 * Small example that uses {@link BitwiseFunctionSynthesizer} to search for a
 * program that reverses the bits of an N‑bit input.
 *
 * Examples of things found using this class:
 *
 * 4 bit gray code binary:
 * Started with: x ^ (x>>>1) ^ (x>>>2) ^ (x>>>3)
 * Found: (((x >>> 1) ^ x) ^ ((((x >>> 1) ^ x) >>> 1) >>> 1));  (runs about 7% faster)
 *
 * 4 bit nibble swap (0->1, 2->3)
 * Started with: ((x&0b1100)>>1)|((x&0b0011)<<1);
 * Found: (((x << 1) | (x >>> 1)) & 0b0110) (runs about 2% faster)
 *
 * 4 bit parity:
 * Started with: (Long.bitCount(x)&1);
 * Found: (((x ^ ((x >>> 1) >>> 1)) ^ ((x ^ ((x >>> 1) >>> 1)) >>> 1)) & 0b0001) (much slower, bitCount is intrinsic)
 *
 * Some random 4-bit conditional:
 * Start with: (x&1)==1 ? (x | 0b1010) : (x & 0b0101)
 * Found: (((((x & 0b1101) << 1) ^ (((x & 0b1101) << 1) << 1)) >>> 1) ^ ((((x & 0b1101) << 1) << 1) << 1)) (is slower on my machine)
 *
 * Lowest 4-bit set one bit:
 * Started with: Long.lowestOneBit(x)
 * Found: ((((x << 1) | x) ^ (((x << 1) | x) << 1)) & (((((x << 1) | x) << 1) << 1) ^ (((x << 1) | x) ^ (((x << 1) | x) << 1))))
 *
 */
public class SynthesizeReverseBits {

    static final int amountBits = 4;
    static final int numOps    = 8;
    static final int numConsts = 0;  // allow solver to pick two N‑bit constants

    /** Compute a transformation of an N-bit value. */
    private static long fun(long x) {

//        return Long.reverse(x) >>> 64-amountBits;
//        return ((x<<1)&0xF) | (x>>>3);

//        long b = x ^ (x >>> 1);
//        b ^= (b >>> 1);
//        b ^= (b >>> 2);
//        b ^= (b >>> 3);
//        return b;

        return Long.numberOfTrailingZeros(x)&0xF;
    }

    private static long generated(long x) {
        return ((((~(x | (x << 1))) & (((x | (x << 1)) >>> 1) >>> 1)) >>> 1) ^ ((~(x | (x << 1))) & (((x | (x << 1)) >>> 1) >>> 1)));
    }
  
    public static void main(String[] args) throws IOException {

        String constraintFile = "dimacs/bitwise_synth.cnf";
        String outputFile = "dimacs/output.cnf";

        // this will print constants once, then run all inputs
        for (long x = 0; x < 1L<<amountBits; x++) {
            System.out.println(Long.toBinaryString(x) + " -> expected: " + Long.toBinaryString(fun(x)));
        }

        var varMap = BitwiseFunctionSynthesizer.synthesize(
                amountBits,
                SynthesizeReverseBits::fun,
                numOps,
                numConsts,
                constraintFile
        );

        System.out.println("selector→var map = " + varMap);

        runMinisat(constraintFile, outputFile);

        // run your SAT solver over dimacs/bitwise_synth.cnf → output.cnf
        BitwiseFunctionSynthesizer.Program program = BitwiseFunctionSynthesizer.extractProgram(
                Path.of(outputFile),
                amountBits,
                numOps,
                numConsts,
                varMap
        );
        System.out.println("consts = " + program.constants.values().stream().map(Integer::toBinaryString).collect(Collectors.joining()));
        System.out.println("prog   = " + program.instructions);

        System.out.println(program.programToExpression(amountBits));

        // this will print constants once, then run all inputs
        for (long x = 0; x < 1L<<amountBits; x++) {
            long y = BitwiseFunctionSynthesizer.runProgram(program, amountBits, x);
            System.out.println(x + " -> expected: " + fun(x) + " got: " + y);
            if(y != (fun(x) & ((1L<<amountBits)-1))) {
                System.out.println("Error.");
            }
        }
        System.out.println("✓ all inputs mapped to x");

        long y = 0;
        long before = 0;
        before = System.currentTimeMillis();
        for(int times = 0; times < 10000; times++) {
            for (int x = 0; x < 1000000; x++) {
                y += fun(x);
            }
        }
        System.out.println("Took: " + (System.currentTimeMillis() - before) + "ms");
        before = System.currentTimeMillis();
        y = 0;
        for(int times = 0; times < 10000; times++) {
            for (int x = 0; x < 1000000; x++) {
                y += generated(x);
            }
        }
        System.out.println("Took: " + (System.currentTimeMillis() - before) + "ms");
        before = System.currentTimeMillis();
        y = 0;
        for(int times = 0; times < 10000; times++) {
            for (int x = 0; x < 1000000; x++) {
                y += fun(x);
            }
        }
        System.out.println("Took: " + (System.currentTimeMillis() - before) + "ms");
        before = System.currentTimeMillis();
        y = 0;
        for(int times = 0; times < 10000; times++) {
            for (int x = 0; x < 1000000; x++) {
                y += generated(x);
            }
        }
        System.out.println("Took: " + (System.currentTimeMillis() - before) + "ms");

    }

    public static void runMinisat(String constraintFile, String outputFile) {
        try {
            // Command to run MiniSat
            String[] cmd = {"minisat", constraintFile, Path.of(outputFile).toString()};

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // Merge stdout and stderr
            Process process = pb.start();

            // Capture the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("Process exited with code: " + exitCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
