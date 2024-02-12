package com.tlb.OpcUaSlotxGenerator;

import com.tlb.OpcUaSlotxGenerator.demo.slots.DecisionResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Otputter implements Runnable {
    Logger logger = LoggerFactory.getLogger(Otputter.class);
    private LinkedBlockingQueue<String> q;
    private Sinks.Many<DecisionResp> sink;
    private final Flux<DecisionResp> decisionsOutput;
    public Otputter() {
        this.q = new LinkedBlockingQueue<>();
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.decisionsOutput = sink.asFlux().doOnNext((l) -> {
            logger.info(" new msg - {}", l.getDecision());
        });
    }

    public Flux<DecisionResp> getDecisionsOutput() {
        return decisionsOutput;
    }
    public void add(String data) {
        logger.info("added nee to que");
        q.add(data);
    }
    @Override
    public void run() {
        while (true) {
            try {
                String s = q.take();
                if(s == null) {
                    logger.info("dupso 153 ");
                } else {
                    String[] dataArray = s.split("_");
                    int z = Integer.parseInt(dataArray[1]);
                    logger.info("ahuha mama - {}", z);
                    short a = (short) z;
                    DecisionResp resp = new DecisionResp(a);
                    sink.tryEmitNext(resp);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
