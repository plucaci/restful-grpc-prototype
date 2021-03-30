package qm.ds.cw.grpc;

import io.grpc.stub.StreamObserver;
import qm.ds.cw.grpc.MatrixMultGrpc.MatrixMultImplBase;
import qm.ds.cw.utils.Utils;

import java.util.stream.Stream;

public class MatrixMultImpl extends MatrixMultImplBase {
    
	public interface BlockOperation {
    	int[][] execute(int[][] blockA, int[][] blockB, int blockSize);
    }
	
	public BlockOperation multiplyBlocks = (blockA, blockB, blockSize) -> {
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
    public BlockOperation addBlocks = (blockA, blockB, blockSize) -> {
    	int C[][]= new int[blockSize][blockSize];
    	
    	for (int i=0;i<C.length;i++) {
    		for (int j=0;j<C.length;j++) {
    			C[i][j]=blockA[i][j]+blockB[i][j];
    		}
    	}
    	
    	return C;
    };
    
    
	public Output processRequest(InputBlocks request, BlockOperation blockOperation) {
    	
		int blockSize = request.getBlockSize();
		
		int[][] blockA = Utils.toArray(blockSize, request.getBlockA());
        int[][] blockB = Utils.toArray(blockSize, request.getBlockB());
        
        int[][] newBlock = blockOperation.execute(blockA, blockB, blockSize);
        
        Output blockResponse = Output.newBuilder()
        		
        		.setOutput( Utils.toMatrix(newBlock) )
        		.build();
        
		return blockResponse;
    }
	
	@Override
    public void syncMultiplyBlocks(
      InputBlocks request, StreamObserver<Output> responseObserver) {
        System.out.println("[MULTIPLY] Request received from client:\n" + request);
        
        Output blockResponse = processRequest(request, multiplyBlocks);
        
        responseObserver.onNext(blockResponse);
        responseObserver.onCompleted();
    }


	@Override
	public StreamObserver<InputBlocks> asyncMultiplyBlocks(StreamObserver<Output> responseObserver) {
		/**
		 * System.out.println("[MULTIPLY] Request received from client:\n" + request);
		 *
		 * Output blockResponse = processRequest(request, multiplyBlock);
		 *
		 * responseObserver.onNext(blockResponse);
		 * responseObserver.onCompleted();
		 */

		return null;
	}

	@Override
    public void syncAddBlocks(
      InputBlocks request, StreamObserver<Output> responseObserver) {
        System.out.println("[ADD] Request received from client:\n" + request);
        
        Output blockResponse = processRequest(request, addBlocks);
        
        responseObserver.onNext(blockResponse);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<InputBlocks> asyncAddBlocks(StreamObserver<Output> responseObserver) {

		/**
		 * System.out.println("[ADD] Request received from client:\n" + request);
		 *
		 * Output blockResponse = processRequest(request, addBlocks);
		 *
		 * responseObserver.onNext(blockResponse);
		 * responseObserver.onCompleted();
		 */

		return null;
	}
	
}
