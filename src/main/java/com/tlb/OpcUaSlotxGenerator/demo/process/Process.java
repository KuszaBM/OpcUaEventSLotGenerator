package com.tlb.OpcUaSlotxGenerator.demo.process;

import com.tlb.OpcUaSlotxGenerator.demo.slots.DecisionReq;
import com.tlb.OpcUaSlotxGenerator.demo.slots.DecisionResp;
import com.tlb.OpcUaSlotxGenerator.demo.slots.ToPlcReq;
import com.tlb.OpcUaSlotxGenerator.demo.slots.ToPlcResp;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper.SlotFromPlcUsable;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper.SlotToPlcUsable;
import com.tlb.OpcUaSlotxGenerator.schedulers.InThreadScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.time.Duration;


public class Process {
    private InThreadScheduler mainScheduler;
    private InThreadScheduler plcScheduler;
    private OpcUaSlotsProvider slotsProvider;
    Logger logger = LoggerFactory.getLogger(Process.class);
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
                SlotFromPlcUsable<DecisionReq, DecisionResp> slot = slotsProvider.makeTwoDirectionSlotFromPlc(i, mainScheduler, DecisionReq.class, DecisionResp.class);
                Flux.from(slot.getPublisher()).doOnNext((x) -> {
                    logger.info("new camera for track id - {} ", x.getTrackId());
                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).publishOn(mainScheduler).map((z) -> {
                    return new DecisionResp((short) (z.getTrackId() % 2));
                }).subscribe(slot.getSubscriber());
            }
            slotsProvider.getUaNotifierSingle().setInit(true);
            for (int i = 21; i < 22; i++) {
                SlotToPlcUsable<ToPlcResp, ToPlcReq> slot = slotsProvider.makeSlotToPlc(i, mainScheduler, ToPlcResp.class, ToPlcReq.class);

                Flux.interval(Duration.ofMillis(2000000 + (i-20)*50)).onBackpressureDrop().map((z) -> {return new ToPlcResp(z.shortValue());}).doOnNext((s) -> {
                    logger.info("new req");
                }).subscribe(slot.getProcessor());

                Flux.from(slot.getProcessor()).log().doOnNext((s) -> {
                    logger.info("new resp - {}", s.getTrackId());
                }).subscribe((s) -> {
                    logger.info("!!!!!!!!!!!!!!!!!!! ALe jajca");
                });
            }
            mainScheduler.run();
        } catch (Exception e) {
            logger.info("a - ", e);
        }
    }
}
