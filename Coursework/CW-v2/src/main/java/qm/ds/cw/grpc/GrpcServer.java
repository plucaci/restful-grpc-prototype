package qm.ds.cw.grpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class GrpcServer {

	private static int FOOT_PRINT_SERVER_PORT = 8080;
	private static int FIRST_USABLE_PORT = 8081;
	private static int NUM_USABLE_SERVERS = 12;

	public static HashMap<Integer, Server> usableServers = new HashMap<>();
	
    public static void main(String[] args) throws IOException, InterruptedException {

    	if (args.length == 3) {
			FOOT_PRINT_SERVER_PORT = Integer.getInteger(args[0]);
			FIRST_USABLE_PORT = Integer.getInteger(args[1]);
			NUM_USABLE_SERVERS = Integer.getInteger(args[2]);
		}

    	launchGrpcServer(FOOT_PRINT_SERVER_PORT);
    	for (int portNo = 0; portNo < NUM_USABLE_SERVERS; portNo++) {
			launchGrpcServer(FIRST_USABLE_PORT + portNo);
		}

    	usableServers.get(FIRST_USABLE_PORT + NUM_USABLE_SERVERS-1).awaitTermination();

    }
    
	public static void launchGrpcServer(int atPort)  throws IOException, InterruptedException {
        System.out.println("Starting server at " + atPort);
    		Server server = ServerBuilder.forPort(atPort)
					.addService(new MatrixMultImpl())
					.addService(new MatrixFormingImpl())
					.build();

    		server.start(); usableServers.put(atPort, server);

    		//TODO FIX: server.awaitTermination() causing blocking effect and not coming back from recursion;
    		System.out.println("Server at :" + atPort + " started successfully!\n");
	}
}