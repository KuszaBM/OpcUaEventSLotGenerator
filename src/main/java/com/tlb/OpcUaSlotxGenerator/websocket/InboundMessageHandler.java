package com.tlb.OpcUaSlotxGenerator.websocket;


import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class InboundMessageHandler {
    Logger log = LoggerFactory.getLogger(InboundMessageHandler.class);
    private final OpcUaSlotsProvider slotsProvider;
    private final BlockingQueue<PhsWebsocketMessage<?>> queue = new LinkedBlockingQueue<>();
    private Thread thread;
    @Autowired
    public InboundMessageHandler(OpcUaSlotsProvider slotsProvider) {
        this.slotsProvider = slotsProvider;
        this.thread = new Thread(() -> {
            while (true) {
                try {
                    PhsWebsocketMessage<?> a = queue.take();
                    handle(a);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
    }
    public void addToQueue(PhsWebsocketMessage<?> message) {
        queue.add(message);
    }
    public void handle(PhsWebsocketMessage<?> message) {
        log.info("new message to handle Type: {}", message.type);
        int slotNo = 0;
        if (message.type.contains("REQUEST-SLOT")) {
            slotNo = Integer.parseInt(message.type.split("-")[2]);
            log.info("called request for slot {}", slotNo);
            if (slotsProvider.getSlotToAdd().get(slotNo).getSlotGuiData().getDirection().equals("OUT"))
                slotsProvider.getSlotFromPlc(slotNo).forceReq(message.data);
            if (slotsProvider.getSlotToAdd().get(slotNo).getSlotGuiData().getDirection().equals("IN"))
                slotsProvider.getSlotToPlc(slotNo).forceSlotRequest(message.data);
        }
        if(message.type.contains("ACK-SLOT")) {
            slotNo = Integer.parseInt(message.type.split("-")[2]);
            log.info("called request for slot {}", slotNo);
            if (slotsProvider.getSlotToAdd().get(slotNo).getSlotGuiData().getDirection().equals("IN"))
                slotsProvider.getSlotToPlc(slotNo).onTokenChange();
        }
        if(message.type.contains("RESPONSE-SLOT")) {
            slotNo = Integer.parseInt(message.type.split("-")[2]);
            log.info("called request for slot {}", slotNo);
            if (slotsProvider.getSlotToAdd().get(slotNo).getSlotGuiData().getDirection().equals("IN"))
                slotsProvider.getSlotToPlc(slotNo).forceSlotResponse(message.data);
        }

    }
}
