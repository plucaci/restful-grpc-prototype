package qm.ds.cw.grpc;

import io.grpc.stub.StreamObserver;
import qm.ds.cw.grpc.MatrixMultGrpc.MatrixMultImplBase;
import qm.ds.cw.utils.Utils;

public class MatrixMultImpl extends MatrixMultImplBase {

	public int[][] multiplyBlocks(int[][] blockA, int[][] blockB, int blockSize) {
    	int[][] C = new int[blockSize][blockSize];
		
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
    }
    public int[][] addBlocks(int[][] blockA, int[][] blockB, int blockSize) {
    	int[][] C = new int[blockSize][blockSize];
    	
    	for (int i=0;i<C.length;i++) {
    		for (int j=0;j<C.length;j++) {
    			C[i][j]=blockA[i][j]+blockB[i][j];
    		}
    	}
    	
    	return C;
    }



	@Override
    public void syncMultiplyBlocks(
      InputBlocks request, StreamObserver<Output> responseObserver) {
        System.out.println("[MULTIPLY] Request received from client:\n" + request);

		int blockSize = request.getBlockSize();
		int[][] blockA = Utils.toArray(blockSize, request.getBlockA());
		int[][] blockB = Utils.toArray(blockSize, request.getBlockB());

		int[][] outputBlock = addBlocks(blockA, blockB, blockSize);
        responseObserver.onNext(Output.newBuilder().setOutput(Utils.toMatrix(outputBlock)).setTile(request.getTile()).build());
        responseObserver.onCompleted();
    }
	@Override
	public StreamObserver<InputBlocks> asyncMultiplyBlocks(StreamObserver<Output> responseObserver) {

		return new StreamObserver<InputBlocks>() {
			final Output.Builder blockResponse = Output.newBuilder();

			@Override
			public void onNext(InputBlocks value) {
				System.out.println("[MULTIPLY] Request received from client:\n" + value);

				int blockSize = value.getBlockSize();
				int[][] blockA = Utils.toArray(blockSize, value.getBlockA());
				int[][] blockB = Utils.toArray(blockSize, value.getBlockB());

				int[][] outputBlock = multiplyBlocks(blockA, blockB, blockSize);
				blockResponse
							.setOutput( Utils.toMatrix(outputBlock) )
							.setSize(value.getBlockSize())
							.setTile(value.getTile());
			}
			@Override
			public void onError(Throwable t) {
			}
			@Override
			public void onCompleted() {

				// the pong effect: responseObserver being the client's representation inside the server
				responseObserver.onNext(blockResponse.build());
				responseObserver.onCompleted();
			}
		};
	}



	@Override
    public void syncAddBlocks(
      InputBlocks request, StreamObserver<Output> responseObserver) {
        System.out.println("[ADD] Request received from client:\n" + request);

		int blockSize = request.getBlockSize();
		int[][] blockA = Utils.toArray(blockSize, request.getBlockA());
		int[][] blockB = Utils.toArray(blockSize, request.getBlockB());

		int[][] outputBlock = addBlocks(blockA, blockB, blockSize);

		responseObserver.onNext(Output.newBuilder().setOutput(Utils.toMatrix(outputBlock)).setTile(request.getTile()).build());
        responseObserver.onCompleted();
    }
    @Override
    public StreamObserver<InputBlocks> asyncAddBlocks(StreamObserver<Output> responseObserver) {

		return new StreamObserver<InputBlocks>() {
			final Output.Builder blockResponse = Output.newBuilder();

			@Override
			public void onNext(InputBlocks value) {
				System.out.println("[ADD] Request received from client:\n" + value);

				int blockSize = value.getBlockSize();
				int[][] blockA = Utils.toArray(blockSize, value.getBlockA());
				int[][] blockB = Utils.toArray(blockSize, value.getBlockB());

				int[][] outputBlock = addBlocks(blockA, blockB, blockSize);
				blockResponse
						.setOutput( Utils.toMatrix(outputBlock) )
						.setSize(value.getBlockSize())
						.setTile(value.getTile());
			}
			@Override
			public void onError(Throwable t) {
			}
			@Override
			public void onCompleted() {

				// the pong effect: responseObserver being the client's representation inside the server
				responseObserver.onNext(blockResponse.build());
				responseObserver.onCompleted();
			}
		};
	}
}
