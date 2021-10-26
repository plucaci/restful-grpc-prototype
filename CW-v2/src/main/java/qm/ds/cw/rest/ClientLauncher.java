package qm.ds.cw.rest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import qm.ds.cw.grpc.Health;
import qm.ds.cw.grpc.HealthStatusGrpc;

@SpringBootApplication
public class ClientLauncher {

	public static void main(String[] args) {

		// Client can only connect to contiguous GRPC server ports, ranging from ClientConfig.FIRST_GRPC_USABLE_PORT
		for (int portNo = 0; portNo < ClientConfig.NO_GRPC_SERVERS_USABLE; portNo++) {

			int port = portNo + ClientConfig.FIRST_GRPC_USABLE_PORT;
			ManagedChannel managedChannel = ManagedChannelBuilder
					.forAddress("localhost", port)
					.maxInboundMessageSize(50000000)
					.usePlaintext()
					.build();

			// Avoiding connection delays when taking footprint by connecting to all servers from the beginning ...
			if(isGrpcServerAlive(managedChannel)) {
				ClientConfig.GRPC_Channels.add(managedChannel);
				System.out.println("Successfully connected to gRPC Server listening to port :" + port);
			} else {
				System.out.println("[WARNING] Unresponsive gRPC Server at port :" + port);
				System.out.println("Currently connected gRPC Servers: " + ClientConfig.GRPC_Channels.size());
			}
		}

		if (ClientConfig.GRPC_Channels.size() > 0)  {
			if ( (ClientConfig.NO_GRPC_SERVERS_USABLE / 2) > ClientConfig.GRPC_Channels.size()) {
				System.out.println("[WARNING] Fewer than half of the default gRPC Servers available. Performance could be impacted.");
			}

			// Setting the actual number of usable servers... In case there are less than default
			ClientConfig.NO_GRPC_SERVERS_USABLE = ClientConfig.GRPC_Channels.size();

			SpringApplication.run(ClientLauncher.class, args);

		} else {
			System.out.println("Abort. Could not launch Client. No gRPC Servers available.");
		}
	}

	// Pinging to detect whether 1) the server is up and running, and 2) to reduce connection delays upon getting the footprint
	// In essence, if the footprint is the first time either channel is used, this implies delays on top of the footprint itself
	public static boolean isGrpcServerAlive(ManagedChannel managedChannel) {
		try {
			HealthStatusGrpc.newBlockingStub(managedChannel).checkHealth(Health.newBuilder().setHealth("ping").build());
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}