package com.tlb.OpcUaSlotxGenerator;

import com.tlb.OpcUaSlotxGenerator.config.OpcUaConfig;
import com.tlb.OpcUaSlotxGenerator.demo.process.Process;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
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
		System.out.println("supa 12");
		Process process = new Process(provider);
		process.start();
	}

}
