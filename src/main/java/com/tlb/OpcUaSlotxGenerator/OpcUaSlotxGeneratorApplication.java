package com.tlb.OpcUaSlotxGenerator;

import com.tlb.OpcUaSlotxGenerator.config.OpcUaConfig;
import com.tlb.OpcUaSlotxGenerator.config.WebClientConfig;
import com.tlb.OpcUaSlotxGenerator.demo.process.Process;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcSlotsActivator;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaClientProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.UaNotifierSingle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class OpcUaSlotxGeneratorApplication {
	public static void main(String[] args) {
		SpringApplication.run(OpcUaSlotxGeneratorApplication.class, args);
		ApplicationContext ctx = new AnnotationConfigApplicationContext(OpcUaConfig.class);
//		ApplicationContext ctx2 = new AnnotationConfigApplicationContext(KafkaProcedureConfig.class);
		OpcUaSlotsProvider provider = ctx.getBean(OpcUaSlotsProvider.class);
		ApplicationContext ctx3 = new AnnotationConfigApplicationContext(WebClientConfig.class);
		WebClient webClient = ctx3.getBean(WebClient.class);
		if(webClient == null)
			System.out.println("web jajco");
		provider.setWebClient(webClient);
//		ServicesConnector servicesConnector = ctx2.getBean(ServicesConnector.class);
		provider.setUaNotifierSingle(UaNotifierSingle.getInstance());
		OpcUaClientProvider providerSlot = ctx.getBean(OpcUaClientProvider.class);
		Logger logger = LoggerFactory.getLogger(OpcUaSlotxGeneratorApplication.class);
//		Process p = new Process(provider, );
		Process p = new Process(provider);
		Thread t = new Thread(p::start);
		t.setName("w1");
		Thread slot = new Thread(providerSlot::startConnection);
		slot.start();
		while (!providerSlot.isConnected()) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		OpcSlotsActivator slots = new OpcSlotsActivator(providerSlot, provider.getUaNotifierSingle());
		Thread pro = new Thread(provider::initialStart, "PLC");
		pro.start();
		//provider.initialStart();
		logger.info("provider done");
		while (!provider.isAfterInit()) {
			logger.info("provider not done");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		t.start();
		slots.run();
//		Thread az = new Thread(() -> {
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
//            logger.info("!!!!!!!!!!!!!!! test slot 1 call");
//			Short[] ac = new Short[1];
//			ac[0] = 1;
//			provider.getUaNotifierSingle().runByMethod(ac);
//		});
//		az.start();
	}
}
