package com.tlb.OpcUaSlotxGenerator.demo.industry;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class FratCameraReq {
    private short trackId;
    private String barcode;

    @OpcUaConstructor
    public FratCameraReq(@OpcUaNode(name = "CAMERA_TID_DATA") short trackId,@OpcUaNode(name = "CAMERA_BARCODE_DATA") String barcode) {
        this.trackId = trackId;
        this.barcode = barcode;
    }

    public short getTrackId() {
        return trackId;
    }

    public void setTrackId(short trackId) {
        this.trackId = trackId;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}
