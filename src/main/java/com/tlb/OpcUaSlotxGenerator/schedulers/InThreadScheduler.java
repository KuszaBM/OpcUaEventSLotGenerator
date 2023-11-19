package com.tlb.OpcUaSlotxGenerator.schedulers;

import org.slf4j.LoggerFactory;

public final class InThreadScheduler extends AbstractSingleQueueBaseScheduler implements Runnable {

	public InThreadScheduler(String instance) {
		super(instance, LoggerFactory.getLogger(InThreadScheduler.class));
	}

	@Override
	public final void run() {
		try {
			loop();
		} catch (InterruptedException e) {
			logger.error("Thread interrupted", e);
		}
	}
}
