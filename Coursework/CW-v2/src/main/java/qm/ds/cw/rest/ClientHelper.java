package qm.ds.cw.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import qm.ds.cw.grpc.*;
import qm.ds.cw.grpc.MatrixMultGrpc.MatrixMultBlockingStub;
import qm.ds.cw.rest.models.MatrixOutput;
import qm.ds.cw.utils.Utils;

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

		ArrayList<int[][]> splits = new ArrayList<>();
		splits.add(0, null);
		splits.add(1, null);
		splits.add(2, null);
		splits.add(3, null);

		CountDownLatch splitLatches = new CountDownLatch(4);
		StreamObserver<Output> outputSplitsObserver = new StreamObserver<Output>() {

			@Override
			public void onNext(Output value) {
				System.out.println(
						"[INPUT SPLIT] Received from server tile " + value.getTile() + " for matrix with index " + value.getMatrixIndex()
				);

				splits.set(value.getTile(), Utils.toArray(value.getSize(), value.getOutput()));
				splitLatches.countDown();
			}
			@Override
			public void onError(Throwable t) {
			}
			@Override
			public void onCompleted() {
			}
		};



		for (int tile = 0; tile < 4; tile++) {

			StreamObserver<SplitInput> split;
			split = MatrixFormingGrpc.newStub(clientConfig.GRPC_Channels.get(portIndex)).inputSplitting(outputSplitsObserver);
			portIndex++;

			split.onNext(splitInputBuilder.setTile(tile).build());
			split.onCompleted();
		}

		try {
			if(splitLatches.await(timeout, TimeUnit.NANOSECONDS)) {
				return new MatrixOutput(0, 0,
						splits.get(0), splits.get(1), splits.get(2), splits.get(3),
						null, 0, 0
				);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void asyncMultiply(ManagedChannel channel, StreamObserver<Output> outputObserver, int tile, int[][] a, int[][] b) {

		InputBlocks inputBlocks = Utils.arrayInputBuilder(clientStorage.blockSize, tile, a, b);

		StreamObserver<InputBlocks> inputBlocksStream;
		inputBlocksStream = MatrixMultGrpc.newStub(channel).asyncMultiplyBlocks(outputObserver);

		inputBlocksStream.onNext(inputBlocks);
		inputBlocksStream.onCompleted();
	}
	public void asyncAdd(ManagedChannel channel, StreamObserver<Output> outputObserver, int tile, Output multOutputA, Output multOutputB) {

		InputBlocks inputBlocks = Utils.objectInputBuilder(clientStorage.blockSize, tile, multOutputA, multOutputB);

		StreamObserver<InputBlocks> inputBlocksStream;
		inputBlocksStream = MatrixMultGrpc.newStub(channel).asyncAddBlocks(outputObserver);

		inputBlocksStream.onNext(inputBlocks);
		inputBlocksStream.onCompleted();
	}


	public MatrixOutput getDotProduct() {

		HashMap<Integer, ArrayList<Output>> multOutputBlocks = new HashMap<>();
		multOutputBlocks.put(0, new ArrayList<>()); multOutputBlocks.put(1, new ArrayList<>());
		multOutputBlocks.put(2, new ArrayList<>()); multOutputBlocks.put(3, new ArrayList<>());

		CountDownLatch multiplyLatches = new CountDownLatch(8);
		StreamObserver<Output> outputMultiplyObserver = new StreamObserver<Output>() {
			@Override
			public void onNext(Output value) {
				System.out.println("[MULTIPLY] Received from server tile " + value.getTile());

				ArrayList<Output> tile = multOutputBlocks.get(value.getTile()); tile.add(value);
				multOutputBlocks.put(value.getTile(), tile);

				multiplyLatches.countDown();
			}
			@Override
			public void onError(Throwable t) {
			}
			@Override
			public void onCompleted() {
			}
		};

		int[][][][] multInputBlocks = new int[4][4][clientStorage.blockSize][clientStorage.blockSize];
		multInputBlocks[0][0] = clientStorage.A00;  multInputBlocks[1][0] = clientStorage.A00;
		multInputBlocks[0][1] = clientStorage.B00;  multInputBlocks[1][1] = clientStorage.B01;
		multInputBlocks[0][2] = clientStorage.A01;  multInputBlocks[1][2] = clientStorage.A01;
		multInputBlocks[0][3] = clientStorage.B10;  multInputBlocks[1][3] = clientStorage.B11;

		multInputBlocks[2][0] = clientStorage.A10;  multInputBlocks[3][0] = clientStorage.A10;
		multInputBlocks[2][1] = clientStorage.B00;  multInputBlocks[3][1] = clientStorage.B01;
		multInputBlocks[2][2] = clientStorage.A11;  multInputBlocks[3][2] = clientStorage.A11;
		multInputBlocks[2][3] = clientStorage.B10;  multInputBlocks[3][3] = clientStorage.B11;


		GRPC_Channels_LinkedList channels_inUse = GRPC_Channels_LinkedList.getChannels(clientConfig.GRPC_SERVERS_NEEDED);
		for (int tile = 0; tile < 4; tile++) {
			for (int multBlocks = 0; multBlocks < 4; multBlocks+= 2) {

				this.asyncMultiply(channels_inUse.channel, outputMultiplyObserver,
						tile, multInputBlocks[tile][multBlocks], multInputBlocks[tile][multBlocks+1]);

				channels_inUse =  channels_inUse.next;
			}
		}

		try {
			if(!multiplyLatches.await(8* clientConfig.INPUT_FOOTPRINT, TimeUnit.NANOSECONDS)) {
				System.out.println("[WARNING] Did not meet multiply deadline!");
			}

			ArrayList<int[][]> C = new ArrayList<>();
			C.add(0, null);
			C.add(1, null);
			C.add(2, null);
			C.add(3, null);

			CountDownLatch additionLatches = new CountDownLatch(4);
			StreamObserver<Output> outputAdditionObserver = new StreamObserver<Output>() {
				@Override
				public void onNext(Output value) {
					System.out.println("[ADD] Received from server tile " + value.getTile());

					int[][] arr = Utils.toArray(value.getSize(), value.getOutput());
					int tile = value.getTile();


					C.set(tile, arr);


					additionLatches.countDown();
				}
				@Override
				public void onError(Throwable t) {
				}
				@Override
				public void onCompleted() {
				}
			};

			for (int tile = 0; tile < 4; tile++) {
				this.asyncAdd(channels_inUse.channel, outputAdditionObserver,
						tile, multOutputBlocks.get(tile).get(0), multOutputBlocks.get(tile).get(1));

				channels_inUse =  channels_inUse.next;
			}

			try {
				if(additionLatches.await(4, TimeUnit.SECONDS)) {

					clientStorage.C00 = C.get(0);
					clientStorage.C01 = C.get(1);
					clientStorage.C10 = C.get(2);
					clientStorage.C11 = C.get(3);

					MergeInput mergeInput = MergeInput.newBuilder()
							.setC00(Utils.toMatrix(clientStorage.C00))
							.setC01(Utils.toMatrix(clientStorage.C01))
							.setC10(Utils.toMatrix(clientStorage.C10))
							.setC11(Utils.toMatrix(clientStorage.C11))
							.setInputSize(clientStorage.inputSize)
							.setBlockSize(clientStorage.blockSize)
							.build();

					System.out.println(mergeInput);

					clientStorage.C = Utils.toArray(clientStorage.inputSize,
							MatrixFormingGrpc.newBlockingStub(channels_inUse.channel).outputMerging(mergeInput).getOutput());

					return new MatrixOutput(clientStorage.blockSize, clientStorage.inputSize,
							clientStorage.C00, clientStorage.C01, clientStorage.C10, clientStorage.C11, clientStorage.C,
							(int) TimeUnit.NANOSECONDS.toMillis(clientConfig.INPUT_FOOTPRINT), clientConfig.GRPC_SERVERS_NEEDED);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public long getFootprint(int[][] a, int[][] b, int blockSize) {

		InputBlocks multiplyInput = Utils.arrayInputBuilder(blockSize, -1, a, b);
		MatrixMultBlockingStub footPrintStream = MatrixMultGrpc.newBlockingStub(clientConfig.GRPC_Channels.get(0));

		long startTime = System.nanoTime();
		footPrintStream.syncMultiplyBlocks(multiplyInput);
		long elapsedNanos = System.nanoTime() - startTime;

		return elapsedNanos;
	}
}
class GRPC_Channels_LinkedList {

	public ManagedChannel channel;
	public GRPC_Channels_LinkedList next;

	public GRPC_Channels_LinkedList (ManagedChannel channel) {
		this.next = null;
		this.channel =  channel;
	}

	public GRPC_Channels_LinkedList () {
		this.next = null;
		this.channel =  null;
	}

	public static GRPC_Channels_LinkedList getChannels(int numServers) {

		if (numServers > ClientConfig.GRPC_SERVERS_USABLE) {
			System.out.println(
					"[Warning] Only " + ClientConfig.GRPC_SERVERS_USABLE
							+ " servers available, but required are " + numServers
							+ ". Using maximum available."
			);

			numServers = ClientConfig.GRPC_SERVERS_USABLE;
		}

		GRPC_Channels_LinkedList ptr = new GRPC_Channels_LinkedList(ClientConfig.GRPC_Channels.get(1));
		GRPC_Channels_LinkedList head = ptr;

		for (int portIndex = 2; portIndex <= numServers; portIndex++) {
			ptr.next = new GRPC_Channels_LinkedList();
			ptr = ptr.next;

			ptr.channel = ClientConfig.GRPC_Channels.get(portIndex);
		}
		ptr.next = head;

		return head;
	}
}