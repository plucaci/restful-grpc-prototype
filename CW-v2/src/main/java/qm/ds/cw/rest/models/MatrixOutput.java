package qm.ds.cw.rest.models;

public class MatrixOutput implements Response{
	
	private int blockSize;	
	private int[][] block00;
	private int[][] block01;
	private int[][] block10;
	private int[][] block11;
	
	private int inputSize;
	private int[][] C;

	private int footprint;
	private int no_servers_used;
	private int no_servers_required;
	private int deadline;

	// REST Model for final output, or for temporarily storing intermediate results

	public MatrixOutput(int blockSize, int inputSize,
						int[][] block00, int[][] block01, int[][] block10, int[][] block11, int[][] C,
						int footprint, int no_servers_used, int no_servers_required, int deadline) {

		this.footprint = footprint;
		this.no_servers_used = no_servers_used;
		this.no_servers_required = no_servers_required;
		this.deadline = deadline;

		this.block00 = block00;
		this.block01 = block01;
		this.block10 = block10;
		this.block11 = block11;
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

	public int[][] getBlock00() {
		return this.block00;
	}
	public int[][] getBlock01() {
		return this.block01;
	}
	public int[][] getBlock10() {
		return this.block10;
	}
	public int[][] getBlock11() {
		return this.block11;
	}
	public int getBlockSize() {
		return this.blockSize;
	}

	public int getFootprint() {
		return this.footprint;
	}
	public int getNo_servers_used() {
		return this.no_servers_used;
	}
	public int getNo_servers_required() {
		return this.no_servers_required;
	}
	public int getDeadline() {
		return this.deadline;
	}

}