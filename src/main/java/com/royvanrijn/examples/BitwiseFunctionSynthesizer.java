package com.royvanrijn.examples;

import java.util.*;
import java.util.function.Function;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Gates;
import com.royvanrijn.sattor.library.Logic;

/**
 * Utility to synthesize simple bitwise programs using SAT.
 *
 * The class builds a SAT encoding of a small program consisting of
 * {@code numOps} operations that must match the provided function {@code f}
 * on all 8-bit inputs. The generated formula can be written to a DIMACS file
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
    }

    /** Representation of a program as a list of instructions. */
    public static class Program {
        public final List<Instruction> instructions = new ArrayList<>();
    }

    /**
     * Build a SAT instance that searches for a program implementing {@code f}.
     *
     * @param f          Function to realize for all 8-bit inputs
     * @param numOps     Number of operations in the synthesized program
     * @param allowedOps Set of allowed op types
     * @return optional program or empty if UNSAT after running a solver
     */
    public static Optional<Program> synthesize(Function<Integer, Integer> f,
                                               int numOps,
                                               Set<OpType> allowedOps) {
        Formula formula = Formula.create();

        // Operations share the same type/src selectors for all inputs.
        class OpVars {
            VariableSequence type;
            VariableSequence srcA;
            VariableSequence srcB;
        }
        List<OpVars> opVars = new ArrayList<>();

        List<OpType> opOrder = new ArrayList<>(allowedOps);

        // For each op create type and source selectors.
        for (int i = 0; i < numOps; i++) {
            OpVars vars = new OpVars();
            vars.type = formula.newVariables(opOrder.size());
            Logic.exactlyOne(formula, vars.type);

            int wires = 1 + i; // input plus previous op results
            vars.srcA = formula.newVariables(wires);
            Logic.exactlyOne(formula, vars.srcA);
            vars.srcB = formula.newVariables(wires);
            Logic.exactlyOne(formula, vars.srcB);
            opVars.add(vars);
        }

        // Simulate program for all 256 input values.
        for (int x = 0; x < 256; x++) {
            // Fixed input bits for this x.
            VariableSequence in = formula.newVariables(8);
            for (int b = 0; b < 8; b++) {
                boolean bit = ((x >> (7 - b)) & 1) == 1;
                formula.add((bit ? "" : "-") + in.get(b) + " 0");
            }

            List<VariableSequence> wires = new ArrayList<>();
            wires.add(in);

            // Execute symbolic ops.
            for (int i = 0; i < numOps; i++) {
                OpVars vars = opVars.get(i);
                VariableSequence a = mux(formula, wires, vars.srcA);
                VariableSequence b = mux(formula, wires, vars.srcB);
                Map<OpType, VariableSequence> results = new EnumMap<>(OpType.class);

                for (OpType t : opOrder) {
                    results.put(t, apply(formula, t, a, b));
                }

                VariableSequence chosen = mux(formula,
                        opOrder.stream().map(results::get).toList(),
                        vars.type);
                wires.add(chosen);
            }

            VariableSequence out = wires.get(wires.size() - 1);
            int expected = f.apply(x) & 0xFF;
            for (int b = 0; b < 8; b++) {
                boolean bit = ((expected >> (7 - b)) & 1) == 1;
                formula.add((bit ? "" : "-") + out.get(b) + " 0");
            }
        }

        // Write to file so an external solver can be used.
        formula.writeToFile("dimacs/bitwise_synth.cnf");

        // At this point an external SAT solver should be run on the generated
        // file to determine satisfiability. Parsing the result back into a
        // Program object is left as an exercise for the caller.
        return Optional.empty();
    }

    /** Apply an operation on two inputs producing fresh output variables. */
    private static VariableSequence apply(Formula formula, OpType t,
                                          VariableSequence a, VariableSequence b) {
        VariableSequence out = formula.newVariables(8);
        for (int i = 0; i < 8; i++) {
            switch (t) {
                case AND -> Gates.and(formula, a.get(i), b.get(i), out.get(i));
                case OR -> Gates.or(formula, a.get(i), b.get(i), out.get(i));
                case XOR -> Gates.xor(formula, a.get(i), b.get(i), out.get(i));
                case NOT -> {
                    formula.add(-a.get(i) + " " + -out.get(i) + " 0");
                    formula.add(a.get(i) + " " + out.get(i) + " 0");
                }
                case SHL1 -> {
                    if (i == 7) {
                        formula.add("-" + out.get(i) + " 0");
                    } else {
                        Gates.eq(formula, a.get(i + 1), out.get(i));
                    }
                }
                case SHR1 -> {
                    if (i == 0) {
                        formula.add("-" + out.get(i) + " 0");
                    } else {
                        Gates.eq(formula, a.get(i - 1), out.get(i));
                    }
                }
            }
        }
        return out;
    }

    /** Build a small multiplexer selecting a wire based on a one-hot selector. */
    private static VariableSequence mux(Formula formula,
                                        List<VariableSequence> options,
                                        VariableSequence selector) {
        VariableSequence out = formula.newVariables(8);
        for (int b = 0; b < 8; b++) {
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
}
