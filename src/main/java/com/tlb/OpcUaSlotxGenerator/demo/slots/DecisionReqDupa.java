package com.tlb.OpcUaSlotxGenerator.demo.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class DecisionReqDupa {
    private int trackId;
    private String barcode;

    @OpcUaConstructor
    public DecisionReqDupa(@OpcUaNode(name = "CAM_TID_DATA")short trackId) {
        this.trackId = trackId;
        this.barcode = "BC" + trackId + "00" + trackId%2 + "DSK";
    }

    public DecisionReqDupa() {
    }


    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public DecisionReqDupa(int trackId, String barcode) {
        this.trackId = trackId;
        this.barcode = barcode;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
