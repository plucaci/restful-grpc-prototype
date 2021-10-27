# Usage
Client runs on port 80, by project's defaults. Server runs from 8080 to 8087, by project's defaults.

For connecting Client to Server, the Client must match the contiguous set (i.e., the range from 8080-8087) of ports.
If Client connects to fewer ports than the server has available,
then fewer channels will be used and subsequently the client will send its requests to fewer servers; This could impact performance.

# Client-side
Runs on NO_GRPC_SERVERS_USABLE ports (default 8), beginning with FIRST_GRPC_USABLE_PORT (default 8080). For more, see ClientConfig.java.
How the Client connects to the Servers, see ClientLauncher.java.

## Routing (Client.java)

### Removal of stored values (from ClientStorage.java) + footprint + deadline (from ClientConfig.java)
#### (GET) client-host/wipe

### Shutting down all channels
#### (GET) client-host/bye

### To upload 1 file at a time, with sanity checks for input sizes being included:
#### (POST) client-host/upload?file=

### Asynchronous and distributed splitting of the inputs with gRPC servers. 1/4 blocks per server. Each input matrix is sent to 4 servers in total.
#### (GET) client-host/split_input
With functionality defined in ```ClientHelper.java: public MatrixOutput asyncInputSplitting(int[][] input, int matrixIndex, int portIndex)```

### Only sets the deadline to milliseconds-long. For measuring footprint and calculating resources required, see next.
#### (POST) client-host/deadline?set=

### Measuring footprint and calculating number of servers required
#### (GET) client-host/footprint
With functionality defined in ```ClientHelper.java: public long getFootprint(int[][] a, int[][] b, int blockSize)```

### Multiply the 2 matrices  // sanity checking to avoid 'Null Pointer Exceptions', included
#### (GET) client-host/resolve
With functionality defined in ```ClientHelper.java: public MatrixOutput getDotProduct()```

## How does the Client work with less than 8 channels?
Previously, a closed-LinkedList (i.e., tail->head), which provided easy access to the channels, making these re-usable by moving a pointer over the linked list infinitely many times.
This was scraped, since linked lists have a bad effect on locality (https://kjellkod.wordpress.com/2012/02/25/why-you-should-never-ever-ever-use-linked-list-in-your-code-again/).

Currently, a separate class defines the new functionality: ```ClientHelper.java: class Grpc_ChannelsInUse```.
Particularly, in metod ```ClientHelper.java: class Grpc_ChannelsInUse: public ManagedChannel getNextChannel()```,
if the last channel was used previously, then the first channel is to be re-used (i.e., a counter is stored and re-initialised to 0)
... and all other channels after the first are re-used as well, and so on time and again.

## How does the Client handle the ```(GET) client-host/resolve``` request?
With functionality defined in ```ClientHelper.java: public MatrixOutput getDotProduct()```
It all boils down for the client to include in the message to the server, the tile (an integer from 0 to 3, inclusive) along with the inputs 
the server is expected to process, and for the client to receive the server's output along with the tile the client had sent for those inputs.
This ensures that the client is able to store and process the output correctly, and not mistake the blocks, given that they are received asynchronously.
Comments have been left in the code throughout the method above to explain this.

## What does Utils.java do?
It is a utility class for parsing the contents of Objects exchanged between the servers and the client.
Proto messages are converted to Java Objects (given that the gRPC Servers are written in Java, and so is the Client).
As such, Utils.java would extract the data from these Objects and return them as either int, int[], or int[][], etc.
The code is self-explanatory, but it all boils down to understanding that the proto messages are used as Java Objects for inbounds or outbounds messages,
and they need such separate treatment to be able to manipulate their contents.

# Server-side
## Same per above but in file ```GrpcServer.java```, and the client to connect would have to match with:
```
FIRST_GRPC_USABLE_PORT; // first port in contiguous series of ports meant for GRPC Servers (default: 8080)
NO_GRPC_SERVERS_USABLE; // number of available servers (default: number of machine's CPU cores)
```
The small difference is that each server is executed within a separate thread, to ensure multi-threading and multi-core usage.
This is enabled by the class ```GrpcServer.java: class GrpcServerLauncher``` and the following thread pool ```serversPool = Executors.newFixedThreadPool(NO_GRPC_SERVERS_USABLE);```