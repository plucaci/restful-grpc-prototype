package qm.ds.lab1.grpc.server;

import io.grpc.stub.StreamObserver;
import qm.ds.lab1.grpc.Input;
import qm.ds.lab1.grpc.MatrixMultGrpc.MatrixMultImplBase;
import qm.ds.lab1.grpc.Output;
import qm.ds.lab1.grpc.Utils;


public class MatrixMultImpl extends MatrixMultImplBase {
    
	public interface BlockOperation {
    	int[][] execute(int[][] blockA, int[][] blockB, int blockSize);
    }
	
	
	
	public BlockOperation multiplyBlock  = (blockA, blockB, blockSize) -> {
    	int C[][]= new int[blockSize][blockSize];
		
    	for (int i=0;i<C.length;i++) {
    		
    		for (int j=0; j<C.length; j++) {
    			
    			int dp = 0;
	    		for (int q=0;q<C.length;q++) {
	    			
	    			dp += blockA[i][q]*blockB[q][j];
	    		}
	    		C[i][j] = dp;
	    		
	    		
    		}
    	}
    	return C;
    };
    public BlockOperation addBlock  = (blockA, blockB, blockSize) -> {
    	int C[][]= new int[blockSize][blockSize];
    	
    	for (int i=0;i<C.length;i++) {
    		for (int j=0;j<C.length;j++) {
    			C[i][j]=blockA[i][j]+blockB[i][j];
    		}
    	}
    	
    	return C;
    };
	
    
    
	public Output processRequest(Input request, BlockOperation blockOperation) {
    	
		int blockSize = request.getBlockSize();
		
		int[][] blockA = Utils.toArray(blockSize, request.getBlockA());
        int[][] blockB = Utils.toArray(blockSize, request.getBlockB());
        
        int[][] newBlock = blockOperation.execute(blockA, blockB, blockSize);
        
        Output blockResponse = Output.newBuilder()
        		
        		.setBlockC( Utils.toMatrix(newBlock) )
        		.build();
        
		return blockResponse;
    }
	
	@Override
    public void multiplyBlock(
      Input request, StreamObserver<Output> responseObserver) {
        System.out.println("[MULTIPLY] Request received from client:\n" + request);
        
        Output blockResponse = processRequest(request, multiplyBlock);
        
        responseObserver.onNext(blockResponse);
        responseObserver.onCompleted();
    }
	@Override
    public void addBlock(
      Input request, StreamObserver<Output> responseObserver) {
        System.out.println("[ADD] Request received from client:\n" + request);
        
        Output blockResponse = processRequest(request, addBlock);
        
        responseObserver.onNext(blockResponse);
        responseObserver.onCompleted();
    }
	
}
