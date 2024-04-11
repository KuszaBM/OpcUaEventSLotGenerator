package com.tlb.OpcUaSlotxGenerator.opcUa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MethodTimeoutCounter {
    Logger logger = LoggerFactory.getLogger(MethodTimeoutCounter.class);
    private final int id;
    private boolean listenTimeout;
    private boolean timeout;
    public MethodTimeoutCounter(int id) {
        this.id = id;
    }

    public boolean startTimeoutCounting() {
        int loopCount = 0;
        listenTimeout = true;
        while (listenTimeout) {
            loopCount++;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(loopCount >= 300) {
                logger.info("METHOD {} did received response - timeout", id);
                if(listenTimeout) {
                    timeout = true;
                    listenTimeout = false;
                }
            }
        }
        return timeout;
    }
    public void stopTimeout() {
        listenTimeout = false;
    }
}
