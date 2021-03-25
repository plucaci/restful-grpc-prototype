package qm.ds.cw.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

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

	@RequestMapping(value = "/resolve", method = RequestMethod.GET)
	public Response compute() {

		ClientHelper
				.splitInputs(ClientStorage.A, 1,1,
					ClientStorage.A00, ClientStorage.A01, ClientStorage.A10, ClientStorage.A11);
		//ClientHelper
		//		.splitInputs(ClientStorage.B, 2,5,
		//			ClientStorage.B00, ClientStorage.B01, ClientStorage.B10, ClientStorage.B11);


		// however, other blocks could have large(r) values
		//ClientConfig.GRPC_SERVER_FOOTPRINT = ClientHelper
		//		.getFootprint(ClientStorage.A00, ClientStorage.A01, ClientStorage.blockSize);
		//ClientConfig.GRPC_SERVERS_NEEDED = (int) (ClientConfig.GRPC_SERVER_FOOTPRINT/ClientConfig.GRPC_SERVER_DEADLINE);



		MatrixOutput matOut = new MatrixOutput(0, 0,
				ClientStorage.A00, ClientStorage.A01, ClientStorage.A10, ClientStorage.A11, ClientStorage.A,
				(int) (ClientConfig.GRPC_SERVER_FOOTPRINT/1000000000L));

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
		ClientConfig.GRPC_SERVER_DEADLINE = deadline * 1000000000L;
		return new Reply("New deadline set to " + deadline + " seconds", ReplyType.SUCCESS);
	}
	@RequestMapping(value = "/footprint_port", method = RequestMethod.POST)
	public Response setGrpcFootprintServerPort(@RequestParam(value = "set") int footprint_port) {
		ClientConfig.GRPC_FOOTPRINT_PORT = footprint_port;
		return new Reply("Footprint load determined by GRPC Server at port:" + footprint_port, ReplyType.SUCCESS);
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

		ClientStorage.inputSize = n;
		ClientStorage.saveBlockSize(n);
		if (ClientStorage.saveInput(input)) {
			return new Reply("Matrix A loaded", ReplyType.SUCCESS);
		} else {
			return new Reply("Matrix B loaded", ReplyType.SUCCESS);
		}

	}
	
}