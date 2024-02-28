package com.tlb.OpcUaSlotxGenerator.demo.process;

import com.tlb.OpcUaSlotxGenerator.demo.industry.*;
import com.tlb.OpcUaSlotxGenerator.demo.slots.DecisionReq;
import com.tlb.OpcUaSlotxGenerator.demo.slots.DecisionResp;
import com.tlb.OpcUaSlotxGenerator.demo.slots.ToPlcReq;
import com.tlb.OpcUaSlotxGenerator.demo.slots.ToPlcResp;
import com.tlb.OpcUaSlotxGenerator.exceptions.SlotCreationException;
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
    private OpcUaSlotsProvider slotsProvider;
    Logger logger = LoggerFactory.getLogger(Process.class);
    public InThreadScheduler getMainScheduler() {
        return mainScheduler;
    }

    public Process(OpcUaSlotsProvider slotsProvider) {
        this.slotsProvider = slotsProvider;
        this.mainScheduler = new InThreadScheduler("Process");
    }
    public void start() {

        //Slot 1 trackId gen

        TrackIdsPublisher trackIdsPublisher = new TrackIdsPublisher(logger, mainScheduler);

        Flux<TrackId> trackIdFlux = Flux.from(trackIdsPublisher).doOnNext((tid) -> {
            logger.info("new trackId - {} -> plc", tid.nextTrackId());
         //   slotsProvider.activateSimSlot(1, tid);
        });
        trackIdsPublisher.retrievingDone();

        try {
            SlotToPlcUsable<TrackId, TrackIdResponse> slot = slotsProvider.makeSlotToPlc(1, mainScheduler, TrackId.class, TrackIdResponse.class);
            trackIdFlux.subscribe(slot.getProcessor());
            Flux.from(slot.getProcessor()).doOnNext((resp) -> {
                logger.info("Ack from Plc");
            }).subscribe();
        } catch (SlotCreationException e) {
            logger.info("jajko = ", e);
        }

        //Slot 2 Barcode assign / decision
        try {
            SlotFromPlcUsable<
                    BarcodeAssignWithDecisionReq,
                    BarcodeAssignWithDecisionResp
                    > slot = slotsProvider.makeTwoDirectionSlotFromPlc(
                    2,
                    mainScheduler,
                    BarcodeAssignWithDecisionReq.class,
                    BarcodeAssignWithDecisionResp.class);

            Flux.from(slot.getPublisher()).doOnNext((req) -> {
                logger.info("new barcode assign TID: {} -> BC: {} ", req.getTrackId(), req.getTrackId());
            }).map((z) -> {
                return new BarcodeAssignWithDecisionResp((short) (z.getTrackId() % 2 == 0 ? 2 : 3));
            }).doOnNext((a) -> {
                logger.info("New Decision  response");
               // slotsProvider.activateSimSlot(2, a);
            }).subscribe(slot.getSubscriber());
        } catch (SlotCreationException | NoSuchMethodException e) {
            logger.info("error = ", e);
        }

        //Slot 3 - 4 barcode - decision
        for(int i = 3; i < 5; i++) {
            final int slotId = i;
            try {
                SlotFromPlcUsable<BarcodeDecisionReq,
                        BarcodeDecisionResp> slot = slotsProvider.makeTwoDirectionSlotFromPlc(
                                i, mainScheduler, BarcodeDecisionReq.class, BarcodeDecisionResp.class
                );
                Flux.from(slot.getPublisher()).doOnNext((req) -> {
                    logger.info("new barcode decision Req BC: {} ", req.getBarcode());
                }).map((z) -> {
                    return new BarcodeDecisionResp((short) (System.currentTimeMillis() % 2 == 0 ? 1 : 2));
                }).doOnNext((a) -> {
                    logger.info("New Decision  response");
                  //  slotsProvider.activateSimSlot(slotId, a);
                }).subscribe(slot.getSubscriber());
            } catch (SlotCreationException | NoSuchMethodException e) {
                logger.info("jajko = ", e);
            }

        }


        try {
            for (int i = 5; i < 5; i++) {
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
            for (int i = 21; i < 21; i++) {
                SlotToPlcUsable<ToPlcResp, ToPlcReq> slot = slotsProvider.makeSlotToPlc(i, mainScheduler, ToPlcResp.class, ToPlcReq.class);

                Flux.interval(Duration.ofMillis(2000000 + (i-20)*50)).onBackpressureDrop().map((z) -> {return new ToPlcResp(z.shortValue());}).doOnNext((s) -> {
                    logger.info("new req");
                }).subscribe(slot.getProcessor());

                Flux.from(slot.getProcessor()).log().doOnNext((s) -> {
                    logger.info("new resp - {}", s.getTrackId());
                }).subscribe();
            }
            mainScheduler.run();
        } catch (Exception e) {
            logger.info("a - ", e);
        }
    }
}
