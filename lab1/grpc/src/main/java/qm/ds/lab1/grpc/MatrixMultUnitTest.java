package qm.ds.lab1.grpc;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import qm.ds.lab1.grpc.client.GrpcClient;

public class MatrixMultUnitTest {
	
	private int minInputSize = 4;
	private int maxInputSize = 4;

	private static Random rand = new Random();
	private int minV, maxV;
	
	private int[][] A, B, expectedC, realC;
	
	public MatrixMultUnitTest(int minInputSize, int maxInputSize, int minV, int maxV) {
		
		this.minInputSize = minInputSize;
		this.maxInputSize = maxInputSize;
		
		this.minV = minV;
		this.maxV = maxV;
		
		testDim();
		
		test();
	}
	
	@Test
	void testDim() {

		Assert.assertTrue("Minimum Input Size must be a multiple of 2", minInputSize%2==0);
		Assert.assertTrue("Maximum Input Size must be a multiple of 2", maxInputSize%2==0);

		Assert.assertTrue("Minimum Input Size must be at least equal to 4", minInputSize>=4);
		Assert.assertTrue("Maximum Input Size must be at least equal to 4", maxInputSize>=4);
		
		Assert.assertTrue("Minimum Input Size must be less than or equal to Maximum Input Size", minInputSize<=maxInputSize);
		
		Assert.assertTrue("Minimum Value must be less than or equal to Maximum Value", minV <= maxV);
	}
	
	
	private int generator() {
		return rand.nextInt(maxV - minV +1) + minV;
	}
	
	private void generateInput(int[][] in) {
		
		for (int i=0; i<in.length; i++) {
			for (int j=0; j<in.length; j++) {
				in[i][j] = generator();
			}
		}
	}
	
	private void show(int[][] in) {
		
    	for (int i=0; i<in.length; i++)
    	{
    		for (int j=0; j<in.length;j++)
    		{
    			System.out.print(in[i][j]+" ");
    		}
    		System.out.println("");
    	}
    	
    	System.out.println("");
	}
	
	@Test
	void test() {
		
		for(int inputSize=minInputSize; inputSize<=maxInputSize; inputSize+=2) {
			
			A = new int[inputSize][inputSize];
			B = new int[inputSize][inputSize];

			int bSize = (inputSize == 4)? 2 : inputSize/2;
			
			System.out.println("Block Size: " + bSize);
			
			generateInput(A);
			System.out.println("Input A: ");
			show(A);
			
			generateInput(B);
			System.out.println("Input B: ");
			show(B);
			
			expectedC = BlockMult.multiplyMatrixBlock(A, B, inputSize, bSize);
			System.out.println("EXPECTED output: ");
			show(expectedC);
			
			realC = GrpcClient.multiplyMatrixBlock(A, B, inputSize, bSize);
			System.out.println("REAL output: ");
			show(realC);
			
			for (int i=0; i<expectedC.length; i++) {
				Assert.assertArrayEquals(expectedC[i], realC[i]);
			}
		}
	}
}
