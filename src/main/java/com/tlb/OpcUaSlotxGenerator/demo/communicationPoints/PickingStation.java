package com.tlb.OpcUaSlotxGenerator.demo.communicationPoints;

import com.tlb.OpcUaSlotxGenerator.demo.PtlStation;
import com.tlb.OpcUaSlotxGenerator.demo.industry.BarcodeDecisionReq;
import com.tlb.OpcUaSlotxGenerator.demo.industry.StationReq;
import com.tlb.OpcUaSlotxGenerator.demo.industry.StationResp;
import com.tlb.OpcUaSlotxGenerator.exceptions.SlotCreationException;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper.SlotFromPlcUsable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;

public class PickingStation {
    Logger logger;
    OpcUaSlotsProvider slotsProvider;
    Scheduler mainScheduler;
    int slot1Id;
    int slot2Id;
    String name;
    public PickingStation(String name, int slot1Id, int slot2Id, Scheduler mainScheduler, OpcUaSlotsProvider slotsProvider) {


        this.logger = LoggerFactory.getLogger("Station_"+ name);
        this.slot1Id = slot1Id;
        this.slot2Id = slot2Id;
        this.name = name;
        this.slotsProvider = slotsProvider;
        this.mainScheduler = mainScheduler;
    }

    public void register() {
        PtlStation station = new PtlStation(name);
        try {
            SlotFromPlcUsable<BarcodeDecisionReq,
                    Object> slot = slotsProvider.makeAckOnlySlotFromPlc(
                    slot1Id, mainScheduler, BarcodeDecisionReq.class
            );
            Flux.from(slot.getPublisher()).doOnNext((req) -> {
                logger.info("new barcode Station Req BC: {} ", req.getBarcode());
                station.setActualBarcode(req.getBarcode());
            }).doOnNext((a) -> {
                logger.info("New response");
                //  slotsProvider.activateSimSlot(slotId, a);
            }).subscribe(slot.getSubscriber());
        } catch (SlotCreationException e) {
            logger.info("jajko = ", e);
        }

        try {
            SlotFromPlcUsable<StationReq,
                    StationResp> slot = slotsProvider.makeTwoDirectionTokenSlotFromPlc(
                    slot2Id, mainScheduler, StationReq.class, StationResp.class
            );
            Flux.from(slot.getPublisher()).doOnNext((req) -> {
                logger.info("new Station Picking req- {}", req.getRequestCode());
            }).doOnNext((a) -> {
                if(a.getRequestCode() == 1) {
                    Thread t = new Thread(() -> {
                        station.startCompleting();
                    });
                    t.setName("PICKING_" + station.getName());
                    t.start();
                }

                if(a.getRequestCode() == 2)
                    station.stopCompleting();
                //  slotsProvider.activateSimSlot(slotId, a);
            }).filter((z) -> {
                if(z.getRequestCode() == 2)
                    return false;
                return true;
            }).subscribe();

            station.getOutputFlux().doOnNext((s) -> {
                logger.info("jaca koniec");
            }).subscribe(slot.getSubscriber());

        } catch (SlotCreationException e) {
            logger.info("jajko = ", e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
