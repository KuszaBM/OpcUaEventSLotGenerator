package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class UaNotifierSingle {
    Logger logger = LoggerFactory.getLogger(UaNotifierSingle.class);
    private Set<UaResponseListener> slots;
    private Map<Integer, UaResponseListener> slots2;
    private OpcUaClient client;
    private int count;

    public UaNotifierSingle() {
        slots = new HashSet<>();
        slots2 = new HashMap<>();
    }

    public void setClient(OpcUaClient client) {
        this.client = client;
    }

    public void addSlotToNotifier(UaResponseListener slot) {
        slots.add(slot);
        slots2.put(slot.getSlotId(), slot);
    }
    public void startListeningOnSLot(UaResponseListener slot) {
        slot.setListening(true);
    }
    public void stopListeningOnSLot(UaResponseListener slot) {
        slot.setListening(false);
    }
    private void activateSlot(UaResponseListener slot) {
        logger.info("trig for slot - {} ", slot.getName());
        slot.onTokenChange();
    }
    private boolean isActivated(UaResponseListener slot) {
        try {
            Boolean trig = (Boolean) client.getAddressSpace().getVariableNode(slot.getTokenNode()).readValue().getValue().getValue();
            if(trig == slot.getDirection()) {
                count++;
                stopListeningOnSLot(slot);
                activateSlot(slot);
                logger.info("ACK/RCK from PLC - {}", slot.getName());
                //wasAnswer = true;
                return true;
            }
        } catch (UaException e) {
            logger.info("exception while reading Slot Token Value - ", e);
            return false;
        }
        return false;
    }
    public void runByMethod(Short[] slots) {
        for(int i = 0; i < slots.length; i++) {
            if(slots[i] != -1) {
                logger.info("Activating slot {}", slots[i]);
                activateSlot(slots2.get(slots[i].intValue()));
            }
        }
    }
    public void run() {
        logger.info("notifier running - slots reported {}", slots.size());
        for(UaResponseListener toInitSlot : slots) {
            toInitSlot.setListening(toInitSlot.getDirection());
        }
        while (true) {
            this.count = 0;
            int x = (int) System.currentTimeMillis();
            for(UaResponseListener slot : slots) {
                if(slot.isListening()) {
                    //logger.info("waiting for PLC trigger on slot  - {}", slot.getName());
                    isActivated(slot);
                }
            }
            int z = (int) System.currentTimeMillis();
           // logger.info("loop time = {} | slots activated = {}", z - x, count);
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
        }
    }
}
