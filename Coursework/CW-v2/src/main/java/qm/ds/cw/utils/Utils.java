package qm.ds.cw.utils;

import qm.ds.cw.grpc.InputBlocks;
import qm.ds.cw.grpc.Matrix;
import qm.ds.cw.grpc.Output;
import qm.ds.cw.grpc.Matrix.Builder;
import qm.ds.cw.grpc.MatrixMultGrpc.MatrixMultBlockingStub;

public class Utils {
	
	public static boolean isPowerOfTwo(int n) {
		return (n > 0 && (n & (n - 1)) == 0);
	}
	
	public static Matrix toMatrix(int[][] C) {
    	
        Builder M = Matrix.newBuilder();
        
        for (int i=0; i<C.length; i++) {
    		for (int j=0; j<C.length; j++) {
    			M.addFlatten(C[i][j]);
    		}
    	}
        
        return M.build();
    }
    
    public static int[][] toArray(int size, Matrix M) {
        
    	int[][] C = new int[size][size];
    	
    	int f = 0;
    	for (int i=0; i<C.length; i++) {
    		for (int j=0; j<C.length; j++) {
    			C[i][j]=M.getFlatten(f++);
    		}
    	}
    	
		return C;
    }
    
    public static InputBlocks inputBuilder(int blockSize, int[][]... C) {

		InputBlocks in = InputBlocks.newBuilder()
    			.setBlockA(toMatrix(C[0]))
    			.setBlockB(toMatrix(C[1]))
    			.setBlockSize(blockSize)
    			.build();
    	
		return in;
    }
    
    public static InputBlocks inputBuilder(int blockSize, Output... out) {

		InputBlocks in = InputBlocks.newBuilder()
    			.setBlockA(out[0].getOutput())
    			.setBlockB(out[1].getOutput())
    			.setBlockSize(blockSize)
    			.build();
    	
		return in;
    }
    
    public static int[][] outputToInteger(MatrixMultBlockingStub stub, int bSize, int[][]... C) {

		InputBlocks multiplyInput1 = inputBuilder(bSize, C[0], C[1]);
		InputBlocks multiplyInput2 = inputBuilder(bSize, C[2], C[3]);
    	
    	Output multiplyOutput1 = stub.multiplyBlock(multiplyInput1);
    	Output multiplyOutput2 = stub.multiplyBlock(multiplyInput2);

		InputBlocks addInput = inputBuilder(bSize, multiplyOutput1, multiplyOutput2);
    	Output addOutput = stub.addBlock(addInput);
    	
    	return toArray(bSize, addOutput.getOutput());
    }


    
    public static String outputToString(int[][] C, String newln) {
    	
    	String Ctring = "";
    	
    	for (int i=0; i<C.length; i++) {
    		for (int j=0; j<C.length; j++) {
    			
    			Ctring += Integer.toString(C[i][j]) + " ";
    		}
    		
			Ctring += newln;
    	}
    	
		return Ctring;
    	
    }
}