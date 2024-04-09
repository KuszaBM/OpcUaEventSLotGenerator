package com.tlb.OpcUaSlotxGenerator.demo.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class ReportReq {
    private short trackId;
    private short exit;
    public ReportReq() {
    }
    @OpcUaConstructor
    public ReportReq(@OpcUaNode(name = "SORTER_REPORT_TID_DATA") short trackId,@OpcUaNode(name = "SORTER_REPORT_DATA") short exit) {
        this.trackId = trackId;
        this.exit = exit;
    }

    public short getTrackId() {
        return trackId;
    }

    public void setTrackId(short trackId) {
        this.trackId = trackId;
    }

    public short getExit() {
        return exit;
    }

    public void setExit(short exit) {
        this.exit = exit;
    }
}
