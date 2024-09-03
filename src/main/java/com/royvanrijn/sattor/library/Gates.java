package com.royvanrijn.sattor.library;

import java.util.List;

import com.royvanrijn.sattor.Formula;

/**
 * This class contains some of the more basic building blocks, AND/OR/XOR/NAND gates etc.
 */
public class Gates {


    public static void eq(Formula formula, int in1, int in2) {
        formula.add(in1 + " " + -in2 + " 0");
        formula.add(-in1 + " " + in2 + " 0");
    }
    public static void and(Formula formula, int in1, int in2, int out) {
        formula.add(in1 + " " + -out + " 0");
        formula.add(in2 + " " + -out + " 0");
        formula.add(-in1 + " " + -in2 + " " + out + " 0");
    }

    public static void and(Formula formula, List<Integer> variables, int out) {
        String combined = "";
        for(int in : variables) {
            formula.add(in + " " + -out + " 0");
            combined += -in + " ";
        }
        formula.add(combined + " " + out + " 0");
    }

    public static void or(Formula formula, int in1, int in2, int out) {
        formula.add(-in1 + " " + out + " 0");
        formula.add(-in2 + " " + out + " 0");
        formula.add(in1 + " " + in2 + " " + -out + " 0");
    }

    public static void or(Formula formula, List<Integer> variables, int out) {
        String combined = "";
        for(int in : variables) {
            formula.add(-in + " " + out + " 0");
            combined += in + " ";
        }
        formula.add(combined + -out + " 0");
    }

    public static void nand(Formula formula, int in1, int in2, int out) {
        formula.add(-in1 + " " + -out + " 0");
        formula.add(-in2 + " " + -out + " 0");
        formula.add(in1 + " " + in2 + " " + out + " 0");
    }

    public static void xor(Formula formula, int in1, int in2, int out) {
        formula.add(in1 + " " + in2 + " " + -out + " 0");
        formula.add(-in1 + " " + -in2 + " " + -out + " 0");
        formula.add(-in1 + " " + in2 + " " + out + " 0");
        formula.add(in1 + " " + -in2 + " " + out + " 0");
    }
//
//    public static void xor(Formula formula, int in1, int in2, int in3, int out) {
//        // TODO more generic version to build this without temp variables? looks to be slower though
//        formula.add(-in1 + " " + in2 + " " + in3 + " " + out + " 0");
//        formula.add(in1 + " " + -in2 + " " + in3 + " " + out + " 0");
//        formula.add(in1 + " " + in2 + " " + -in3 + " " + out + " 0");
//        formula.add(in1 + " " + in2 + " " + in3 + " " + -out + " 0");
//        formula.add(in1 + " " + -in2 + " " + -in3 + " " + -out + " 0");
//        formula.add(-in1 + " " + in2 + " " + -in3 + " " + -out + " 0");
//        formula.add(-in1 + " " + -in2 + " " + in3 + " " + -out + " 0");
//        formula.add(-in1 + " " + -in2 + " " + -in3 + " " + out + " 0");
//    }


}
