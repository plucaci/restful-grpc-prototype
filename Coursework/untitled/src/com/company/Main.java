package com.company;

public class Main {

    public static void main(String[] args) {
        int[][] C00 = null;
        C00 = new int[4][4];
        C00[0][0] = 0;

        int[][][] C = new int[4][][];
        C[0] = C00;

        C[0][0][0] = 1;
        System.out.println(C00[0][0]);
    }
}
