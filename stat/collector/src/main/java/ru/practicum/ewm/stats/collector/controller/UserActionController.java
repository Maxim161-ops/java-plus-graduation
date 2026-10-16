package ru.practicum.ewm.stats.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.collector.kafka.UserActionProducer;
import ru.practicum.ewm.stats.collector.mapper.UserActionMapper;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@GrpcService
public class UserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final UserActionMapper mapper;
    private final UserActionProducer producer;

    @Override
    public void collectUserAction(
            UserActionProto request,
            StreamObserver<Empty> responseObserver
    ) {
        UserActionAvro userAction = mapper.toAvro(request);
        producer.send(userAction);

        System.out.println("Proto: " + request);
        System.out.println("Avro: " + userAction);

        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }
}
