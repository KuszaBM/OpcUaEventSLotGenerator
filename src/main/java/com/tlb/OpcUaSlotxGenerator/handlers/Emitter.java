package com.tlb.OpcUaSlotxGenerator.handlers;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class Emitter<T> {
    private final Sinks.Many<T> sink;
    private final Flux<T> outputFLux;

    public Emitter() {
        this.sink = Sinks.many().unicast().onBackpressureBuffer();
        this.outputFLux = sink.asFlux();
    }

    public Sinks.Many<T> getSink() {
        return sink;
    }

    public Flux<T> getOutputFLux() {
        return outputFLux;
    }
    public void emit(T req) {
        sink.tryEmitNext(req);
    }
}
