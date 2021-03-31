package qm.ds.cw.grpc;

import io.grpc.stub.StreamObserver;
import qm.ds.cw.grpc.MatrixFormingGrpc.MatrixFormingImplBase;
import qm.ds.cw.utils.Utils;

import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

public class MatrixFormingImpl extends MatrixFormingImplBase {

    public int[][] inputSplitting(int[][] in, int inputSize, int blockSize, int tile) {

        switch (tile) {

            case 0:
                int[][] A00 = new int[blockSize][blockSize];

                for (int i = 0; i < blockSize; i++) {
                    for (int j = 0; j < blockSize; j++) {
                        A00[i][j] = in[i][j];
                    }
                }
                return A00;

            case 1:
                int[][] A01 = new int[blockSize][blockSize];

                for (int i = 0; i < blockSize; i++) {
                    for (int j = blockSize; j < inputSize; j++) {
                        A01[i][j - blockSize] = in[i][j];
                    }
                }
                return A01;

            case 2:
                int[][] A10 = new int[blockSize][blockSize];

                for (int i = blockSize; i < inputSize; i++) {
                    for (int j = 0; j < blockSize; j++) {
                        A10[i - blockSize][j] = in[i][j];
                    }
                }

                return A10;

            case 3:
                int[][] A11 = new int[blockSize][blockSize];

                for (int i = blockSize; i < inputSize; i++) {
                    for (int j = blockSize; j < inputSize; j++) {
                        A11[i - blockSize][j - blockSize] = in[i][j];
                    }
                }
                return A11;

        }

        return null;
    }

    @Override
    public StreamObserver<SplitInput> inputSplitting(StreamObserver<Output> responseObserver) {

        return new StreamObserver<SplitInput>() {

            Output.Builder blockResponse = Output.newBuilder();

            @Override
            public void onNext(SplitInput value) {
                System.out.println("[INPUT SPLIT] Request received from client:\n" + value);

                int inputSize = value.getInputSize();
                int[][] input = Utils.toArray(inputSize, value.getInput());

                int blockSize = value.getBlockSize();
                int tile = value.getTile();
                int[][] outputBlock = inputSplitting(input, inputSize, blockSize, tile);

                blockResponse
                            .setOutput(Utils.toMatrix(outputBlock))
                            .setSize(blockSize)
                            .setTile(tile)
                            .setMatrixIndex(value.getMatrixIndex());
            }

            @Override
            public void onError(Throwable t) {

            }
            @Override
            public void onCompleted() {
                responseObserver.onNext(blockResponse.build());
            }
        };
    }


    public static int[][] outputMerging(int[][] C00, int[][] C01, int[][] C10, int[][] C11, int inputSize, int blockSize) {

        if (C00 == null || C01 == null || C10 == null || C11 == null || inputSize <= blockSize || inputSize <= 0 || blockSize <= 0) {
            return null;
        }

        int[][] C = new int[inputSize][inputSize];

        for (int i = 0; i < blockSize; i++) {
            for (int j = 0; j < blockSize; j++) {
                C[i][j] = C00[i][j];
            }
        }

        for (int i = 0; i < blockSize; i++) {
            for (int j = blockSize; j < inputSize; j++) {
                C[i][j] = C01[i][j - blockSize];
            }
        }

        for (int i = blockSize; i < inputSize; i++) {
            for (int j = 0; j < blockSize; j++) {
                C[i][j] = C10[i - blockSize][j];
            }
        }

        for (int i = blockSize; i < inputSize; i++) {
            for (int j = blockSize; j < inputSize; j++) {
                C[i][j] = C11[i - blockSize][j - blockSize];
            }
        }

        return C;
    }

    @Override
    public void outputMerging(MergeInput request, StreamObserver<Output> responseObserver) {
        System.out.println("[OUTPUT MERGE] Request received from client:\n" + request);

        int blockSize = request.getBlockSize();
        int[][] C00 = Utils.toArray(blockSize, request.getC00());
        int[][] C01 = Utils.toArray(blockSize, request.getC01());
        int[][] C10 = Utils.toArray(blockSize, request.getC10());
        int[][] C11 = Utils.toArray(blockSize, request.getC11());

        int inputSize = request.getInputSize();
        int[][] outputBlockC = outputMerging(C00, C01, C10, C11, inputSize, blockSize);

        Output blockResponse = Output.newBuilder()
                .setOutput(Utils.toMatrix(outputBlockC))
                .build();

        responseObserver.onNext(blockResponse);
        responseObserver.onCompleted();
    }
}
