package com.tlb.OpcUaSlotxGenerator.demo.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class ToPlcResp {
    @OpcUaNode(name = "TID_DATA")
    short trackId;

    public ToPlcResp() {
    }

    public ToPlcResp(short trackId) {
        this.trackId = trackId;
    }

    public short getTrackId() {
        return trackId;
    }

    public void setTrackId(short trackId) {
        this.trackId = trackId;
    }
}
