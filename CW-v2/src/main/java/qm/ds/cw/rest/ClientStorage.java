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
		this.A = null;
		this.A00 = null;
		this.A01 = null;
		this.A10 = null;
		this.A11 = null;

		this.B = null;
		this.B00 = null;
		this.B01 = null;
		this.B10 = null;
		this.B11 = null;

		this.C = null;
		this.C00 = null;
		this.C01 = null;
		this.C10 = null;
		this.C11 = null;

		this.inputSize = 0;
		this.blockSize = 0;
	}

	public void saveBlockSize(int inputSize) {

		blockSize = (inputSize == 4) ? 2 : inputSize / 2;

		C00 = new int[blockSize][blockSize];
		C01 = new int[blockSize][blockSize];
		C10 = new int[blockSize][blockSize];
		C11 = new int[blockSize][blockSize];
	}

	public boolean hasSplits() {
		return  this.A00 != null && this.A01 != null && this.A10 != null && this.A11 != null &&
				this.B00 != null && this.B01 != null && this.B10 != null && this.B11 != null;
	}

}