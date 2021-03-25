package qm.ds.cw.rest;

import java.util.ArrayList;

class ClientStorage {

	public static int inputSize;
	public static int blockSize;

	public static int[][] A = null;
	public static int[][] A00 = null;
	public static int[][] A01 = null;
	public static int[][] A10 = null;
	public static int[][] A11 = null;

	public static int[][] B = null;
	public static int[][] B00 = null;
	public static int[][] B01 = null;
	public static int[][] B10 = null;
	public static int[][] B11 = null;

	public static int[][] C = null;
	public static int[][] C00 = null;
	public static int[][] C01 = null;
	public static int[][] C10 = null;
	public static int[][] C11 = null;

	public static ArrayList<int[][]> inputBlocks = new ArrayList<>();

	public static boolean saveInput(int[][] in) {
		if (A == null) {
			A = in;
			return true;
		} else if (B == null) {
			B = in;
			return false;
		}
		return false;

	}

	public static void wipeStorage() {
		A = null;
		B = null;
		C = null;
		inputSize = 0;
		blockSize = 0;
	}

	public static void saveBlockSize(int inputSize) {
		ClientStorage.blockSize = (inputSize == 4) ? 2 : inputSize / 2;
	}
}