package qm.ds.cw.rest;

import io.grpc.ManagedChannel;

import java.util.ArrayList;

class ClientConfig {

    // must match server-side (i.e., contiguous)
    public static int FIRST_GRPC_USABLE_PORT = 8080; // first port in a contiguous series of ports meant for GRPC Servers (default: 8080)
    // up to the number of running servers
    public static int NO_GRPC_SERVERS_USABLE = 8;   // number of servers assumed to be available (default: 8)

    public long INPUT_FOOTPRINT         = 0L; // timed multiplication (in ns, long)
    public long GRPC_SERVER_DEADLINE    = 0L; // the deadline by which to return the output
    public int NO_GRPC_SERVERS_REQUIRED = 0;  // forecasted number of servers required using footprint & deadline

    public int NO_GRPC_SERVERS_USED     = 0;  // actual number of servers used, maximum necessary: min(usable, required)

    public static ArrayList<ManagedChannel> GRPC_Channels = new ArrayList<>(); // GRPC channels available (as many or up to 'usable')

    public ClientConfig() {
    }

}