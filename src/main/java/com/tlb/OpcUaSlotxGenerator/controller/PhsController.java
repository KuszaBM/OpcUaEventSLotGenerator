package com.tlb.OpcUaSlotxGenerator.controller;

import com.tlb.OpcUaSlotxGenerator.handlers.PhsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PhsController {
    private final PhsHandler phsHandler;
    Logger log = LoggerFactory.getLogger(PhsHandler.class);

    @Autowired
    public PhsController(PhsHandler phsHandler) {
        this.phsHandler = phsHandler;
    }

    @PostMapping("slotCall/{slotId}")
    public String callSlot(@PathVariable int slotId, @RequestBody Object req) {
        log.info("called this {}", slotId);
        phsHandler.handleInput(slotId, req);
        return "OK";
    }
}
