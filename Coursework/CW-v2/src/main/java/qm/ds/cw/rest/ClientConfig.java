package qm.ds.cw.rest;

import io.grpc.ManagedChannel;

import java.util.ArrayList;

class ClientConfig {

    public static long GRPC_SERVERS_USABLE = 12;
    public int GRPC_SERVERS_NEEDED = 0;
    public static int GRPC_FOOTPRINT_PORT = 8080;

    public long GRPC_SERVER_DEADLINE = 0L;
    public long INPUT_FOOTPRINT = 0L;

    public static ArrayList<ManagedChannel> GRPC_Channels = new ArrayList<ManagedChannel>();

    public ClientConfig (int grpc_servers_needed, long output_deadline, long input_footprint) {
        this.GRPC_SERVERS_NEEDED = grpc_servers_needed;
        this.GRPC_SERVER_DEADLINE = output_deadline;
        this.INPUT_FOOTPRINT = input_footprint;
    }

    public ClientConfig() { }






}