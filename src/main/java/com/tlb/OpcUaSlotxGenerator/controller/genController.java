package com.tlb.OpcUaSlotxGenerator.controller;

import com.tlb.OpcUaSlotxGenerator.config.OpcUaConfig;
import com.tlb.OpcUaSlotxGenerator.demo.slots.DecisionReq;
import com.tlb.OpcUaSlotxGenerator.demo.slots.ToPlcResp;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.SlotGuiData;
import com.tlb.OpcUaSlotxGenerator.opcUa.UaSlotBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
public class genController {

    private OpcUaSlotsProvider slotsProvider;

    Logger log = LoggerFactory.getLogger(genController.class);

    public genController() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(OpcUaConfig.class);
        this.slotsProvider = ctx.getBean(OpcUaSlotsProvider.class);
    }

    @GetMapping("slots/all")
    public @ResponseBody Flux<SlotGuiData> getContainerList() {
        log.info("slots from provider - {} | {}", slotsProvider.getSlotToAdd().size(), slotsProvider);
        return Flux.fromIterable(slotsProvider.getSlotToAdd().values().stream().map(UaSlotBase::getSlotGuiData).toList());
    }
    @PostMapping("slots/request/{slotId}")
    public String testForce(@RequestBody DecisionReq req, @PathVariable int slotId) {
        if (slotsProvider.getSlotToAdd().get(slotId).getSlotGuiData().getDirection().equals("OUT"))
            slotsProvider.getSlotFromPlc(slotId).forceReq(req);
        return "OK";
    }
    @PostMapping("slots/requestOut/{slotId}")
    public String testForceOut(@RequestBody ToPlcResp req, @PathVariable int slotId) {
        if (slotsProvider.getSlotToAdd().get(slotId).getSlotGuiData().getDirection().equals("IN"))
            slotsProvider.getSlotToPlc(slotId).forceSlotRequest(req);
        return "OK";
    }

//    @GetMapping("slots/all")
//    public String getAll(@RequestBody String a) {
//        while (!slotsProvider.isAfterInit()) {
//            System.out.println("wait to provider up");
//            try {
//                wait(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//        }
//
//        slotsProvider.propagateALlSlots2();
//        return "OK";
//    }
}
