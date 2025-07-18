package com.royvanrijn.examples;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Logic;

/**
 * Utility to synthesize simple bitwise programs using SAT.
 *
 * The class builds a SAT encoding of a small program consisting of
 * {@code numOps} operations that must match the provided function {@code f}
 * on all N-bit inputs. The generated formula can be written to a DIMACS file
 * and solved with an external SAT solver.
 */
public class BitwiseFunctionSynthesizer {

    /** Supported operation types. */
    public enum OpType { AND, OR, XOR, NOT, SHL1, SHR1 }

    /** Representation of a single synthesized instruction. */
    public static class Instruction {
        public OpType type;
        public int srcA;
        public int srcB;

        @Override
        public String toString() {
            return type + " " + srcA + " " + srcB;
        }
    }

    /** Representation of a program as a list of instructions. */
    public static class Program {
        public final List<Instruction> instructions = new ArrayList<>();
        /** constIndex → N‑bit value **/
        public final Map<Integer,Integer> constants = new LinkedHashMap<>();

        /**
         * Render a synthesized Program as a single Java expression in binary form.
         *
         * @param amountBits width of the word (e.g. 4 or 8)
         * @return           a Java‐style expression string over `x`
         */
        public String programToExpression(int amountBits) {
            // mask for & operations, e.g. 0b1111 for 4 bits
            String mask = "0b" + "1".repeat(amountBits);

            // Build wires: wire[0] = "x", wire[1..] = constants
            List<String> wires = new ArrayList<>();
            wires.add("x");
            // constants in order 0..N‑1
            for (int c = 0; c < constants.size(); c++) {
                int val = constants.get(c);
                // binary literal with leading zeros up to amountBits
                String bits = Integer.toBinaryString(val & ((1<<amountBits)-1));
                bits = "0".repeat(amountBits - bits.length()) + bits;
                wires.add("0b" + bits);
            }
            // compute each op in turn
            for (Instruction ins : instructions) {
                String a = wires.get(ins.srcA);
                String b = wires.get(ins.srcB);
                String out;
                switch (ins.type) {
                    case AND  -> out = "(" + a + " & " + b + ")";
                    case OR   -> out = "(" + a + " | " + b + ")";
                    case XOR  -> out = "(" + a + " ^ " + b + ")";
//                    case NOT  -> out = "((~" + a + ") & " + mask + ")";
//                    case SHL1 -> out = "((" + a + " << 1) & " + mask + ")";
                    case NOT  -> out = "(~" + a + ")";
                    case SHL1 -> out = "(" + a + " << 1)";
                    case SHR1 -> out = "(" + a + " >>> 1)";
                    default   -> throw new IllegalStateException("Unknown op "+ins.type);
                }
                wires.add(out);
            }
            // the last wire is the program result
            return wires.get(wires.size() - 1);
        }

    }

