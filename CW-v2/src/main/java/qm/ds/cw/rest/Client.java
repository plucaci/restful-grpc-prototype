package qm.ds.cw.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
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

	// Making use of the constructor to create only 1 instance of each of these utilities per client.
	// This also ensures maintainability

	// There is only 1 Client Helper / Client // Initialised at its first call, along with the config and storage, if either are null.
	private ClientHelper clientHelper;
	private ClientHelper getClientHelper() {
		if (this.clientHelper ==  null) {
			this.clientHelper = new ClientHelper(this.getClientConfig(), this.getClientStorage());
		}
		return this.clientHelper;
	}

	// Only 1 Client Config / Client // Initialised alone at its first call, essentially triggered by getClientHelper() calling this
	private ClientConfig clientConfig;
	private ClientConfig getClientConfig() {
		if (this.clientConfig == null) {
			this.clientConfig = new ClientConfig();
		}
		return this.clientConfig;
	}

	// Only 1 Client Storage / Client // Initialised alone at its first call, essentially triggered by getClientHelper() calling this
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

	//  /resolve  // to multiply the 2 matrices; sanity checks included to avoid 'Null Pointer Exceptions'
	@RequestMapping(value = "/resolve", method = RequestMethod.GET)
	public Response resolve() {
		if (getClientStorage().A != null && getClientStorage().B != null) {
			if (getClientStorage().hasSplits()) {
				if (getClientConfig().GRPC_SERVER_DEADLINE != 0 && getClientConfig().INPUT_FOOTPRINT != 0) {

					MatrixOutput result = getClientHelper().getDotProduct();

					// wiping storage to avoid wrong use of inputs in next computations
					getClientStorage().wipeStorage(); getClientConfig().INPUT_FOOTPRINT = 0L; getClientConfig().GRPC_SERVER_DEADLINE = 0L;

					return result;

				} else {
					return new Reply("Set deadline (in ms) at /set?deadline='' and get footprint at /footprint", ReplyType.ERROR);
				}
			} else {
				return new Reply("Both matrices A and B must be split first. Consider visiting /split_input first", ReplyType.ERROR);
			}
		} else {
			return new Reply("Both matrices A and B must be uploaded first. Consider visiting /upload first", ReplyType.ERROR);
		}
	}

	//  /footprint  // measuring footprint and calculating number of servers required; sanity checks included to avoid 'Null Pointer Exceptions'
	@RequestMapping(value = "/footprint", method = RequestMethod.GET)
	public Response getFootprint() {
		if (getClientStorage().A != null && getClientStorage().B != null) {
			if (getClientStorage().hasSplits()) {
				if (getClientConfig().GRPC_SERVER_DEADLINE != 0) {

					getClientConfig().INPUT_FOOTPRINT = getClientHelper().getFootprint(
							// however, other blocks could have large(r) values, and thus influence the outcome more...
							getClientStorage().A00, getClientStorage().B00, getClientStorage().blockSize);

					getClientConfig().NO_GRPC_SERVERS_REQUIRED = (int) ((8 * getClientConfig().INPUT_FOOTPRINT) / getClientConfig().GRPC_SERVER_DEADLINE);
					if (getClientConfig().NO_GRPC_SERVERS_REQUIRED == 0) {
						getClientConfig().NO_GRPC_SERVERS_REQUIRED = 1;
					}

					return new Reply("Footprint of " + TimeUnit.NANOSECONDS.toMillis(getClientConfig().INPUT_FOOTPRINT)
							+ "ms / multiplication. Requires " + getClientConfig().NO_GRPC_SERVERS_REQUIRED + " servers for 8 multiplications",
							ReplyType.SUCCESS);
				} else {
					return new Reply("Set deadline (in ms) at /set?deadline=", ReplyType.ERROR);
				}
			} else {
				return new Reply("Both matrices A and B must be split first. Consider visiting /split_input first", ReplyType.ERROR);
			}
		} else {
			return new Reply("Both matrices A and B must be uploaded first. Consider visiting /upload first", ReplyType.ERROR);
		}
	}

	//  /deadline?set=  // sets deadline; always expecting values in ms
	@RequestMapping(value = "/deadline", method = RequestMethod.POST)
	public Response setDeadline(@RequestParam(value = "set") long deadline) {

		if (deadline <= 0) {
			return new Reply("Deadline must be greater than 0 ms", ReplyType.ERROR);
		}

		getClientConfig().GRPC_SERVER_DEADLINE = TimeUnit.MILLISECONDS.toNanos(deadline);
		return new Reply("New deadline set to " + deadline + "ms", ReplyType.SUCCESS);
	}

	//  /split_input  // sanity checks included to avoid 'Null Pointer Exceptions'
	// splitting the inputs with gRPC servers; receiving 1 block per server; each input matrix is sent to 4 servers in total
	@RequestMapping(value = "/split_input", method = RequestMethod.GET)
	public Response split_input() {
		if (getClientStorage().A != null && getClientStorage().B != null) {

			MatrixOutput splitsMatrixA = getClientHelper()
					.asyncInputSplitting(getClientStorage().A, 0, 1);

			if (splitsMatrixA == null) { // asyncInputSplitting(..) returns 'null' in case it goes wrong
				return new Reply("Could not split Matrix A. Consider restarting the client", ReplyType.ERROR);
			}

			getClientStorage().A00 = splitsMatrixA.getBlock00();
			getClientStorage().A01 = splitsMatrixA.getBlock01();
			getClientStorage().A10 = splitsMatrixA.getBlock10();
			getClientStorage().A11 = splitsMatrixA.getBlock11();

			MatrixOutput splitsMatrixB = getClientHelper()
					.asyncInputSplitting(getClientStorage().B, 1, 1);

			if (splitsMatrixB == null) { // asyncInputSplitting(..) returns 'null' in case it goes wrong
				return new Reply("Could not split Matrix B. Consider restarting the client", ReplyType.ERROR);
			}

			getClientStorage().B00 = splitsMatrixB.getBlock00();
			getClientStorage().B01 = splitsMatrixB.getBlock01();
			getClientStorage().B10 = splitsMatrixB.getBlock10();
			getClientStorage().B11 = splitsMatrixB.getBlock11();

			return new Reply("Matrices A and B have been split into blocks", ReplyType.SUCCESS);
		}
		return new Reply("Both matrices A and B must be uploaded first. Consider visiting /upload first", ReplyType.ERROR);
	}

	//  /upload?file=  // 1 file per upload; with sanity checks for input sizes being included
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public Response handleFileUpload(@RequestParam(value = "file") MultipartFile file) {

		if (file.isEmpty()) {
			return new Reply("Failed to upload empty file", ReplyType.ERROR);
		}

		Scanner scanner = null;
		try {
			scanner = new Scanner(file.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		ArrayList<String> rows = new ArrayList<String>();

		int n=0, m=-1;
		while (true) {
			assert scanner != null;
			if (!scanner.hasNextLine()) break;

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

		if (getClientStorage().A != null) {
			if (getClientStorage().A.length != n || getClientStorage().A[0].length != n) {
				return new Reply("New matrix is of size " + n +"x"+ n
						+", but previous input had been passed with size " + getClientStorage().A.length +"x"+ getClientStorage().A.length,
						ReplyType.ERROR);
			}
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

		// uploading / storing is sequential: A, then B, always. Re-attempting will not re-upload either matrix. See /wipe for this just below.
		if (getClientStorage().saveInput(input)) { // saveInput(..) returns false if either matrix is stored
			return new Reply("Matrix A loaded", ReplyType.SUCCESS);
		} else {
			return new Reply("Matrix B loaded", ReplyType.SUCCESS);
		}
	}

	//  /wipe  // Removal of stored values (from ClientStorage.java) + footprint + deadline (from ClientConfig.java)
	@RequestMapping(value = "/wipe", method = RequestMethod.GET)
	public Response wipe() {
		getClientStorage().wipeStorage(); getClientConfig().INPUT_FOOTPRINT = 0L; getClientConfig().GRPC_SERVER_DEADLINE = 0L;
		return new Reply("Shining! Storage, footprint and deadline have been wiped out", ReplyType.SUCCESS);
	}

	//  /bye  // Shutting down all channels
	@RequestMapping(value = "/bye", method = RequestMethod.GET)
	public Response goodbye() {

		for (ManagedChannel grpc_channel : ClientConfig.GRPC_Channels) {
			grpc_channel.shutdown();
		}

		return new Reply("Goodbye. Client can be terminated safely now. Channels have shutdown", ReplyType.SUCCESS);
	}
}