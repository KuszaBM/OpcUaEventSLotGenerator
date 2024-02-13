package com.tlb.OpcUaSlotxGenerator.demo.process;

import com.tlb.OpcUaSlotxGenerator.ServicesConnector;
import com.tlb.OpcUaSlotxGenerator.demo.slots.DecisionReq;
import com.tlb.OpcUaSlotxGenerator.demo.slots.DecisionResp;
import com.tlb.OpcUaSlotxGenerator.demo.slots.ToPlcReq;
import com.tlb.OpcUaSlotxGenerator.demo.slots.ToPlcResp;
import com.tlb.OpcUaSlotxGenerator.opcUa.*;
import com.tlb.OpcUaSlotxGenerator.schedulers.InThreadScheduler;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;


public class Process {
    private InThreadScheduler mainScheduler;
    private InThreadScheduler plcScheduler;
    private OpcUaSlotsProvider slotsProvider;
    private ServicesConnector servicesConnector;
    Logger logger = LoggerFactory.getLogger(Process.class);
    private int ci = 0;
    short arg = 0;
    boolean slotLock = false;
    private final AtomicLong clientHandles = new AtomicLong(1L);

    public InThreadScheduler getMainScheduler() {
        return mainScheduler;
    }

    public Process(OpcUaSlotsProvider slotsProvider) {
        this.slotsProvider = slotsProvider;
        this.mainScheduler = new InThreadScheduler("Process");
        this.plcScheduler = slotsProvider.getScheduler();
    }
    public void start() {
        try {
            for (int i = 1; i < 2; i++) {
                SlotFromPlc slot = new SlotFromPlc(this.slotsProvider.getSlotToAdd().get(i));
                slot.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
                Publisher<DecisionReq> p = slot.makePublisher(new OpcUaReader<>(DecisionReq.class, slotsProvider.getSlotToAdd().get(i)), plcScheduler, new DecisionReq((short)0));
                Subscriber<DecisionResp> s = slot.makeSubscriber(new OpcUaWriter<>(DecisionResp.class, slotsProvider.getSlotToAdd().get(i)), mainScheduler);
                slotsProvider.addSlotFromPlc(slot);
                Flux.from(p).doOnNext((x) -> {
                    logger.info("new camera for track id - {} ", x.getTrackId());
                    try {
                        Thread.sleep(600);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).publishOn(mainScheduler).map((z) -> {
                    return new DecisionResp((short) 3);
                }).subscribe(s);
            }
            slotsProvider.getUaNotifierSingle().setInit(true);



            for (int i = 21; i < 23; i++) {
                this.ci = i;
                SlotToPlc slot = new SlotToPlc(slotsProvider.getSlotToAdd().get(i));
                slot.setUaNotifierSingle(slotsProvider.getUaNotifierSingle());
                Processor<ToPlcResp, ToPlcReq> processor = slot.makeProcessor(ToPlcResp.class, ToPlcReq.class, plcScheduler, mainScheduler);
                slotsProvider.addSlotToPlc(slot);
                Flux.interval(Duration.ofMillis(200000 + (i-20)*50)).onBackpressureDrop().map((z) -> {return new ToPlcResp(z.shortValue());}).doOnNext((s) -> {
                    logger.info("new req");
                }).publishOn(mainScheduler).subscribe(processor);
                Flux.from(processor).doOnNext((s) -> {
                    logger.info("new resp from ");
                }).publishOn(mainScheduler).subscribe();
            }
            mainScheduler.run();
        } catch (Exception e) {
            logger.info("a - ", e);
        }
    }
}
