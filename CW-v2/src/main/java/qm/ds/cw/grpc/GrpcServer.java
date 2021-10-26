package qm.ds.cw.grpc;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class GrpcServer {

	private static int FIRST_GRPC_USABLE_PORT; // first port in contiguous series of ports meant for GRPC Servers (default: 8080)
	private static int NO_GRPC_SERVERS_USABLE; // number of available servers (default: number of machine's CPU cores)

	private static ExecutorService serversPool;

	// If Server-side exported to runnable JAR file, args order in terminal: FIRST_GRPC_USABLE_PORT, NO_GRPC_SERVERS_USABLE
    public static void main(String[] args) {

		if (args.length == 2) { // For compiled JAR files...
			FIRST_GRPC_USABLE_PORT = Integer.getInteger(args[0]);

			NO_GRPC_SERVERS_USABLE = Integer.getInteger(args[1]);
			// Determining, on Server-side, whether the number of total given number of required servers can be acquired:
			// min(availableProcessors(), number of servers requested (via CLI) )
			if (NO_GRPC_SERVERS_USABLE > Runtime.getRuntime().availableProcessors()) {
				System.out.println("[WARNING] Number of servers requested exceeds allowed available resources. " +
						"Requested " + NO_GRPC_SERVERS_USABLE + ", but available are only " + Runtime.getRuntime().availableProcessors() +
						". Will use maximum available.");
				NO_GRPC_SERVERS_USABLE = Runtime.getRuntime().availableProcessors();
			}
		} else { // In Development / Testing...
			// default is maximum available
			NO_GRPC_SERVERS_USABLE = Runtime.getRuntime().availableProcessors();

			FIRST_GRPC_USABLE_PORT = 8080;
		}

		// Thread pool, 1 thread in thread-pool per CPU core
		serversPool = Executors.newFixedThreadPool(NO_GRPC_SERVERS_USABLE);
    	for (int portNo = 0; portNo < NO_GRPC_SERVERS_USABLE; portNo++) {
			launchGrpcServer(FIRST_GRPC_USABLE_PORT + portNo);
		}

    }

	public static void launchGrpcServer(int port) {
        System.out.println("Starting server listening to port :" + port);

        Server server = ServerBuilder.forPort(port)
				.addService(new MatrixMultImpl())
				.addService(new MatrixFormingImpl())
				.addService(new HealthStatusImpl())
				.maxInboundMessageSize(50000000)
				.build();

        // Multithreading for load balancing + non-blocking upon awaitTermination()
		serversPool.execute(new GrpcServerLauncher(server));
	}
}

class GrpcServerLauncher implements Runnable {

	private Server server;

	public GrpcServerLauncher (Server server) {
		this.server = server;
	}

	@Override
	public void run() {
		try {

			server.start();
			System.out.println("Server listening to port :" + server.getPort() + " started successfully!");
			server.awaitTermination();

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}