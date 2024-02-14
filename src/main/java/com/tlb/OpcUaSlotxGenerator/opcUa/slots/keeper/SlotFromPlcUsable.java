package com.tlb.OpcUaSlotxGenerator.opcUa.slots.keeper;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

public class SlotFromPlcUsable<Req, Resp> {
    private Publisher<Req> publisher;
    private Subscriber<Resp> subscriber;
    private Class<Req> requestClass;
    private Class<Resp> respClass;

    public SlotFromPlcUsable(Publisher<Req> publisher, Subscriber<Resp> subscriber, Class<Req> requestReference, Class<Resp> respReference) {
        this.publisher = publisher;
        this.subscriber = subscriber;
        this.requestClass = requestReference;
        this.respClass = respReference;
    }

    public Publisher<Req> getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher<Req> publisher) {
        this.publisher = publisher;
    }

    public Subscriber<Resp> getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber<Resp> subscriber) {
        this.subscriber = subscriber;
    }

    public Class<Req> getRequestClass() {
        return requestClass;
    }

    public void setRequestClass(Class<Req> requestClass) {
        this.requestClass = requestClass;
    }

    public Class<Resp> getRespClass() {
        return respClass;
    }

    public void setRespClass(Class<Resp> respClass) {
        this.respClass = respClass;
    }
}
