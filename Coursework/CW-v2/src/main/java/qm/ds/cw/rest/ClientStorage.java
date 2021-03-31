package qm.ds.cw.rest;

import java.util.ArrayList;

class ClientStorage {

	public ClientStorage() { }

	public int inputSize;
	public int blockSize;

	public int[][] A = null;
	public int[][] A00 = null;
	public int[][] A01 = null;
	public int[][] A10 = null;
	public int[][] A11 = null;

	public int[][] B = null;
	public int[][] B00 = null;
	public int[][] B01 = null;
	public int[][] B10 = null;
	public int[][] B11 = null;

	public int[][] C = null;
	public int[][] C00 = null;
	public int[][] C01 = null;
	public int[][] C10 = null;
	public int[][] C11 = null;

	public ArrayList<int[][]> inputBlocks = new ArrayList<>();

	public boolean saveInput(int[][] in) {
		if (A == null) {
			A = in;
			return true;

		} else if (B == null) {
			B = in;
			return false;

		}

		return false;
	}

	public void wipeStorage() {
		A = null;
		B = null;
		C = null;

		inputSize = 0;
		blockSize = 0;
	}

	public void saveBlockSize(int inputSize) {

		blockSize = (inputSize == 4) ? 2 : inputSize / 2;

		A00 = new int[blockSize][blockSize];
		A01 = new int[blockSize][blockSize];
		A10 = new int[blockSize][blockSize];
		A11 = new int[blockSize][blockSize];

		B00 = new int[blockSize][blockSize];
		B01 = new int[blockSize][blockSize];
		B10 = new int[blockSize][blockSize];
		B11 = new int[blockSize][blockSize];

		C00 = new int[blockSize][blockSize];
		C01 = new int[blockSize][blockSize];
		C10 = new int[blockSize][blockSize];
		C11 = new int[blockSize][blockSize];
	}
}