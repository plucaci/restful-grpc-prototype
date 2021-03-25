package qm.ds.cw.rest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.ManagedChannelProvider;

import java.util.ArrayList;
import java.util.Queue;

class ClientConfig {
    public static int GRPC_FOOTPRINT_PORT = 8080;
    public static long GRPC_SERVERS_TOTAL = 12;

    public static long GRPC_SERVER_DEADLINE = 0L;
    public static long GRPC_SERVER_FOOTPRINT = 0L;
    public static int GRPC_SERVERS_NEEDED = 0;

    public static ArrayList<ManagedChannel> GRPC_Channels = new ArrayList<ManagedChannel>();

}