    /**
     * Build the CNF and return a map of "selector name → SAT‑var number" so
     * you can later look up exactly which var encodes which bit.
     */
    public static Map<String,Integer> synthesize(
            int amountBits,
            Function<Long,Long> f,
            int numOps,
            int numConsts,
            String outputFile
    ) {
        Formula formula = Formula.create();
        List<OpType> opOrder = List.of(OpType.AND, OpType.OR, OpType.XOR,
                OpType.NOT, OpType.SHL1, OpType.SHR1);

        // allocate constant‑wires
        List<VariableSequence> constWires = new ArrayList<>();
        for (int c = 0; c < numConsts; c++) {
            constWires.add(formula.newVariables(amountBits));
        }

        // build selector vars
        class OpVars { VariableSequence type, srcA, srcB; }
        List<OpVars> opVars = new ArrayList<>();
        for (int i = 0; i < numOps; i++) {
            OpVars v = new OpVars();
            v.type = formula.newVariables(opOrder.size());
            Logic.exactlyOne(formula, v.type);
            int wires = 1 + numConsts + i;
            v.srcA = formula.newVariables(wires);
            Logic.exactlyOne(formula, v.srcA);
            v.srcB = formula.newVariables(wires);
            Logic.exactlyOne(formula, v.srcB);
            opVars.add(v);
        }

        // dump varMap
        Map<String,Integer> varMap = new LinkedHashMap<>();
        for (int c = 0; c < numConsts; c++) {
            for (int b = 0; b < amountBits; b++) {
                varMap.put("const" + c + ".bit" + b,
                        constWires.get(c).get(b));
            }
        }
        for (int i = 0; i < numOps; i++) {
            OpVars v = opVars.get(i);
            for (int t = 0; t < opOrder.size(); t++) {
                varMap.put("op" + i + ".type." + opOrder.get(t),
                        v.type.get(t));
            }
            for (int a = 0; a <= i + numConsts; a++) {
                varMap.put("op" + i + ".srcA." + a,
                        v.srcA.get(a));
            }
            for (int b = 0; b <= i + numConsts; b++) {
                varMap.put("op" + i + ".srcB." + b,
                        v.srcB.get(b));
            }
        }

        long maxValMask = (1L << amountBits) - 1;
        // simulate N‑bit inputs
        for (long x = 0; x < (1L << amountBits); x++) {
            VariableSequence in = formula.newVariables(amountBits);
            for (int b = 0; b < amountBits; b++) {
                boolean bit = ((x >> (amountBits - 1 - b)) & 1) == 1;
                formula.add((bit ? "" : "-") + in.get(b) + " 0");
            }

            List<VariableSequence> wires = new ArrayList<>();
            wires.add(in);
            wires.addAll(constWires);

            for (int i = 0; i < numOps; i++) {
                OpVars v = opVars.get(i);
                VariableSequence a = mux(formula, amountBits, wires, v.srcA);
                VariableSequence b = mux(formula, amountBits, wires, v.srcB);

                Map<OpType,VariableSequence> results = new EnumMap<>(OpType.class);
                for (int tIdx = 0; tIdx < opOrder.size(); tIdx++) {
                    OpType t = opOrder.get(tIdx);
                    results.put(t, apply(formula, amountBits, t, a, b, v.type.get(tIdx)));
                }
                wires.add(mux(formula, amountBits,
                        opOrder.stream().map(results::get).toList(),
                        v.type));
            }

            long expected = f.apply(x) & maxValMask;
            VariableSequence out = wires.get(wires.size() - 1);
            for (int b = 0; b < amountBits; b++) {
                boolean bit = ((expected >> (amountBits - 1 - b)) & 1) == 1;
                formula.add((bit ? "" : "-") + out.get(b) + " 0");
            }
        }

        formula.writeToFile(outputFile);
        return varMap;
    }

    private static VariableSequence apply(Formula formula,
                                          int amountBits,
                                          OpType opType,
                                          VariableSequence a,
                                          VariableSequence b,
                                          int guardBit) {
        VariableSequence out = formula.newVariables(amountBits);
        long mask = (1L << amountBits) - 1;
        for (int i = 0; i < amountBits; i++) {
            int ai = a.get(i), bi = b.get(i), oi = out.get(i);
            switch (opType) {
                case AND -> {
                    formula.add(-guardBit + " " + -ai + " " + -bi + " " + oi + " 0");
                    formula.add(-guardBit + " " + ai + " " + -oi + " 0");
                    formula.add(-guardBit + " " + bi + " " + -oi + " 0");
                }
                case OR -> {
                    formula.add(-guardBit + " " + ai + " " + bi + " " + -oi + " 0");
                    formula.add(-guardBit + " " + -ai + " " + oi + " 0");
                    formula.add(-guardBit + " " + -bi + " " + oi + " 0");
                }
                case XOR -> {
                    formula.add(-guardBit + " " + -ai + " " + -bi + " " + -oi + " 0");
                    formula.add(-guardBit + " " + ai + " " + bi + " " + -oi + " 0");
                    formula.add(-guardBit + " " + -ai + " " + bi + " " + oi + " 0");
                    formula.add(-guardBit + " " + ai + " " + -bi + " " + oi + " 0");
                }
                case NOT -> {
                    formula.add(-guardBit + " " + -ai + " " + -oi + " 0");
                    formula.add(-guardBit + " " + ai + " " + oi + " 0");
                }
                case SHL1 -> {
                    if (i == amountBits - 1) {
                        formula.add(-guardBit + " " + -oi + " 0");
                    } else {
                        formula.add(-guardBit + " " + -a.get(i + 1) + " " + oi + " 0");
                        formula.add(-guardBit + " " + a.get(i + 1) + " " + -oi + " 0");
                    }
                }
                case SHR1 -> {
                    if (i == 0) {
                        formula.add(-guardBit + " " + -oi + " 0");
                    } else {
                        formula.add(-guardBit + " " + -a.get(i - 1) + " " + oi + " 0");
                        formula.add(-guardBit + " " + a.get(i - 1) + " " + -oi + " 0");
                    }
                }
            }
        }
        return out;
    }

