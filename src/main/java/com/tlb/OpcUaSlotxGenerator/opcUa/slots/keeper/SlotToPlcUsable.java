package com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper;

import org.reactivestreams.Processor;

public class SlotToPlcUsable <Req, Resp>{
    Processor<Req, Resp> processor;
    Class<Req> reqClass;
    Class<Resp> respClass;

    public SlotToPlcUsable(Processor<Req, Resp> processor, Class<Req> reqClass, Class<Resp> respClass) {
        this.processor = processor;
        this.reqClass = reqClass;
        this.respClass = respClass;
    }

    public Processor<Req, Resp> getProcessor() {
        return processor;
    }

    public void setProcessor(Processor<Req, Resp> processor) {
        this.processor = processor;
    }

    public Class<Req> getReqClass() {
        return reqClass;
    }

    public void setReqClass(Class<Req> reqClass) {
        this.reqClass = reqClass;
    }

    public Class<Resp> getRespClass() {
        return respClass;
    }

    public void setRespClass(Class<Resp> respClass) {
        this.respClass = respClass;
    }
}
