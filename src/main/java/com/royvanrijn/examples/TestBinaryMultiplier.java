package com.royvanrijn.examples;

import java.math.BigInteger;

import com.royvanrijn.sattor.Formula;
import com.royvanrijn.sattor.VariableSequence;
import com.royvanrijn.sattor.library.Arithmetic;

public class TestBinaryMultiplier {

    private Formula formula = Formula.create();

    public static void main(String[] args) {
        new TestBinaryMultiplier().run();
    }

    private void run() {

        // Let's see it we can factor a number:

        //1299019 * 1290427
        String target = "1676289191113";

        String binary = new BigInteger(target).toString(2);

        int l1 = 1 + (binary.length()/2);
        int l2 = l1;

        // Create the circuit:
        VariableSequence in1 = formula.newVariables(l1);
        VariableSequence in2 = formula.newVariables(l2);

        VariableSequence sum = Arithmetic.mul(formula, in1, in2);

        // Fix the binary number on the output:
        for(int i = sum.length()-binary.length(); i < binary.length(); i++) {
            formula.add((binary.charAt(i)=='0'?"-":"") + sum.get(i) + " 0");
        }

        // Both input are not even:
        formula.add(in1.get(in1.length()-1) + " 0");
        formula.add(in2.get(in2.length()-1) + " 0");

//        String b1 = "110011111101111010100000111010110100110101010001111011000011000000000100110101001011001111101101100110011011110011100011000111100110101010000000111110010010011110111";
//        for(int i = 0; i < b1.length(); i++) {
//            formula.add((b1.charAt(i)=='0'?"-":"") + in1.get(i) + " 0");
//        }

//        String b2 = "110110110111100010100000111111001100011101110101101100001100110111100000000110110000000100010000000010110000010100101111101110101111010100000011111001101111100011101";
//        for(int i = 0; i < b2.length(); i++) {
//            formula.add((b2.charAt(i)=='0'?"-":"") + in2.get(i) + " 0");
//        }

//                formula.print();
        formula.writeToFile("dimacs/example.cnf");

//        try {
//            String output = Files.readString(Path.of("output.cnf"));
//            String result = sum.variables().stream().map(i->output.charAt(output.indexOf(i+" ")-1)=='-'?"0":"1").collect(Collectors.joining(""));
//            System.out.println(result);
//            BigInteger b = new BigInteger("101100100011010101100110101111010001111100100000011010101100110111001001011011010001110010101111100100000101111110001110111111011110101011100001010100001110011010111101110010011011101101001111011111110111110110011001001000100111010001010101011101110000001011011101110001110001111010010100001110111101111100010111100101100011111011", 2);
//            System.out.println(b);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }


        System.out.println(l1+" "+l2);
    }
}



