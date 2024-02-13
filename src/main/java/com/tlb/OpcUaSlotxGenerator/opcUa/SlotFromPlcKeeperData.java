package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class SlotFromPlcKeeperData <Req, Resp> {
    private Publisher<?> publisher;
    private Subscriber<?> subscriber;
    private Req requestReference;
    private Resp respReference;
}
