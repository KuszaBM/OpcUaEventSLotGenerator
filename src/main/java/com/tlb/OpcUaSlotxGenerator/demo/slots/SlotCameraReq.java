package com.tlb.OpcUaSlotxGenerator.demo.slots;

import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaConstructor;
import com.tlb.OpcUaSlotxGenerator.opcUa.annnotations.OpcUaNode;

public class SlotCameraReq {
    private short trackId;
    private String barcode;

    @OpcUaConstructor
    public SlotCameraReq(@OpcUaNode(name = "TID_DATA") short trackId,@OpcUaNode(name = "BC_DATA") String barcode) {
        this.trackId = trackId;
        this.barcode = barcode;
    }

    public short getTrackId() {
        return trackId;
    }

    public String getBarcode() {
        return barcode;
    }
}
