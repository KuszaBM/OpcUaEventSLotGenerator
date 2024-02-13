package com.tlb.OpcUaSlotxGenerator.demo.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;

public class ToPlcReq {
    private short trackId;
    @OpcUaConstructor
    public ToPlcReq() {
        trackId = 1;
    }

    public ToPlcReq(short trackId) {
        this.trackId = trackId;
    }

    public short getTrackId() {
        return trackId;
    }

    public void setTrackId(short trackId) {
        this.trackId = trackId;
    }
}
