package com.tlb.OpcUaSlotxGenerator.demo.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class ToPlcResp {
    @OpcUaNode(name = "X_TID_DATA")
    short trackId;

    public ToPlcResp(short trackId) {
        this.trackId = trackId;
    }
}
