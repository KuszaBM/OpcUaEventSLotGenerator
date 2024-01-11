package com.tlb.OpcUaSlotxGenerator.demo.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class ToPlcResp {
    @OpcUaNode(name = "X_TID_DATA")
    short trackId;
    int sourceSlot;

    public int getSourceSlot() {
        return sourceSlot;
    }

    public ToPlcResp(short trackId, int sourceSlot) {
        this.trackId = trackId;
        this.sourceSlot = sourceSlot;
    }
}