    private static VariableSequence mux(Formula formula,
                                        int amountBits,
                                        List<VariableSequence> options,
                                        VariableSequence selector) {
        VariableSequence out = formula.newVariables(amountBits);
        for (int b = 0; b < amountBits; b++) {
            int outVar = out.get(b);
            for (int i = 0; i < options.size(); i++) {
                int sel = selector.get(i);
                int in = options.get(i).get(b);
                formula.add(-sel + " " + -in + " " + outVar + " 0");
                formula.add(-sel + " " + in + " " + -outVar + " 0");
            }
        }
        return out;
    }

    public static Program extractProgram(Path solverOutput,
                                         int amountBits,
                                         int numOps,
                                         int numConsts,
                                         Map<String,Integer> varMap)
            throws IOException {
        String all = Files.readAllLines(solverOutput).stream()
                .filter(l -> !l.startsWith("c") && !l.startsWith("p") && !l.equals("SAT"))
                .map(l -> l.startsWith("v ") ? l.substring(2) : l)
                .collect(Collectors.joining(" "));
        Set<Integer> pos = Arrays.stream(all.trim().split("\\s+"))
                .map(Integer::parseInt)
                .filter(v -> v > 0)
                .collect(Collectors.toSet());

        Program program = new Program();

        // constants
        for (int c = 0; c < numConsts; c++) {
            int value = 0;
            for (int b = 0; b < amountBits; b++) {
                if (pos.contains(varMap.get("const" + c + ".bit" + b))) {
                    value |= (1 << (amountBits - 1 - b));
                }
            }
            program.constants.put(c, value);
        }

        List<OpType> opOrder = List.of(
                OpType.AND, OpType.OR, OpType.XOR,
                OpType.NOT, OpType.SHL1, OpType.SHR1
        );

        for (int i = 0; i < numOps; i++) {
            Instruction ins = new Instruction();
            for (OpType t : opOrder) {
                if (pos.contains(varMap.get("op" + i + ".type." + t))) {
                    ins.type = t;
                    break;
                }
            }
            for (int a = 0; a <= numConsts + i; a++) {
                if (pos.contains(varMap.get("op" + i + ".srcA." + a))) {
                    ins.srcA = a;
                    break;
                }
            }
            for (int b = 0; b <= numConsts + i; b++) {
                if (pos.contains(varMap.get("op" + i + ".srcB." + b))) {
                    ins.srcB = b;
                    break;
                }
            }
            program.instructions.add(ins);
        }

        return program;
    }

    static long runProgram(Program p, int amountBits, long x) {
        int numConsts = p.constants.size();
        int numOps    = p.instructions.size();
        int totalWires = 1 + numConsts + numOps;
        long mask = (1L << amountBits) - 1;

        long[] wires = new long[totalWires];
        wires[0] = x & mask;
        for (int c = 0; c < numConsts; c++) {
            wires[1 + c] = p.constants.get(c) & mask;
        }

        if (x == 0) {
            System.out.println("Synthesized constants:");
            p.constants.forEach((c,val) ->
                    System.out.printf("  const%d = 0x%X (%d)%n", c, val, val)
            );
            System.out.println();
        }

        for (int i = 0; i < numOps; i++) {
            Instruction ins = p.instructions.get(i);
            long a = wires[ins.srcA];
            long b = wires[ins.srcB];
            long res = switch (ins.type) {
                case AND  -> a & b;
                case OR   -> a | b;
                case XOR  -> a ^ b;
                case NOT  -> (~a) & mask;
                case SHL1 -> (a << 1) & mask;
                case SHR1 -> a >>> 1;
            };
            wires[1 + numConsts + i] = res;
        }

        return wires[totalWires - 1];
    }
}
