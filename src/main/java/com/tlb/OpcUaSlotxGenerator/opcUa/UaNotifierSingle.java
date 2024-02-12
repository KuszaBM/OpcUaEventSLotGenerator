package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class UaNotifierSingle {
    private static UaNotifierSingle instance;
    Logger logger = LoggerFactory.getLogger(UaNotifierSingle.class);
    private Map<Integer, UaResponseListener> slots;
    private boolean isInit = false;

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }

    private UaNotifierSingle() {
        slots = new HashMap<>();
    }
    public static UaNotifierSingle getInstance() {
        if (instance == null) {
            instance = new UaNotifierSingle();
        }
        return instance;
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
                UaResponseListener slot = slots.get(slotsToRun[i].intValue());
                if(slot == null) {
                    logger.info("no slot with id {} added in PHS", slotsToRun[i].intValue());
                    return;
                }
                activateSlot(slots.get(slotsToRun[i].intValue()));
            }
        }
    }
    public Map<Integer, UaResponseListener> getSlots() {
        return slots;
    }
}
