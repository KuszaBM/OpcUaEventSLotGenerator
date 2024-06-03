package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class FratReportReq {
    private short trackId;
    private short reportValue;

    @OpcUaConstructor
    public FratReportReq(@OpcUaNode(name = "REPORT_TID_DATA") short trackId,@OpcUaNode(name = "REPORT_DATA") short reportValue) {
        this.trackId = trackId;
        this.reportValue = reportValue;
    }

    public short getTrackId() {
        return trackId;
    }

    public void setTrackId(short trackId) {
        this.trackId = trackId;
    }

    public short getReportValue() {
        return reportValue;
    }

    public void setReportValue(short reportValue) {
        this.reportValue = reportValue;
    }
}
