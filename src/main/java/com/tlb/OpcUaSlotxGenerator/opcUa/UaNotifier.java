package com.tlb.OpcUaSlotxGenerator.opcUa;


import org.eclipse.milo.opcua.sdk.client.api.subscriptions.UaMonitoredItem;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class UaNotifier {
    Logger logger = LoggerFactory.getLogger(UaNotifier.class);
    private UaResponseListener slot;
    private boolean direction;
    private UaSlotBase slotBase;
    private boolean isListening;
    private boolean wasAnswer;
    private int interval;
    public UaNotifier(boolean direction, UaResponseListener listener, UaSlotBase slotBase, int requestInterval) throws ExecutionException, InterruptedException {
        this.direction = direction;
        this.slot = listener;
        this.slotBase = slotBase;
        this.interval = requestInterval;
    }
    public void startListen() {
           Thread t = new Thread(this::listenForTrigger);
           t.setName(slotBase.getSlotName() == null ? "new T" : slotBase.getSlotName());
           t.start();
    }
    public void listenForTrigger() {
        isListening = true;
        wasAnswer = false;
        while(isListening) {
            try {
                Boolean trig = (Boolean) slotBase.getClient().getAddressSpace().getVariableNode(slotBase.getTokenId()).readValue().getValue().getValue();
                Thread.sleep(interval);
                if(trig == direction) {
                    isListening = false;
                    slot.onTokenChange();
                    logger.info("ACK/RCK from PLC - {}", slotBase.getSlotName());
                    wasAnswer = true;
                }

            } catch (Exception e) {
                logger.info("UA Listener {} - exception - ",slotBase.getSlotName(), e);
                if(!isListening && !wasAnswer) {
                    logger.info("listener loop for {} closed without correct Plc answer, new listener loop has been started", slotBase.getSlotName());
                    startListen();
                } else {
                    logger.info("Problem during processing/Reading Slot but listener was closed correctly");
                }
            }
        }
    }
    public void setReadyForRequest() {
        startListen();
    }
    public void onResponse(UaMonitoredItem item, DataValue value) {
        boolean b = (boolean) value.getValue().getValue();
        if(direction == b)
            slot.onTokenChange();
    }
}

