package qm.ds.lab1.grpc;

import qm.ds.lab1.grpc.Matrix.Builder;
import qm.ds.lab1.grpc.MatrixMultGrpc.MatrixMultBlockingStub;

public class Utils {

	public static Matrix toMatrix(int[][] C) {
    	
        Builder M = Matrix.newBuilder();
        
        for (int i=0; i<C.length; i++) {
    		for (int j=0; j<C.length; j++) {
    			M.addFlatten(C[i][j]);
    		}
    	}
        
        return M.build();
    }
    
    public static int[][] toArray(int bSize, Matrix M) {
        
    	int[][] C = new int[bSize][bSize];
    	
    	int f = 0;
    	for (int i=0; i<C.length; i++) {
    		for (int j=0; j<C.length; j++) {
    			C[i][j]=M.getFlatten(f++);
    		}
    	}
    	
		return C;
    }
    
    public static Input inputBuilder(int bSize, int[][]... C) {
    	
    	Input in = Input.newBuilder()
    			.setBlockA(toMatrix(C[0]))
    			.setBlockB(toMatrix(C[1]))
    			.setBlockSize(bSize)
    			.build();
    	
		return in;
    }
    
    public static Input inputBuilder(int bSize, Output... out) {
    	
    	Input in = Input.newBuilder()
    			.setBlockA(out[0].getBlockC())
    			.setBlockB(out[1].getBlockC())
    			.setBlockSize(bSize)
    			.build();
    	
		return in;
    }
    
    public static int[][] outputToInteger(MatrixMultBlockingStub stub, int bSize, int[][]... C) {
    	
    	Input multiplyInput1 = inputBuilder(bSize, C[0], C[1]);
    	Input multiplyInput2 = inputBuilder(bSize, C[2], C[3]);
    	
    	Output multiplyOutput1 = stub.multiplyBlock(multiplyInput1);
    	Output multiplyOutput2 = stub.multiplyBlock(multiplyInput2);

    	Input addInput = inputBuilder(bSize, multiplyOutput1, multiplyOutput2);
    	Output addOutput = stub.addBlock(addInput);
    	
    	return toArray(bSize, addOutput.getBlockC());
    }
}