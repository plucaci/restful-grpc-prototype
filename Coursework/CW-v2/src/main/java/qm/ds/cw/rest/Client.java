package qm.ds.cw.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
	public Client() {
	}

	@RequestMapping(value = "/resolve", method = RequestMethod.GET)
	public Response compute() {

		if (ClientConfig.GRPC_FOOTPRINT_PORT != 0) {
			if (getClientConfig().GRPC_SERVER_DEADLINE != 0) {

				MatrixOutput splitsMatrixA = getClientHelper()
						.splitInputs(getClientStorage().A, 0, 1, getClientConfig().GRPC_SERVER_DEADLINE);

				getClientStorage().A00 = splitsMatrixA.getBlock00(); getClientStorage().A01 = splitsMatrixA.getBlock01();
				getClientStorage().A10 = splitsMatrixA.getBlock10(); getClientStorage().A11 = splitsMatrixA.getBlock11();

				MatrixOutput splitsMatrixB = getClientHelper()
						.splitInputs(getClientStorage().B, 1, 1, getClientConfig().GRPC_SERVER_DEADLINE);

				getClientStorage().B00 = splitsMatrixB.getBlock00(); getClientStorage().B01 = splitsMatrixB.getBlock01();
				getClientStorage().B10 = splitsMatrixB.getBlock10(); getClientStorage().B11 = splitsMatrixB.getBlock11();

				getClientConfig().INPUT_FOOTPRINT = getClientHelper().getFootprint(
						// however, other blocks could have large(r) values
						getClientStorage().A00, getClientStorage().A01, getClientStorage().blockSize);

				getClientConfig().GRPC_SERVERS_NEEDED = 1 + (int)((8*getClientConfig().INPUT_FOOTPRINT)/getClientConfig().GRPC_SERVER_DEADLINE);

				return getClientHelper().getDotProduct();
			} else {
				return new Reply("Set deadline (in seconds) at /set?deadline=", ReplyType.ERROR);
			}
		} else {
			return new Reply("Set GRPC Server port to be used for footprinting at /set?footprint_port=", ReplyType.ERROR);
		}
	}

	@RequestMapping(value = "/deadline", method = RequestMethod.POST)
	public Response setDeadline(@RequestParam(value = "set") String deadline_param) {

		if (Float.valueOf(deadline_param) <= 0) {
			return new Reply("Deadline must be strictly greater than 0 seconds!", ReplyType.ERROR);
		}

		float deadline = Float.valueOf(deadline_param) * 1000000000L;

		getClientConfig().GRPC_SERVER_DEADLINE = (long)deadline;
		return new Reply("New deadline set to " + TimeUnit.NANOSECONDS.toMillis((long)deadline) + "ms", ReplyType.SUCCESS);
	}

	@RequestMapping(value = "/footprint_port", method = RequestMethod.POST)
	public Response setFootprintPort(@RequestParam(value = "set") int footprint_port) {

		if (footprint_port < 8080) {
			return new Reply("New foot printing port must be 8080 or higher", ReplyType.ERROR);
		}

		ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", footprint_port).usePlaintext().build();

		if (ClientLauncher.doesChannelExist(managedChannel)) {
			return new Reply("Already connected to this gRPC Server", ReplyType.ERROR);
		}

		if (ClientLauncher.isGrpcServerAlive(managedChannel)) {
			ClientConfig.GRPC_Channels.set(0, managedChannel);

			return new Reply("Successfully connected to gRPC Server at port :"+ footprint_port, ReplyType.SUCCESS);
		} else {
			return new Reply("Could not connect to gRPC Server at port :"+ footprint_port, ReplyType.ERROR);
		}

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