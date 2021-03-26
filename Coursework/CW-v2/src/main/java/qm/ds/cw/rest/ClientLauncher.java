package qm.ds.cw.rest;

import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ClientLauncher {

	public static void main(String[] args) {
		SpringApplication.run(ClientLauncher.class, args);

		for (int port = 0; port < ClientConfig.GRPC_SERVERS_USABLE; port++) {
			ClientConfig.GRPC_Channels.add(ManagedChannelBuilder
					.forAddress("localhost", port + ClientConfig.GRPC_FOOTPRINT_PORT)
					.usePlaintext()
					.build()
			);
		}
	}
}