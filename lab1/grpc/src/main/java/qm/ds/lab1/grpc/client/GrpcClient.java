package qm.ds.lab1.grpc.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import qm.ds.lab1.grpc.MatrixMultGrpc;
import qm.ds.lab1.grpc.MatrixMultGrpc.MatrixMultBlockingStub;
import qm.ds.lab1.grpc.MatrixMultUnitTest;
import qm.ds.lab1.grpc.Utils;

public class GrpcClient {
    
    private static MatrixMultBlockingStub stub = null;

	public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
            .usePlaintext()
            .build();
        
        stub = MatrixMultGrpc.newBlockingStub(channel);
        
        new MatrixMultUnitTest(4, 12, 0, 3);
        
        channel.shutdown();
    }
    
    public static int[][] multiplyMatrixBlock(int A[][], int B[][], int inputSize, int bSize) {
    	    	
    	int[][] A00 = new int[bSize][bSize];
    	int[][] B00 = new int[bSize][bSize];
    	int[][] C00 = new int[bSize][bSize];
    	
    	int[][] A01 = new int[bSize][bSize];
    	int[][] B01 = new int[bSize][bSize];
    	int[][] C01 = new int[bSize][bSize];
    	
    	int[][] A10 = new int[bSize][bSize];
    	int[][] B10 = new int[bSize][bSize];
    	int[][] C10 = new int[bSize][bSize];
    	
    	int[][] A11 = new int[bSize][bSize];
    	int[][] B11 = new int[bSize][bSize];
    	int[][] C11 = new int[bSize][bSize];
    	
    	int[][] C = new int[inputSize][inputSize];
    	
    	for (int i = 0; i < bSize; i++) 
        { 
            for (int j = 0; j < bSize; j++)
            {
                A00[i][j]=A[i][j];
                B00[i][j]=B[i][j];
            }
        }
    	for (int i = 0; i < bSize; i++) 
        { 
            for (int j = bSize; j < inputSize; j++)
            {
                A01[i][j-bSize]=A[i][j];
                B01[i][j-bSize]=B[i][j];
            }
        }
    	for (int i = bSize; i < inputSize; i++) 
        { 
            for (int j = 0; j < bSize; j++)
            {
                A10[i-bSize][j]=A[i][j];
                B10[i-bSize][j]=B[i][j];
            }
        } 
    	for (int i = bSize; i < inputSize; i++) 
        { 
            for (int j = bSize; j < inputSize; j++)
            {
                A11[i-bSize][j-bSize]=A[i][j];
                B11[i-bSize][j-bSize]=B[i][j];
            }
        }
    	
    	C00 =  Utils.outputToInteger(stub, bSize, A00,B00,A01,B10);
    	C01 =  Utils.outputToInteger(stub, bSize, A00,B01,A01,B11);
    	C10 =  Utils.outputToInteger(stub, bSize, A10,B00,A11,B10);
    	C11 =  Utils.outputToInteger(stub, bSize, A10,B01,A11,B11);
    	
    	for (int i = 0; i < bSize; i++) { 
            for (int j = 0; j < bSize; j++) {
                C[i][j]=C00[i][j];
            }
        }
    	
    	for (int i = 0; i < bSize; i++) { 
            for (int j = bSize; j < inputSize; j++) {
                C[i][j]=C01[i][j-bSize];
            }
        }
    	
    	for (int i = bSize; i < inputSize; i++) { 
            for (int j = 0; j < bSize; j++) {
                C[i][j]=C10[i-bSize][j];
            }
        }
    	
    	for (int i = bSize; i < inputSize; i++) { 
            for (int j = bSize; j < inputSize; j++) {
                C[i][j]=C11[i-bSize][j-bSize];
            }
        }
    	
    	return C;
    }
}
