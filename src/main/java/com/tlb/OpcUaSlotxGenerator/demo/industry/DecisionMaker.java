package com.tlb.OpcUaSlotxGenerator.demo.industry;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class DecisionMaker {
    Flux<DecisionReq> decisionReqFlux;
    Sinks.Many<DecisionReq> decisionSinks;

    public DecisionMaker() {
        this.decisionSinks = Sinks.many().unicast().onBackpressureBuffer();
        this.decisionReqFlux = decisionSinks.asFlux();
    }

    public void makeDecision(int trackId) {
        int rand = (int) ((Math.random() * (5 - 1)) + 1);
        boolean[] decision = new boolean[5];
        decision[rand - 1] = true;
        decisionSinks.tryEmitNext(new DecisionReq(decision, (short) trackId));
    }

    public Flux<DecisionReq> getDecisionReqFlux() {
        return decisionReqFlux;
    }
}
