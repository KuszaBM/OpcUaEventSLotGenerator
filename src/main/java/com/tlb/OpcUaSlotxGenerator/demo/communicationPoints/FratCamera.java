package com.tlb.OpcUaSlotxGenerator.demo.communicationPoints;

import com.tlb.OpcUaSlotxGenerator.demo.industry.*;
import com.tlb.OpcUaSlotxGenerator.exceptions.SlotCreationException;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper.SlotFromPlcUsable;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper.SlotToPlcUsable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class FratCamera implements communicationPoint {

    Logger logger;
    private final String name;
    private List<Short> decisions;
    private LinkedBlockingQueue<TrackId> trackIdsQueue = new LinkedBlockingQueue<>();
    private TrackIdsPublisher trackIdsPublisher;
    private Flux<TrackId> trackIdFlux;
    private Flux<TrackIdResponse> trackIdResponseFlux;
    private Flux<FratDecision> fratDecisionFlux;
    private Flux<FratReportReq> fratReportReqFlux;
    private final SlotToPlcUsable<TrackId, TrackIdResponse> trackIdSlot;
    private final SlotFromPlcUsable<FratCameraReq, FratDecision> decisionSlot;
    private final SlotFromPlcUsable<FratReportReq, Object> reportSlot;
    private Scheduler mainScheduler;

    public FratCamera(
            String name,
            int trackIdSlotId,
            int decisionSlotId,
            int reportSlotId,
            List<Short> decisions,
            Scheduler mainScheduler,
            TrackIdProvider trackIdProvider,
            OpcUaSlotsProvider slotsProvider
    ) {
        this.name = name;
        this.logger = LoggerFactory.getLogger(name);
        this.decisions = decisions;

        // Creating slots
        try {
            this.trackIdSlot = slotsProvider.makeSlotToPlc(trackIdSlotId, mainScheduler, TrackId.class, TrackIdResponse.class);
            this.decisionSlot = slotsProvider.makeTwoDirectionSlotFromPlc(decisionSlotId, mainScheduler, FratCameraReq.class, FratDecision.class);
            this.reportSlot = slotsProvider.makeAutoAckSlotFromPlc(reportSlotId, mainScheduler, FratReportReq.class);
        } catch (SlotCreationException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        // crating fluxes
        this.trackIdsPublisher = new TrackIdsPublisher(LoggerFactory.getLogger("FRAT_TID_PUB"), mainScheduler, trackIdProvider);

          // Slot 1
        this.trackIdFlux = Flux.from(this.trackIdsPublisher).doOnNext((tid) -> {
            logger.info("new trackId - {} -> plc", tid.nextTrackId());
            trackIdsQueue.add(tid);
        });
        this.trackIdResponseFlux = Flux.from(this.trackIdSlot.getProcessor());

            // Slot 2
        this.fratDecisionFlux = Flux.from(decisionSlot.getPublisher())
                .doOnNext((req) -> {
                    logger.info("Decision request for TID: {} | BC: {}", req.getTrackId(), req.getBarcode());
                })
                .map((req) -> {
                    int rand = (int) ((Math.random() * (decisions.size() - 1)) + 1);
                    return new FratDecision(decisions.get(rand));
                });

            // Slot 3
        this.fratReportReqFlux = Flux.from(reportSlot.getPublisher())
                .doOnNext(this::reportHandle);

        this.mainScheduler = mainScheduler;
    }
    private void reportHandle(FratReportReq req) {
        logger.info("Sorter report from exit - {} | {}", req.getTrackId(), req.getReportValue());
        TrackId reportTrackId = new TrackId(req.getTrackId());
        if (reportTrackId.isValid()) {
            TrackId pulledTrackId = trackIdsQueue.poll();
            if(pulledTrackId == null) {
                logger.info("No waiting trackId on FratCamera");
                return;
            }
            while(!reportTrackId.equals(pulledTrackId)) {
                trackIdsPublisher.getTrackIdProvider().trackIdRemove(pulledTrackId);
                pulledTrackId = trackIdsQueue.poll();
                if(pulledTrackId == null) {
                    logger.info("TrackId from report not on FratCamera");
                    break;
                }
            }
            if(pulledTrackId == null)
                return;
            trackIdsPublisher.getTrackIdProvider().trackIdRemove(pulledTrackId);
        } else {
            logger.info("Report for not valid TrackId - {}", req.getTrackId());
        }
    }
    @Override
    public void register() {
        this.trackIdFlux.subscribe();
        this.trackIdResponseFlux.subscribe();
        this.fratDecisionFlux.subscribe(decisionSlot.getSubscriber());
        this.fratReportReqFlux.subscribe(reportSlot.getSubscriber());
    }
}
