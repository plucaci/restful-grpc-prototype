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

		ManagedChannel footPrintChannel = ManagedChannelBuilder
				.forAddress("localhost", ClientConfig.GRPC_FOOTPRINT_PORT)
				.usePlaintext()
				.build();

		if(isGrpcServerAlive(footPrintChannel)) {
			ClientConfig.GRPC_Channels.add(footPrintChannel);
			System.out.println("Successfully connected to footprint gRPC Server listening to port :" + ClientConfig.GRPC_FOOTPRINT_PORT);
		} else {
			System.out.println("[WARNING] Abort. Could not connect to footprint gRPC Server at port :" + ClientConfig.GRPC_FOOTPRINT_PORT);
			return;
		}

		for (int portNo = 0; portNo < ClientConfig.GRPC_SERVERS_USABLE; portNo++) {

			int port = portNo + ClientConfig.FIRST_GRPC_USABLE_PORT;
			ManagedChannel managedChannel = ManagedChannelBuilder
					.forAddress("localhost", port)
					.usePlaintext()
					.build();

			if(isGrpcServerAlive(managedChannel)) {
				ClientConfig.GRPC_Channels.add(managedChannel);
				System.out.println("Successfully connected to gRPC Server listening to port :" + port);
			} else {
				System.out.println("[WARNING] Unresponsive gRPC Server at port :" + port);
				System.out.println("Currently connected gRPC Servers: " + ClientConfig.GRPC_Channels.size());
			}
		}

		if (ClientConfig.GRPC_Channels.size() > 0)  {
			if (ClientConfig.GRPC_SERVERS_USABLE / 2 > ClientConfig.GRPC_Channels.size()) {
				System.out.println("[WARNING] Fewer than half gRPC Servers available. Performance could be impacted.");
			}
			SpringApplication.run(ClientLauncher.class, args);
		} else {
			System.out.println("Abort. Could not launch Client. No gRPC Servers available.");
		}
	}

	public static boolean isGrpcServerAlive(ManagedChannel managedChannel) {
		try {
			HealthStatusGrpc.newBlockingStub(managedChannel).checkHealth(Health.newBuilder().setHealth("ping").build());
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean doesChannelExist(ManagedChannel managedChannel) {

		String managedChannelAuthority = managedChannel.authority();
		for (ManagedChannel grpc_channel : ClientConfig.GRPC_Channels) {
			if (grpc_channel.authority().equalsIgnoreCase(managedChannelAuthority)) {
				return true;
			}
		}

		return false;
	}
}