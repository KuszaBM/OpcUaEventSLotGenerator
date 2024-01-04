package com.tlb.OpcUaSlotxGenerator;

import com.tlb.OpcUaSlotxGenerator.config.OpcUaConfig;
import com.tlb.OpcUaSlotxGenerator.demo.process.Process;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcSlotsFromPlcActivator;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaClientProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.UaNotifierSingle;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@SpringBootApplication
public class OpcUaSlotxGeneratorApplication {
	public static void main(String[] args) {
		SpringApplication.run(OpcUaSlotxGeneratorApplication.class, args);
		ApplicationContext ctx = new AnnotationConfigApplicationContext(OpcUaConfig.class);
		OpcUaSlotsProvider provider = ctx.getBean(OpcUaSlotsProvider.class);
		provider.setUaNotifierSingle(new UaNotifierSingle());
		OpcUaClientProvider providerSlot = ctx.getBean(OpcUaClientProvider.class);
		Logger logger = LoggerFactory.getLogger(OpcUaSlotxGeneratorApplication.class);
		Process p = new Process(provider);
		Thread t = new Thread(p::start);
		t.setName("w1");
		logger.info("opp");
		p.getMainScheduler().schedule(() -> {
			logger.info("po po");
		});
		Thread slot = new Thread(providerSlot::startConnection);
		slot.start();

		OpcSlotsFromPlcActivator slots = new OpcSlotsFromPlcActivator(providerSlot, provider.getUaNotifierSingle());

		Thread pro = new Thread(provider::startConnection);
		pro.start();
		while (!provider.isAfterInit()) {
			try {
				System.out.println("jj1");
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		slots.startingAsk();
		t.start();
		System.out.println("x");
//		Process process = new Process(provider);
//		Thread t = new Thread(process::start);
//		t.start();
		//process.start();
	}

}
