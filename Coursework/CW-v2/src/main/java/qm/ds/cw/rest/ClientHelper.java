package qm.ds.cw.rest;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.grpc.stub.StreamObserver;
import qm.ds.cw.grpc.*;
import qm.ds.cw.grpc.MatrixMultGrpc.MatrixMultBlockingStub;
import qm.ds.cw.rest.models.MatrixOutput;
import qm.ds.cw.utils.Utils;

//C00 = Utils.outputToInteger(this.channelStream, blockSize, A00, B00, A01, B10);
//C01 = Utils.outputToInteger(this.channelStream, blockSize, A00, B01, A01, B11);
//C10 = Utils.outputToInteger(this.channelStream, blockSize, A10, B00, A11, B10);
//C11 = Utils.outputToInteger(this.channelStream, blockSize, A10, B01, A11, B11);

class ClientHelper {

	private ClientConfig clientConfig;
	private ClientStorage clientStorage;
	public ClientHelper (ClientConfig clientConfig, ClientStorage clientStorage) {
		this.clientConfig = clientConfig;
		this.clientStorage = clientStorage;
	}

	public void inputSplitting(int inputSize, int blockSize) {

		for (int i = 0; i < blockSize; i++) {
			for (int j = 0; j < blockSize; j++) {
				clientStorage.A00[i][j] = clientStorage.A[i][j];
				clientStorage.B00[i][j] = clientStorage.B[i][j];
			}
		}

		for (int i = 0; i < blockSize; i++) {
			for (int j = blockSize; j < inputSize; j++) {
				clientStorage.A01[i][j - blockSize] = clientStorage.A[i][j];
				clientStorage.B01[i][j - blockSize] = clientStorage.B[i][j];
			}
		}

		for (int i = blockSize; i < inputSize; i++) {
			for (int j = 0; j < blockSize; j++) {
				clientStorage.A10[i - blockSize][j] = clientStorage.A[i][j];
				clientStorage.B10[i - blockSize][j] = clientStorage.B[i][j];
			}
		}

		for (int i = blockSize; i < inputSize; i++) {
			for (int j = blockSize; j < inputSize; j++) {
				clientStorage.A11[i - blockSize][j - blockSize] = clientStorage.A[i][j];
				clientStorage.B11[i - blockSize][j - blockSize] = clientStorage.B[i][j];
			}
		}
	}

	public MatrixOutput splitInputs(int[][] input, int matrixIndex, int portIndex, long timeout) {

		Matrix inMatrix = Utils.toMatrix(input);
		SplitInput.Builder splitInputBuilder = SplitInput.newBuilder()
				.setInput(inMatrix)
				.setMatrixIndex(matrixIndex)
				.setInputSize(clientStorage.inputSize)
				.setBlockSize(clientStorage.blockSize);



		ArrayList<int[][]> outputBlocksArray = new ArrayList<>();
		outputBlocksArray.add(0, null); outputBlocksArray.add(1, null);
		outputBlocksArray.add(2, null); outputBlocksArray.add(3, null);

		CountDownLatch splitLatch = new CountDownLatch(4);

		StreamObserver<Output> outputObserver = new StreamObserver<Output>() {

			@Override
			public void onNext(Output value) {

				System.out.println(
						"[INPUT SPLIT] Received from server tile " + value.getTile() + " for matrix with index " + value.getMatrixIndex()
				);

				outputBlocksArray.set(value.getTile(), Utils.toArray(value.getSize(), value.getOutput()));
				splitLatch.countDown();
			}

			@Override
			public void onError(Throwable t) { }
			@Override
			public void onCompleted() { }
		};

		// void calls in async, only in blocking stubs are defined the same proto functions that are non-void
		StreamObserver<SplitInput> split0 = MatrixFormingGrpc.newStub(clientConfig.GRPC_Channels.get(portIndex))
				.inputSplitting( outputObserver ); split0.onNext(splitInputBuilder.setTile(0).build());

		StreamObserver<SplitInput> split1 = MatrixFormingGrpc.newStub(clientConfig.GRPC_Channels.get(++portIndex))
				.inputSplitting( outputObserver ); split1.onNext(splitInputBuilder.setTile(1).build());

		StreamObserver<SplitInput> split2 = MatrixFormingGrpc.newStub(clientConfig.GRPC_Channels.get(++portIndex))
				.inputSplitting( outputObserver ); split2.onNext(splitInputBuilder.setTile(2).build());

		StreamObserver<SplitInput> split3 = MatrixFormingGrpc.newStub(clientConfig.GRPC_Channels.get(++portIndex))
				.inputSplitting( outputObserver ); split3.onNext(splitInputBuilder.setTile(3).build());

		split0.onCompleted(); split1.onCompleted();
		split2.onCompleted(); split3.onCompleted();

		try {

			if(splitLatch.await(timeout, TimeUnit.NANOSECONDS)) {

				return new MatrixOutput(0, 0,
						outputBlocksArray.get(0), outputBlocksArray.get(1), outputBlocksArray.get(2), outputBlocksArray.get(3),
						null, 0, 0
				);
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;

	}


	public long getFootprint(int[][] a, int[][] b, int blockSize) {

		InputBlocks multiplyInput = Utils.inputBuilder(blockSize, a, b);
		MatrixMultBlockingStub footPrintStream = MatrixMultGrpc.newBlockingStub(clientConfig.GRPC_Channels.get(0));

		long startTime = System.nanoTime();
		footPrintStream.multiplyBlock(multiplyInput);
		long elapsedNanos = System.nanoTime() - startTime;

		return elapsedNanos;
	}

}