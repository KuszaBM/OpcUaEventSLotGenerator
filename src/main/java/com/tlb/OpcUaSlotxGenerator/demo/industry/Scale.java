package com.tlb.OpcUaSlotxGenerator.demo.industry;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class Scale implements WeightingPoint{

    private Sinks.Many<Float> sink;
    private Flux<Float> floatFlux;
    public Scale() {
        sink = Sinks.many().unicast().onBackpressureBuffer();
        floatFlux = sink.asFlux();
    }
    @Override
    public Flux<Float> getResultFlux() {
        return floatFlux;
    }

    @Override
    public void setWeighting(boolean isWeighting) {

    }

    @Override
    public void newWeightingRequest(short tid) {
        float weight = (float) tid;
        weight = 1000.0f + weight;
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        sink.tryEmitNext(weight);
    }
}
