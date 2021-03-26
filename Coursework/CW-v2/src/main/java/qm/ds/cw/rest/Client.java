package qm.ds.cw.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import qm.ds.cw.rest.models.MatrixOutput;
import qm.ds.cw.rest.models.Reply;
import qm.ds.cw.rest.models.ReplyType;
import qm.ds.cw.rest.models.Response;
import qm.ds.cw.utils.Utils;

@RestController
public class Client {

	private ClientHelper clientHelper;
	private ClientHelper getClientHelper() {
		if (this.clientHelper ==  null) {
			this.clientHelper = new ClientHelper(this.getClientConfig(), this.getClientStorage());
		}
		return this.clientHelper;
	}

	private ClientConfig clientConfig;
	private ClientConfig getClientConfig() {
		if (this.clientConfig == null) {
			this.clientConfig = new ClientConfig();
		}
		return this.clientConfig;
	}

	private ClientStorage clientStorage;
	private ClientStorage getClientStorage() {
		if (this.clientStorage == null) {
			this.clientStorage = new ClientStorage();
		}
		return this.clientStorage;
	}

	@Autowired()
	public Client() { }

	@RequestMapping(value = "/resolve", method = RequestMethod.GET)
	public Response compute() {

		getClientHelper()
				.splitInputs(getClientStorage().A, 1,1,
						getClientStorage().A00, getClientStorage().A01, getClientStorage().A10, getClientStorage().A11);
		//getClientHelper()
		//		.splitInputs(getClientStorage().B, 2,1,
		//				getClientStorage().B00, getClientStorage().B01, getClientStorage().B10, getClientStorage().B11);


		// however, other blocks could have large(r) values
		getClientConfig().INPUT_FOOTPRINT = getClientHelper()
				.getFootprint(getClientStorage().A00, getClientStorage().A01, getClientStorage().blockSize);
		getClientConfig().GRPC_SERVERS_NEEDED = (int) (getClientConfig().INPUT_FOOTPRINT/getClientConfig().GRPC_SERVER_DEADLINE);
		if (getClientConfig().GRPC_SERVERS_NEEDED == 0) {
			getClientConfig().GRPC_SERVERS_NEEDED = 1;
		}

		MatrixOutput matOut = new MatrixOutput(0, 0,
				getClientStorage().A00, getClientStorage().A01, getClientStorage().A10, getClientStorage().A11, getClientStorage().A,
				(int) (getClientConfig().INPUT_FOOTPRINT/1000000000L));

	/**
		if (REST_Config.GRPC_FOOTPRINT_PORT != 0) {
			if (REST_Config.GRPC_SERVER_DEADLINE != 0) {
				//GrpcConfig.GRPC_SERVER_FOOTPRINT = MatrixMultUtils.getFootprint(a,b,2);
			} else {
				return new Reply("Set deadline at /set?deadline=", ReplyType.ERROR);
			}
		} else {
			return new Reply("Set GRPC Server port to be used for footprinting at /set?footprint_port=", ReplyType.ERROR);
		}
	*/
		//MatrixOutput matOut = service.multiplyMatrixBlock(InputStorage.getA(), InputStorage.getB(), InputStorage.getInputSize(), InputStorage.getBlockSize());

		//InputStorage.wipeStorage();
		return matOut;
	}

	@RequestMapping(value = "/deadline", method = RequestMethod.POST)
	public Response setDeadline(@RequestParam(value = "set") int deadline) {

		if (deadline <=0) {
			return new Reply("Deadline must be strictly greater than 0 seconds!", ReplyType.ERROR);
		}

		getClientConfig().GRPC_SERVER_DEADLINE = TimeUnit.NANOSECONDS.toMillis(deadline);
		return new Reply("New deadline set to " + deadline + " seconds", ReplyType.SUCCESS);
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public Response handleFileUpload(@RequestParam(value = "file") MultipartFile file) {
		
		if (file.isEmpty()) {
			return new Reply("Failed to upload empty file", ReplyType.ERROR);
		}
		
		Scanner scanner = null;
		try {
			scanner = new Scanner(file.getInputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ArrayList<String> rows = new ArrayList<String>();
		
		int n=0, m=-1;
		while (scanner.hasNextLine()) {
			
			String row = scanner.nextLine();
			
	        if ((m != -1 && row.split(" ").length != m) || row.split(" ").length == 0) {
	        	scanner.close();
				return new Reply("All Rows must be equal in size and not empty", ReplyType.ERROR);
			}
	        
	        m = row.split(" ").length;
	        if (!Utils.isPowerOfTwo(m)) {
	        	scanner.close();
				return new Reply("Number of Columns not a power of 2", ReplyType.ERROR);
	        }
	        
	        rows.add(row);
		}
		scanner.close();
		
		n = rows.size();
		if (n != m || !Utils.isPowerOfTwo(n)) {
			return new Reply("Number of Rows and Columns are different or the number of Columns not a power of 2", ReplyType.ERROR);
		}
		
		int[][] input = new int[n][m];
		for (int i = 0; i<n; i++) {
			
			String[] row = rows.get(i).split(" ");
			for (int j = 0; j<m; j++) {
				input[i][j] = Integer.parseInt(row[j]);
			}
		}

		getClientStorage().inputSize = n;
		getClientStorage().saveBlockSize(n);
		if (getClientStorage().saveInput(input)) {
			return new Reply("Matrix A loaded", ReplyType.SUCCESS);
		} else {
			return new Reply("Matrix B loaded", ReplyType.SUCCESS);
		}

	}
	
}