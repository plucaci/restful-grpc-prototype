package qm.ds.cw.grpc;
import io.grpc.stub.StreamObserver;
import qm.ds.cw.grpc.HealthStatusGrpc.HealthStatusImplBase;

public class HealthStatusImpl extends HealthStatusImplBase {

    // The footprint being taken before any channels have been used,
    // this would cause connection delay while the timer is still active.
    // Thus, pinging the server upon starting up the client.
    @Override
    public void checkHealth(Health request, StreamObserver<Health> responseObserver) {
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }
}