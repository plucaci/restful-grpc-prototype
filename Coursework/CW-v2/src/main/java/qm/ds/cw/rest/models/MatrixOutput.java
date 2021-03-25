package qm.ds.cw.rest.models;

public class MatrixOutput implements Response{
	
	private int blockSize;	
	private int[][] C00;
	private int[][] C01;
	private int[][] C10;
	private int[][] C11;
	
	private int inputSize;
	private int[][] C;

	private int footprint;

	public MatrixOutput(int blockSize, int inputSize,
						int[][] C00, int[][] C01, int[][] C10, int[][] C11, int[][] C,
						int footprint) {

		this.footprint = footprint;

		this.C00 = C00;
		this.C01 = C01;
		this.C10 = C10;
		this.C11 = C11;
		this.blockSize = blockSize;

		this.C = C;
		this.inputSize = inputSize;

	}

	public int[][] getC() {
		return this.C;
	}
	public int getInputSize() {
		return this.inputSize;
	}

	public int[][] getC00() {
		return this.C00;
	}
	public int[][] getC01() {
		return this.C01;
	}
	public int[][] getC10() {
		return this.C10;
	}
	public int[][] getC11() {
		return this.C11;
	}
	public int getblockSize() {
		return this.blockSize;
	}

}