package com.tlb.OpcUaSlotxGenerator.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tlb.OpcUaSlotxGenerator.config.OpcUaConfig;
import com.tlb.OpcUaSlotxGenerator.demo.slots.ToPlcResp;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.UaSlotBase;
import com.tlb.OpcUaSlotxGenerator.opcUa.slots.gui.SlotGuiData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
public class GeneralController {

    private OpcUaSlotsProvider slotsProvider;

    Logger log = LoggerFactory.getLogger(GeneralController.class);

    public GeneralController() {
        ApplicationContext ctx = new AnnotationConfigApplicationContext(OpcUaConfig.class);
        this.slotsProvider = ctx.getBean(OpcUaSlotsProvider.class);
    }

    @GetMapping("slots/all")
    public @ResponseBody Flux<SlotGuiData> getContainerList() {
        log.info("slots from provider - {} | {}", slotsProvider.getSlotToAdd().size(), slotsProvider);
        return Flux.fromIterable(slotsProvider.getSlotToAdd().values().stream().map(UaSlotBase::getSlotGuiData).toList());
    }
    @PostMapping("slots/request/{slotId}")
    public String testForce(@RequestBody Object req, @PathVariable int slotId) {
        log.info("CallingSlot {}", slotId);
        if (slotsProvider.getSlotToAdd().get(slotId).getSlotGuiData().getDirection().equals("OUT"))
            slotsProvider.getSlotFromPlc(slotId).forceReq(req);
        return "OK";
    }
    @PostMapping("slots/requestOut/{slotId}")
    public String testForceOut(@RequestBody ToPlcResp req, @PathVariable int slotId) {
        if (slotsProvider.getSlotToAdd().get(slotId).getSlotGuiData().getDirection().equals("IN")) {
            slotsProvider.getSlotToPlc(slotId).forceSlotRequest(req);
            ObjectMapper mapper = new ObjectMapper();
            try {
                log.info("new force request - {}", mapper.writeValueAsString(req));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        return "OK";
    }
    @PostMapping("slots/requestOut/forceResp/{slotId}")
    public String testForceIn(@RequestBody Object resp, @PathVariable int slotId) {
            if (slotsProvider.getSlotToAdd().get(slotId).getSlotGuiData().getDirection().equals("IN")) {
                log.info("new force unlock slot {}", slotId);
                slotsProvider.getSlotToPlc(slotId).forceSlotResponse(resp);
            }
            return "OK";
    }
    @PostMapping("slots/requestOut/ackSlot/{slotId}")
    public String testForceIn(@PathVariable int slotId) {
        if (slotsProvider.getSlotToAdd().get(slotId).getSlotGuiData().getDirection().equals("IN")) {
            log.info("new ack slot {}", slotId);
            slotsProvider.getSlotToPlc(slotId).onTokenChange();
        }
        return "OK";
    }
}


