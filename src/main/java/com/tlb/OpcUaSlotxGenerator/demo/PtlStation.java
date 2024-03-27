package com.tlb.OpcUaSlotxGenerator.demo;

import com.tlb.OpcUaSlotxGenerator.demo.industry.StationResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

public class PtlStation {
    Logger log = LoggerFactory.getLogger(PtlStation.class);
    private final String name;
    private String actualBarcode;
    private boolean completing;
    private Sinks.Many<StationResp> sink;
    private Flux<StationResp> outputFlux;

    public PtlStation(String name) {
        this.name = name;
        this.sink = Sinks.many().unicast().onBackpressureBuffer();
        this.outputFlux = sink.asFlux();
    }
    public void setActualBarcode(String actualBarcode) {
        this.actualBarcode = actualBarcode;
    }

    public void stopCompleting() {
        log.info("Carrier has been taken from station - picking stopped, station cleared");
        completing = false;
    }
    public void startCompleting() {
        int counter = 0;
        if(actualBarcode != null) {
            log.info("Picking started - lighting lamps");
            completing = true;
            while (completing) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                counter++;
                if (counter > 150) {
                    completing = false;
                }
            }
            this.actualBarcode = null;
            sink.tryEmitNext(new StationResp());
        }
        log.info("no barcode on station - !!!");
    }

    public String getName() {
        return name;
    }

    public Flux<StationResp> getOutputFlux() {
        return outputFlux;
    }
}
