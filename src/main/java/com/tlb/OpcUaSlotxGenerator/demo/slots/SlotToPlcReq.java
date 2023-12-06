package com.tlb.OpcUaSlotxGenerator.demo.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class SlotToPlcReq {
    @OpcUaNode(name = "TRACKER_DATA_SLOT_0")
    private short tracker;
    @OpcUaNode(name = "ARRAY_OF_BOOL_DATA_SLOT_0")
    private boolean[] arrayOfBool;

    public SlotToPlcReq(short tracker, boolean[] arrayOfBool) {
        this.tracker = tracker;
        this.arrayOfBool = arrayOfBool;
    }
}
