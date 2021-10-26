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

	private final ClientConfig clientConfig;
	private final ClientStorage clientStorage;

	public ClientHelper (ClientConfig clientConfig, ClientStorage clientStorage) {
		this.clientConfig = clientConfig;
		this.clientStorage = clientStorage;
	}

	public MatrixOutput asyncInputSplitting(int[][] input, int matrixIndex, int portIndex) {

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
			}
			@Override
			public void onError(Throwable t) {
			}
			@Override
			public void onCompleted() {
				splitLatches.countDown();
			}
		};

		for (int tile = 0; tile < 4; tile++) {

			StreamObserver<SplitInput> split;
			split = MatrixFormingGrpc.newStub(ClientConfig.GRPC_Channels.get(portIndex)).inputSplitting(outputSplitsObserver);
			portIndex++;

			split.onNext(splitInputBuilder.setTile(tile).build());
			split.onCompleted();
		}

		try {
			splitLatches.await();

			return new MatrixOutput(-1, -1,
					splits.get(0), splits.get(1), splits.get(2), splits.get(3),
					null, -1, -1, -1, -1
			);

		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void asyncMultiply(ManagedChannel channel, StreamObserver<Output> outputObserver,
							  int tile, int[][] multInputBlockA, int[][] multInputBlockB) {

		InputBlocks inputBlocks = Utils.arrayInputBuilder(clientStorage.blockSize, tile, multInputBlockA, multInputBlockB);

		StreamObserver<InputBlocks> inputBlocksStream;
		inputBlocksStream = MatrixMultGrpc.newStub(channel).asyncMultiplyBlocks(outputObserver);

		// the ping effect: inputBlocksStream being the server's representation inside the client
		inputBlocksStream.onNext(inputBlocks);
		inputBlocksStream.onCompleted();
	}
	public void asyncAdd(ManagedChannel channel, StreamObserver<Output> outputObserver,
						 int tile, Output multOutputA, Output multOutputB) {

		InputBlocks inputBlocks = Utils.objectInputBuilder(clientStorage.blockSize, tile, multOutputA, multOutputB);

		StreamObserver<InputBlocks> inputBlocksStream;
		inputBlocksStream = MatrixMultGrpc.newStub(channel).asyncAddBlocks(outputObserver);

		// the ping effect: inputBlocksStream being the server's representation inside the client
		inputBlocksStream.onNext(inputBlocks);
		inputBlocksStream.onCompleted();
	}

	public MatrixOutput getDotProduct() {

		// HashMap<K,V> corresponds to the key K=tile, and V=ArrayList<Output> containing 2 matrix products received asynchronously by the client,
		// and picked up by the outputMultiplyObserver below.
		// Because the client receives this asynchronously, the tile has to be included in the InputBlocks message sent by the client,
		// which the server sends back along with the output block it yielded after processing the inputs sent by the client along with the tile.
		// outputMultiplyObserver picks up the message received by the client, and adds to the ArrayList of that key K in the HashMap, the value it received.
		HashMap<Integer, ArrayList<Output>> multOutputBlocks = new HashMap<>();
		multOutputBlocks.put(0, new ArrayList<>()); multOutputBlocks.put(1, new ArrayList<>());
		multOutputBlocks.put(2, new ArrayList<>()); multOutputBlocks.put(3, new ArrayList<>());

		// 8 latches; 1 to be released per each multiplication (2 multiplications / tile) once the server returns the result of this to the client
		// 4 tiles: 1 latch / multiplication; 2 multiplications / tile; 8 multiplications
		CountDownLatch multiplyLatches = new CountDownLatch(8);

		StreamObserver<Output> outputMultiplyObserver = new StreamObserver<Output>() {
			@Override
			public void onNext(Output value) {
				System.out.println("[MULTIPLY] Received from server tile " + value.getTile());

				ArrayList<Output> tile = multOutputBlocks.get(value.getTile()); tile.add(value);
				multOutputBlocks.put(value.getTile(), tile);

			}
			@Override
			public void onError(Throwable t) {
			}
			@Override
			public void onCompleted() {
				multiplyLatches.countDown();
			}
		};

		// Dimensions: [tile][block for multiplication in tile][block row][block column]
		// Dimension 1: [0-3, tiles of the resulting matrix]
		// Dimension 2: [0-3, first 2 blocks and last 2 blocks meant for multiplication in that tile]
		// Remaining 2 dimensions: The rows and columns; 2D dimensions of each for each of these 4 blocks: [row][col]
		int[][][][] multInputBlocks = new int[4][4][clientStorage.blockSize][clientStorage.blockSize];
		multInputBlocks[0][0] = clientStorage.A00;  multInputBlocks[1][0] = clientStorage.A00;
		multInputBlocks[0][1] = clientStorage.B00;  multInputBlocks[1][1] = clientStorage.B01;
		multInputBlocks[0][2] = clientStorage.A01;  multInputBlocks[1][2] = clientStorage.A01;
		multInputBlocks[0][3] = clientStorage.B10;  multInputBlocks[1][3] = clientStorage.B11;

		multInputBlocks[2][0] = clientStorage.A10;  multInputBlocks[3][0] = clientStorage.A10;
		multInputBlocks[2][1] = clientStorage.B00;  multInputBlocks[3][1] = clientStorage.B01;
		multInputBlocks[2][2] = clientStorage.A11;  multInputBlocks[3][2] = clientStorage.A11;
		multInputBlocks[2][3] = clientStorage.B10;  multInputBlocks[3][3] = clientStorage.B11;

		// Determining, on Client-side, whether the total number of required resources are available: min(required, usable)
		if (clientConfig.NO_GRPC_SERVERS_REQUIRED > ClientConfig.NO_GRPC_SERVERS_USABLE) {
			System.out.println(
					"[Warning] Only " + ClientConfig.NO_GRPC_SERVERS_USABLE
							+ " servers available, but required are " + clientConfig.NO_GRPC_SERVERS_REQUIRED
							+ ". Using maximum available."
			);

			clientConfig.NO_GRPC_SERVERS_USED = ClientConfig.NO_GRPC_SERVERS_USABLE;
		} else {
			clientConfig.NO_GRPC_SERVERS_USED = clientConfig.NO_GRPC_SERVERS_REQUIRED;
		}
		Grpc_ChannelsInUse channels_inUse = new Grpc_ChannelsInUse(ClientConfig.GRPC_Channels, clientConfig.NO_GRPC_SERVERS_USED);
		//GRPC_Channels_LinkedList channels_inUse = GRPC_Channels_LinkedList.getChannels(clientConfig.GRPC_SERVERS_USED); // old, linked lists


		// for each tile, ...
		for (int tile = 0; tile < 4; tile++) {

			// multiply the first 2 blocks (multBlocks+0, multBlocks+1), and the last 2 blocks (multBlocks+2, multBlocks+3)...
			for (int multBlocks = 0; multBlocks < 4; multBlocks+= 2) {

				// step in to see the ping effect
				this.asyncMultiply(channels_inUse.getNextChannel(), outputMultiplyObserver,
						tile, multInputBlocks[tile][multBlocks], multInputBlocks[tile][multBlocks+1]);

				//channels_inUse =  channels_inUse.next;
			}
		}

		try {
			multiplyLatches.await(); // Await until the outputs are received from the servers
			// 8 (EIGHT multiplications) latches have to be released for this to go further below

			ArrayList<int[][]> C = new ArrayList<>();
			// Initialise and add 4 positions in the ArrayList, so that C.set(index=tile, block=int[][]) is called with these exact indices.
			// This method invoked on C, it places the output block received on that index, which corresponds to the tile.
			// The tile is a given, being included by the client along with the inputs to the server in the InputBlocks message when it is sent to the server,
			// and the server returns this value back in its message to the client, which is picked up in the outputAdditionObserver below.
			// Sending the tile (an integer 0 to 3) corresponding to and along with that output block helps ensure that the right blocks are used
			// by operations executed on client-side, since this receives the messages asynchronously.
			C.add(0, null);
			C.add(1, null);
			C.add(2, null);
			C.add(3, null);

			// 4 latches; 1 to be released per each addition (1 addition / tile) once the server returns the result of this to the client
			// 4 tiles: 1 latch / addition; 1 addition / tile
			CountDownLatch additionLatches = new CountDownLatch(4);
			StreamObserver<Output> outputAdditionObserver = new StreamObserver<Output>() {
				@Override
				public void onNext(Output value) {
					System.out.println("[ADD] Received from server tile " + value.getTile());
					C.set(value.getTile(), Utils.toArray(value.getSize(), value.getOutput()));

				}
				@Override
				public void onError(Throwable t) {
				}
				@Override
				public void onCompleted() {
					additionLatches.countDown();
				}
			};

			// for each tile (there were 2 matrix multiplications, which resulted in 2 matrix products)...
			for (int tile = 0; tile < 4; tile++) {

				// step in to see the ping effect
				this.asyncAdd(channels_inUse.getNextChannel(), outputAdditionObserver,
						// sum up the 2 matrix products resulted following the multiplication of the blocks;
						// These were stored once received in the HashMap<K,V> at K=tile and have both been added to the V=ArrayList<int[][]>
						tile, multOutputBlocks.get(tile).get(0), multOutputBlocks.get(tile).get(1));

				//channels_inUse =  channels_inUse.next; // obsolete: linked lists for re-using channels, bad effect on locality
			}

			try {
				additionLatches.await();// Await until the outputs are received from the servers
				// 4 (FOUR additions) latches have to be released for this to go further below

				// Finally... Now that addition has completed, the dot product has ended. We assign the resulting blocks to the client's storage.
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

				// And merge these back into a final 2D array
				clientStorage.C = Utils.toArray(clientStorage.inputSize,
						MatrixFormingGrpc.newBlockingStub(channels_inUse.getNextChannel()).outputMerging(mergeInput).getOutput());

				return new MatrixOutput(clientStorage.blockSize, clientStorage.inputSize,
						clientStorage.C00, clientStorage.C01, clientStorage.C10, clientStorage.C11, clientStorage.C,
						(int) TimeUnit.NANOSECONDS.toMillis(clientConfig.INPUT_FOOTPRINT),
						clientConfig.NO_GRPC_SERVERS_USED, clientConfig.NO_GRPC_SERVERS_REQUIRED,
						(int) TimeUnit.NANOSECONDS.toMillis(clientConfig.GRPC_SERVER_DEADLINE));

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public long getFootprint(int[][] a, int[][] b, int blockSize) {

		InputBlocks multiplyInput = Utils.arrayInputBuilder(blockSize, -1, a, b); // tile is irrelevant, order is not necessary to be ensured
		MatrixMultBlockingStub footPrintStream = MatrixMultGrpc.newBlockingStub(ClientConfig.GRPC_Channels.get(0));

		long startTime = System.nanoTime();
		footPrintStream.syncMultiplyBlocks(multiplyInput);
		long elapsedTime = System.nanoTime() - startTime;

		return elapsedTime;
	}
}
class Grpc_ChannelsInUse {

	public int channelCounter;
	public int inUse;
	public ArrayList<ManagedChannel> channelsInUse;

	public Grpc_ChannelsInUse (ArrayList<ManagedChannel> channels, int inUse) {

		this.channelCounter = 0;
		this.inUse = inUse;
		this.channelsInUse = channels;

		for (int portIndex = 0; portIndex <= this.inUse; portIndex++) {
			this.channelsInUse.add(ClientConfig.GRPC_Channels.get(portIndex));
		}
	}

	public ManagedChannel getNextChannel() {

		// If the last channel was used, the first channel is to be re-used
		// (e.g., in case only 2 servers are required, keep re-using channels)
		if (this.channelCounter > this.inUse) {
			this.channelCounter = 0;
		}
		ManagedChannel managedChannel = channelsInUse.get(this.channelCounter);

		// ... and all other channels after the first are re-used as well, and so on time and again.
		this.channelCounter++;
		return managedChannel;
	}

	// A closed-loop linked list was used before, to iterate as many time over (i.e., to re-use) the channels.
	// This was scraped due to their [linked lists] bad effect on locality
	/*
		public GRPC_Channels_LinkedList (ManagedChannel channel) {
			this.next = null;
			this.channel =  channel;
		}
		public GRPC_Channels_LinkedList () {
			this.next = null;
			this.channel =  null;
		}
		public static GRPC_Channels_LinkedList getChannels(int inUse) {

			GRPC_Channels_LinkedList ptr = new GRPC_Channels_LinkedList(ClientConfig.GRPC_Channels.get(1));
			GRPC_Channels_LinkedList head = ptr;

			for (int portIndex = 2; portIndex <= inUse; portIndex++) {
				ptr.next = new GRPC_Channels_LinkedList();
				ptr = ptr.next;

				ptr.channel = ClientConfig.GRPC_Channels.get(portIndex);
			}
			ptr.next = head;

			return head;
		}
	*/
}