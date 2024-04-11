package com.tlb.OpcUaSlotxGenerator.demo.industry;

import reactor.core.publisher.Flux;

public interface WeightingPoint {
    Flux<Float> getResultFlux();
    void setWeighting(boolean isWeighting);
    void newWeightingRequest(short tid);
}
