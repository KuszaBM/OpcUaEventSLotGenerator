package com.tlb.OpcUaSlotxGenerator.demo.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class DecisionReq {
    private int trackId;

    @OpcUaConstructor
    public DecisionReq(@OpcUaNode(name = "CAM_TID_DATA")short trackId) {
        this.trackId = trackId;
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }
}
