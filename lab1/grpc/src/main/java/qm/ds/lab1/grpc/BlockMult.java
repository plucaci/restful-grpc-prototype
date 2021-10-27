package qm.ds.lab1.grpc;

public class BlockMult {
    
    private static int[][] multiplyBlock(int A[][], int B[][], int bSize) {
    	
		int C[][] = new int[bSize][bSize];
		
    	for (int i=0;i<C.length;i++) {
    		for (int j=0; j<C.length; j++) { 
    			int dp = 0;
	    		for (int q=0;q<C.length;q++) { 
	    			dp += A[i][q]*B[q][j];
	    		}
	    		C[i][j] = dp;
    		}
    	}
    	
    	return C;
    }
    private static int[][] addBlock(int A[][], int B[][], int bSize) {
    	
    	int C[][]= new int[bSize][bSize];

    	for (int i=0;i<C.length;i++) {
    		for (int j=0;j<C.length;j++) {
    			
    			C[i][j]=A[i][j]+B[i][j];
    		}
    	}
    	return C;
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
    	
    	for (int i = 0; i < bSize; i++) {
            for (int j = 0; j < bSize; j++) {
                A00[i][j]=A[i][j];
                B00[i][j]=B[i][j];
            }
        }
    	for (int i = 0; i < bSize; i++) {
            for (int j = bSize; j < inputSize; j++) {
                A01[i][j-bSize]=A[i][j];
                B01[i][j-bSize]=B[i][j];
            }
        }
    	for (int i = bSize; i < inputSize; i++) {
            for (int j = 0; j < bSize; j++) {
                A10[i-bSize][j]=A[i][j];
                B10[i-bSize][j]=B[i][j];
            }
        } 
    	for (int i = bSize; i < inputSize; i++) {
            for (int j = bSize; j < inputSize; j++) {
                A11[i-bSize][j-bSize]=A[i][j];
                B11[i-bSize][j-bSize]=B[i][j];
            }
        }
    	
    	C00 = addBlock(multiplyBlock(A00,B00, bSize), multiplyBlock(A01,B10, bSize), bSize);
    	C01 = addBlock(multiplyBlock(A00,B01, bSize), multiplyBlock(A01,B11, bSize), bSize);
    	C10 = addBlock(multiplyBlock(A10,B00, bSize), multiplyBlock(A11,B10, bSize), bSize);
    	C11 = addBlock(multiplyBlock(A10,B01, bSize), multiplyBlock(A11,B11, bSize), bSize);
    	
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
