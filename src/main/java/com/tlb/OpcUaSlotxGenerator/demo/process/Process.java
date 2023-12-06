package com.tlb.OpcUaSlotxGenerator.demo.process;

import com.tlb.OpcUaSlotxGenerator.demo.slots.*;
import com.tlb.OpcUaSlotxGenerator.opcUa.*;
import com.tlb.OpcUaSlotxGenerator.schedulers.InThreadScheduler;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.sql.SQLException;
import java.util.List;


public class Process {
    private InThreadScheduler mainScheduler;
    private InThreadScheduler plcScheduler;
    private OpcUaSlotsProvider slotsProvider;
    Logger logger = LoggerFactory.getLogger(Process.class);

    public Process(OpcUaSlotsProvider slotsProvider) {
        this.slotsProvider = slotsProvider;
        this.mainScheduler = new InThreadScheduler("Process");
        this.plcScheduler = slotsProvider.getScheduler();
    }

    public void start() {
        SlotToPlc slotToPlc = new SlotToPlc(this.slotsProvider.getSlotToAdd().get(0));

        SlotFromPlc slot1 = new SlotFromPlc(slotsProvider.getSlotToAdd().get(1));
        slot1.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
        Publisher<SlotCameraReq> p1 = slot1.makePublisher(new OpcUaReader<>(SlotCameraReq.class, slotsProvider.getSlotToAdd().get(1)), plcScheduler);
        slot1.makeAutoAck();

        for (int i = 1; i < 21; i++) {
            SlotFromPlc slot = new SlotFromPlc(this.slotsProvider.getSlotToAdd().get(i));
            slot.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
            Publisher<DecisionReq> p = slot.makePublisher(new OpcUaReader<>(DecisionReq.class, slotsProvider.getSlotToAdd().get(i)), plcScheduler);
            slot.makeAutoAck(mainScheduler);
            //Subscriber<DecisionResp> s = slot.makeSubscriber(new OpcUaWriter<>(DecisionResp.class, slotsProvider.getSlotToAdd().get(i)), mainScheduler);
            Flux<DecisionResp> newF = Flux.from(p).doOnNext((x) -> {
                logger.info("new camera for track id - {} ", x.getTrackId());
            }).map((z) -> {
                return new DecisionResp((short) 3);
            });
            //Flux<DecisionResp> neF =
            newF.doOnNext((o) -> {
                logger.info("new decision to be sent");
            }).subscribe();

        }

        Flux<SlotCameraReq> x = Flux.from(p1);
        x.doOnNext((s) -> {
            logger.info("TID - {}, BC - {}", s.getTrackId(), s.getBarcode());
        }).map((s) -> {
            return Integer.parseInt(s.getBarcode());
        }).doOnNext((in) -> {
            logger.info("{}", in.intValue());
        } ).doOnError(SQLException.class, (s) -> {}).subscribe();

        Thread notifier = new Thread(() -> slotsProvider.getUaNotifierSingle().run());
        notifier.start();
        mainScheduler.run();

    }
}