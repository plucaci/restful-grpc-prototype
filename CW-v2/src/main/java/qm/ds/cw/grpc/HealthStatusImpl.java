package qm.ds.cw.grpc;
import io.grpc.stub.StreamObserver;
import qm.ds.cw.grpc.HealthStatusGrpc.HealthStatusImplBase;

public class HealthStatusImpl extends HealthStatusImplBase {

    @Override
    public void checkHealth(Health request, StreamObserver<Health> responseObserver) {
        responseObserver.onNext(request);
        responseObserver.onCompleted();
    }
}