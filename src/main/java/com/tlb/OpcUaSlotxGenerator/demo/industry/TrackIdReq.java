package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class TrackIdReq {
    private short trackId;

    @OpcUaConstructor
    public TrackIdReq(@OpcUaNode(name = "TID_DATA") short trackId) {
        this.trackId = trackId;
    }

    public short getTrackId() {
        return trackId;
    }

    public void setTrackId(short trackId) {
        this.trackId = trackId;
    }
}
