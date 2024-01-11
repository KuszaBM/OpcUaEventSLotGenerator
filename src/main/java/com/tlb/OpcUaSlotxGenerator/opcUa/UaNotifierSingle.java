package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class UaNotifierSingle {
    Logger logger = LoggerFactory.getLogger(UaNotifierSingle.class);
    private Map<Integer, UaResponseListener> slots;
    private boolean isInit = false;

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }

    public UaNotifierSingle() {
        slots = new HashMap<>();
    }

    public void addSlotToNotifier(UaResponseListener slot) {
        slots.put(slot.getSlotId(), slot);
    }
    public void startListeningOnSLot(UaResponseListener slot) {
        slot.setListening(true);
    }
    private void activateSlot(UaResponseListener slot) {
        logger.info("trig for slot - {} ", slot.getName());
        slot.onTokenChange();
    }
    public void runByMethod(Short[] slotsToRun) {
        for(int i = 0; i < slotsToRun.length; i++) {
            if(slotsToRun[i] != -1) {
                logger.info("Activating slot {}", slotsToRun[i]);
                activateSlot(slots.get(slotsToRun[i].intValue()));
            }
        }
    }
    public Map<Integer, UaResponseListener> getSlots() {
        return slots;
    }
}
