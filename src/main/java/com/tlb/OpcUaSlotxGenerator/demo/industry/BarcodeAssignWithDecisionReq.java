package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class BarcodeAssignWithDecisionReq {
    private int trackId;
    private String barcode;
    @OpcUaConstructor
    public BarcodeAssignWithDecisionReq(@OpcUaNode(name = "TID_DATA")short trackId, @OpcUaNode(name = "CAM_BARCODE_DATA") String barcode) {
        this.trackId = trackId;
        this.barcode = barcode;
    }

    public BarcodeAssignWithDecisionReq() {
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
