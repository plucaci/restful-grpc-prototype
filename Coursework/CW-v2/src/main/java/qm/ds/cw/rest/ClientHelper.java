package qm.ds.cw.rest;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

import io.grpc.stub.StreamObserver;
import io.grpc.stub.StreamObservers;
import qm.ds.cw.grpc.*;
import qm.ds.cw.grpc.MatrixMultGrpc.MatrixMultBlockingStub;
import qm.ds.cw.utils.Utils;

//C00 = Utils.outputToInteger(this.channelStream, blockSize, A00, B00, A01, B10);
//C01 = Utils.outputToInteger(this.channelStream, blockSize, A00, B01, A01, B11);
//C10 = Utils.outputToInteger(this.channelStream, blockSize, A10, B00, A11, B10);
//C11 = Utils.outputToInteger(this.channelStream, blockSize, A10, B01, A11, B11);

class ClientHelper {

	public static void splitInputs(int[][] input, int matrixIndex, int portIndex, int[][] in00, int[][] in01, int[][] in10, int[][] in11) {

		Matrix inMatrix = Utils.toMatrix(input);

		SplitInput.Builder splitInputBuilder = SplitInput.newBuilder()
				.setInput(inMatrix)
				.setMatrixIndex(matrixIndex)
				.setInputSize(ClientStorage.inputSize)
				.setBlockSize(ClientStorage.blockSize);

		ArrayList<int[][]> outputBlocksArrays = new ArrayList<>();

		final CountDownLatch latch = new CountDownLatch(4);

		StreamObserver<Output> outputObserver = new StreamObserver<Output>() {
			@Override
			public void onNext(Output value) {
				System.out.println("[INPUT SPLIT] Received from server tile " + value.getTile()
												  + " for matrix with index " + value.getMatrixIndex());

				outputBlocksArrays.add(value.getTile(), Utils.toArray(value.getSize(), value.getOutput()));
				System.out.println("[INPUT SPLIT] Number of current splits: " + outputBlocksArrays.size());
			}

			@Override
			public void onError(Throwable t) { }

			@Override
			public void onCompleted() {

				if (outputBlocksArrays.size() == 4) {
					System.out.println("We got 4 splits!");
				}
				latch.countDown();
				System.out.println("Latches left: " + latch.getCount());
			}
		};

		// void calls in async, only in blocking stubs are defined the same proto functions that are non-void
		StreamObserver<SplitInput> split0 = MatrixFormingGrpc.newStub(ClientConfig.GRPC_Channels.get(portIndex))
				.inputSplitting( outputObserver ); split0.onNext(splitInputBuilder.setTile(0).build());

		StreamObserver<SplitInput> split1 = MatrixFormingGrpc.newStub(ClientConfig.GRPC_Channels.get(++portIndex))
				.inputSplitting( outputObserver ); split1.onNext(splitInputBuilder.setTile(1).build());

		StreamObserver<SplitInput> split2 = MatrixFormingGrpc.newStub(ClientConfig.GRPC_Channels.get(++portIndex))
				.inputSplitting( outputObserver ); split2.onNext(splitInputBuilder.setTile(2).build());

		StreamObserver<SplitInput> split3 = MatrixFormingGrpc.newStub(ClientConfig.GRPC_Channels.get(++portIndex))
				.inputSplitting( outputObserver ); split3.onNext(splitInputBuilder.setTile(3).build());

		try {
			System.out.println("WE GOT HERE");
			latch.await();

			in00 = outputBlocksArrays.get(0);
			in01 = outputBlocksArrays.get(1);
			in10 = outputBlocksArrays.get(2);
			in11 = outputBlocksArrays.get(3);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public static long getFootprint(int[][] a, int[][] b, int blockSize) {

		InputBlocks multiplyInput = Utils.inputBuilder(blockSize, a, b);
		MatrixMultBlockingStub footPrintingChannel = MatrixMultGrpc.newBlockingStub(ClientConfig.GRPC_Channels.get(0));

		long startTime = System.nanoTime();
		footPrintingChannel.multiplyBlock(multiplyInput);
		long elapsedNanos = System.nanoTime() - startTime;

		return elapsedNanos;
	}
}