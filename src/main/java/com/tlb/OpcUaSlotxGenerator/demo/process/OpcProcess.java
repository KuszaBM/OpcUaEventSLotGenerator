package com.tlb.OpcUaSlotxGenerator.demo.process;

import com.tlb.OpcUaSlotxGenerator.demo.industry.*;
import com.tlb.OpcUaSlotxGenerator.demo.slots.ReportReq;
import com.tlb.OpcUaSlotxGenerator.exceptions.SlotCreationException;
import com.tlb.OpcUaSlotxGenerator.handlers.PhsHandler;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper.SlotFromPlcUsable;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper.SlotToPlcUsable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;

@Component
public class OpcProcess {
    private Scheduler mainScheduler;
    private final OpcUaSlotsProvider slotsProvider;
    private final PhsHandler phsHandler;
    Logger logger = LoggerFactory.getLogger(OpcProcess.class);
    public Scheduler getMainScheduler() {
        return mainScheduler;
    }

    @Autowired
    public OpcProcess(OpcUaSlotsProvider slotsProvider, PhsHandler phsHandler) {
        this.slotsProvider = slotsProvider;
        this.phsHandler = phsHandler;
        this.mainScheduler = Schedulers.newSingle("Process");
    }

    public void st11() {

        WeightingPoint scale1 = new WeightingPoint() {
            @Override
            public Flux<Float> getResultFlux() {
                return null;
            }

            @Override
            public void setWeighting(boolean isWeighting) {

            }

            @Override
            public void newWeightingRequest(short tid) {

            }
        };

        TrackIdProvider trackIdProvider = new TrackIdProvider();

        //Slot 1
        try {
            TrackIdsPublisher trackIdsPublisher = new TrackIdsPublisher(logger, mainScheduler, trackIdProvider);
            trackIdsPublisher.retrievingDone();
            Flux<TrackId> trackIdFlux = Flux.from(trackIdsPublisher).doOnNext((tid) -> {
                logger.info("new trackId - {} -> plc", tid.nextTrackId());
                // slotsProvider.activateSimSlot(1, tid);
            });
            SlotToPlcUsable<TrackId, TrackIdResponse> slot = slotsProvider.makeSlotToPlc(1, mainScheduler, TrackId.class, TrackIdResponse.class);

            trackIdFlux.subscribe(slot.getProcessor());
            Flux.from(slot.getProcessor()).doOnNext((resp) -> {
                logger.info("Ack from Plc");
                //          phsHandler.handleInput(1, resp);
            }).subscribe();

        } catch (SlotCreationException e) {
            throw new RuntimeException(e);
        }

        //Slot 2

        try {
            SlotFromPlcUsable<FratCameraReq, FratDecision> slot = slotsProvider.makeTwoDirectionSlotFromPlc(2, mainScheduler, FratCameraReq.class, FratDecision.class);
            Flux<FratCameraReq> reqFlux = Flux.from(slot.getPublisher());
            reqFlux.doOnNext((req) -> {
                logger.info("new frat req tid: {} | bc: {}", req.getTrackId(), req.getBarcode());
            }).map((req) -> {
                return new FratDecision((short) 1002);
            }).doOnNext((resp) -> {
                logger.info("Decision send - {}", resp.getDecision());
            }).subscribe(slot.getSubscriber());

        } catch (SlotCreationException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        //Slot 3

        try {
            SlotFromPlcUsable<TrackIdReq, Object> slot = slotsProvider.makeAckOnlySlotFromPlc(3, mainScheduler, TrackIdReq.class);
            scale1.getResultFlux().doOnNext((result) -> {
                logger.info("new result - {}", result);
            }).subscribe(slot.getSubscriber());

            Flux<TrackIdReq> reqFlux = Flux.from(slot.getPublisher());
            reqFlux.doOnNext((req) -> {
                logger.info("new weighting request for TID: {}", req.getTrackId());
                scale1.newWeightingRequest(req.getTrackId());
            });


            scale1.getResultFlux().doOnNext((result) -> {
                logger.info("new result - {}", result);
            }).subscribe(slot.getSubscriber());
        } catch (SlotCreationException e) {
            throw new RuntimeException();
        }

        try {
            SlotFromPlcUsable<BarcodesVerifyReq, FratDecision> slot = slotsProvider.makeTwoDirectionSlotFromPlc(4, mainScheduler, BarcodesVerifyReq.class, FratDecision.class);
            Flux<BarcodesVerifyReq> reqFlux = Flux.from(slot.getPublisher());
            reqFlux.map(BarcodesVerifyReq::verify).subscribe(slot.getSubscriber());
        } catch (SlotCreationException | NoSuchMethodException e) {
            throw new RuntimeException();
        }




    }
    public void start2() {

        TrackIdProvider trackIdProvider = new TrackIdProvider();
        DecisionMaker decisionMaker = new DecisionMaker();

        TrackIdsPublisher trackIdsPublisher = new TrackIdsPublisher(logger, mainScheduler, trackIdProvider);
        trackIdsPublisher.retrievingDone();

        Flux<TrackId> trackIdFlux = Flux.from(trackIdsPublisher).doOnNext((tid) -> {
            logger.info("new trackId - {} -> plc", tid.nextTrackId());
            // slotsProvider.activateSimSlot(1, tid);
        });
        try {
            SlotToPlcUsable<TrackId, TrackIdResponse> slot = slotsProvider.makeSlotToPlc(1, mainScheduler, TrackId.class, TrackIdResponse.class);

            trackIdFlux.subscribe(slot.getProcessor());
            Flux.from(slot.getProcessor()).doOnNext((resp) -> {
                logger.info("Ack from Plc");
                //          phsHandler.handleInput(1, resp);
            }).subscribe();

        } catch (SlotCreationException e) {
            throw new RuntimeException(e);
        }

        try {
            SlotFromPlcUsable<SorterCameraReq, Object> slot = slotsProvider.makeAutoAckSlotFromPlc(2, mainScheduler, SorterCameraReq.class);
            Flux.from(slot.getPublisher()).doOnNext((s) -> {
                logger.info("new sorter req - {} | {}", s.getTrackId(), s.getBarcode());
                decisionMaker.makeDecision(s.getTrackId());
            }).subscribe();
        } catch (SlotCreationException e) {
            throw new RuntimeException(e);
        }

        try {
            SlotToPlcUsable<DecisionReq, TrackIdResponse> slot = slotsProvider.makeSlotToPlc(3, mainScheduler, DecisionReq.class, TrackIdResponse.class);

            decisionMaker.getDecisionReqFlux().doOnNext((dec) -> {
                logger.info("Decision {} | {}", dec.getTrackId(), Arrays.toString(dec.getDecision()));
            }).subscribe(slot.getProcessor());

            Flux.from(slot.getProcessor()).doOnNext((resp) -> {
                logger.info("Decision ack from Plc");
                //          phsHandler.handleInput(1, resp);
            }).subscribe();

        } catch (SlotCreationException e) {
            throw new RuntimeException(e);
        }

        try {
            SlotFromPlcUsable<ReportReq, Object> slot = slotsProvider.makeAutoAckSlotFromPlc(4, mainScheduler, ReportReq.class);
            Flux.from(slot.getPublisher()).doOnNext((s) -> {
                logger.info("Sorter report from exit - {} | {}", s.getTrackId(), s.getExit());
            }).subscribe();
        } catch (SlotCreationException e) {
            throw new RuntimeException(e);
        }

        slotsProvider.getUaNotifierSingle().setInit(true);
    }
//    public void start1() {
//        ObjectMapper objectMapper = new ObjectMapper();
//        //Slot 1 trackId gen
//        PtlStation station = new PtlStation("STATION_1");
//        TrackIdsPublisher trackIdsPublisher = new TrackIdsPublisher(logger, mainScheduler);
//        trackIdsPublisher.retrievingDone();
//
//        Flux<TrackId> trackIdFlux = Flux.from(trackIdsPublisher).doOnNext((tid) -> {
//            logger.info("new trackId - {} -> plc", tid.nextTrackId());
//           // slotsProvider.activateSimSlot(1, tid);
//        });
//
//
//
//        try {
//          SlotToPlcUsable<TrackId, TrackIdResponse> slot = slotsProvider.makeSlotToPlc(1, mainScheduler, TrackId.class, TrackIdResponse.class);
////            phsHandler.newSlot(1);
////            phsHandler.getEmitterMap().get(1).getOutputFLux().map(o -> {
////                try {
////                    String json = objectMapper.writeValueAsString(o);
////                    logger.info("Got TID object - {}", json);
////                    return objectMapper.readValue(json, TrackId.class);
////                } catch (JsonProcessingException e) {
////                    logger.info("exception mapping - ", e);
////                }
////                return new TrackId(-1);
////            }).filter(trackId -> {
////                return trackId.getTrackId() != -1;
////            }).doOnNext((tid) -> {
////                logger.info("new trackId - {} -> plc", tid.nextTrackId());
////                //   slotsProvider.activateSimSlot(1, tid);
////            }).subscribe(slot.getProcessor());
//            trackIdFlux.subscribe(slot.getProcessor());
//            Flux.from(slot.getProcessor()).doOnNext((resp) -> {
//                logger.info("Ack from Plc");
//      //          phsHandler.handleInput(1, resp);
//            }).subscribe();
////            phsHandler.handleOutput(1, new TrackIdResponse());
//        } catch (SlotCreationException e) {
//
//            logger.info("jajko = ", e);
//        }
//
//        //Slot 2 Barcode assign / decision
//        try {
//            SlotFromPlcUsable<
//                    BarcodeAssignWithDecisionReq,
//                    BarcodeAssignWithDecisionResp
//                    > slot = slotsProvider.makeTwoDirectionSlotFromPlc(
//                    2,
//                    mainScheduler,
//                    BarcodeAssignWithDecisionReq.class,
//                    BarcodeAssignWithDecisionResp.class);
//
//            Flux.from(slot.getPublisher()).doOnNext((req) -> {
//                logger.info("new barcode assign TID: {} -> BC: {} ", req.getTrackId(), req.getTrackId());
//            }).map((z) -> {
//                return new BarcodeAssignWithDecisionResp((short) (z.getTrackId() % 2 == 0 ? 1002 : 1003));
//            }).doOnNext((a) -> {
//                logger.info("New Decision  response");
//               // slotsProvider.activateSimSlot(2, a);
//            }).subscribe(slot.getSubscriber());
//        } catch (SlotCreationException | NoSuchMethodException e) {
//            logger.info("error = ", e);
//        }
////        }
//        try {
//            SlotFromPlcUsable<BarcodeDecisionReq,
//                    BarcodeDecisionResp> slot = slotsProvider.makeTwoDirectionSlotFromPlc(
//                    3, mainScheduler, BarcodeDecisionReq.class, BarcodeDecisionResp.class
//            );
//            Flux.from(slot.getPublisher()).doOnNext((req) -> {
//                logger.info("new barcode decision Req BC: {} ", req.getBarcode());
//            }).map((z) -> {
//                return new BarcodeDecisionResp((short) (System.currentTimeMillis() % 2 == 0 ? 1004 : 1010));
//            }).doOnNext((a) -> {
//                logger.info("New Decision  response");
//                //  slotsProvider.activateSimSlot(slotId, a);
//            }).subscribe(slot.getSubscriber());
//        } catch (SlotCreationException | NoSuchMethodException e) {
//            logger.info("jajko = ", e);
//        }
//        try {
//            SlotFromPlcUsable<BarcodeDecisionReq,
//                    BarcodeDecisionResp> slot = slotsProvider.makeTwoDirectionSlotFromPlc(
//                    4, mainScheduler, BarcodeDecisionReq.class, BarcodeDecisionResp.class
//            );
//            Flux.from(slot.getPublisher()).doOnNext((req) -> {
//                logger.info("new barcode decision Req BC: {} ", req.getBarcode());
//            }).map((z) -> {
//                return new BarcodeDecisionResp((short) (System.currentTimeMillis() % 2 == 0 ? 1006 : 1010));
//            }).doOnNext((a) -> {
//                logger.info("New Decision  response");
//                //  slotsProvider.activateSimSlot(slotId, a);
//            }).subscribe(slot.getSubscriber());
//
//
//        } catch (SlotCreationException | NoSuchMethodException e) {
//            logger.info("jajko = ", e);
//        }
//
//
//
//
//        try {
//            SlotFromPlcUsable<BarcodeDecisionReq,
//                    Object> slot = slotsProvider.makeAckOnlySlotFromPlc(
//                    5, mainScheduler, BarcodeDecisionReq.class
//            );
//            Flux.from(slot.getPublisher()).doOnNext((req) -> {
//                logger.info("new barcode Station Req BC: {} ", req.getBarcode());
//                station.setActualBarcode(req.getBarcode());
//            }).doOnNext((a) -> {
//                logger.info("New response");
//                //  slotsProvider.activateSimSlot(slotId, a);
//            }).subscribe(slot.getSubscriber());
//        } catch (SlotCreationException e) {
//            logger.info("jajko = ", e);
//        }
//
//
//
//        try {
//            SlotFromPlcUsable<StationReq,
//                    StationResp> slot = slotsProvider.makeTwoDirectionTokenSlotFromPlc(
//                    6, mainScheduler, StationReq.class, StationResp.class
//            );
//            Flux.from(slot.getPublisher()).doOnNext((req) -> {
//                logger.info("new Station Picking req- {}", req.getRequestCode());
//            }).doOnNext((a) -> {
//                if(a.getRequestCode() == 1) {
//                    Thread t = new Thread(() -> {
//                        station.startCompleting();
//                    });
//                    t.setName("PICKING_" + station.getName());
//                    t.start();
//                }
//
//                if(a.getRequestCode() == 2)
//                    station.stopCompleting();
//                //  slotsProvider.activateSimSlot(slotId, a);
//            }).filter((z) -> {
//                if(z.getRequestCode() == 2)
//                    return false;
//                return true;
//            }).subscribe();
//
//            station.getOutputFlux().doOnNext((s) -> {
//                logger.info("jaca koniec");
//            }).subscribe(slot.getSubscriber());
//
//        } catch (SlotCreationException e) {
//            logger.info("jajko = ", e);
//        } catch (NoSuchMethodException e) {
//            throw new RuntimeException(e);
//        }
//
//        try {
//            for (int i = 5; i < 5; i++) {
//                SlotFromPlcUsable<DecisionReqDupa, DecisionResp> slot = slotsProvider.makeTwoDirectionSlotFromPlc(i, mainScheduler, DecisionReqDupa.class, DecisionResp.class);
//                Flux.from(slot.getPublisher()).doOnNext((x) -> {
//                    logger.info("new camera for track id - {} ", x.getTrackId());
//                    try {
//                        Thread.sleep(30);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }).publishOn(mainScheduler).map((z) -> {
//                    return new DecisionResp((short) (z.getTrackId() % 2));
//                }).subscribe(slot.getSubscriber());
//            }
//            slotsProvider.getUaNotifierSingle().setInit(true);
//            for (int i = 21; i < 21; i++) {
//                SlotToPlcUsable<ToPlcResp, ToPlcReq> slot = slotsProvider.makeSlotToPlc(i, mainScheduler, ToPlcResp.class, ToPlcReq.class);
//
//                Flux.interval(Duration.ofMillis(2000000 + (i-20)*50)).onBackpressureDrop().map((z) -> {return new ToPlcResp(z.shortValue());}).doOnNext((s) -> {
//                    logger.info("new req");
//                }).subscribe(slot.getProcessor());
//
//                Flux.from(slot.getProcessor()).log().doOnNext((s) -> {
//                    logger.info("new resp - {}", s.getTrackId());
//                }).subscribe();
//            }
//            mainScheduler.run();
//        } catch (Exception e) {
//            logger.info("a - ", e);
//        }
//    }
}
