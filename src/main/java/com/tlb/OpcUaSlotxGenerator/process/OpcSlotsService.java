package com.tlb.OpcUaSlotxGenerator.process;

import com.tlb.OpcUaSlotxGenerator.demo.process.OpcProcess;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcSlotsActivator;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaClientProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OpcSlotsService {

    Logger log = LoggerFactory.getLogger(OpcSlotsService.class);
    private final OpcUaSlotsProvider opcUaSlotsProvider;
    private final OpcUaClientProvider opcUaClientProvider;
    private final WebClient webClient;
    private final OpcProcess process;
    private final Thread processThread;
    private final Thread opcClientProviderThread;
    private final Thread opcSlotsProviderThread;
    private Thread opcActivatorThread;

    private OpcSlotsActivator opcSlotsActivator;



    public OpcSlotsService(OpcUaSlotsProvider opcUaSlotsProvider, OpcUaClientProvider opcUaClientProvider, WebClient webClient, OpcProcess process) {
        this.opcUaSlotsProvider = opcUaSlotsProvider;
        this.opcUaClientProvider = opcUaClientProvider;
        this.webClient = webClient;
        opcUaSlotsProvider.setWebClient(webClient);
        this.process = process;

//        this.processThread = new Thread(this.process::start2);
        this.processThread = new Thread(this.process::st11);
        this.processThread.setName("Process_Thread");

        this.opcClientProviderThread = new Thread(this.opcUaClientProvider::startConnection);
        this.opcClientProviderThread.setName("Opc_Client_Provider_Thread");
        this.opcSlotsProviderThread = new Thread(this.opcUaSlotsProvider::initialStart);
        this.opcSlotsProviderThread.setName("Opc_Slots_Provider_Thread");





    }
    @PostConstruct
    public void processStart() {
        log.info("Starting OpcSlotsService...");
        opcClientProviderThread.start();
        while (!this.opcUaClientProvider.isConnected()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        this.opcSlotsActivator = new OpcSlotsActivator(this.opcUaClientProvider, this.opcUaSlotsProvider.getUaNotifierSingle());
        opcSlotsProviderThread.start();
        log.info("1119 dupa jajko - {}", opcUaSlotsProvider.isAfterInit());
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        log.info("1119  11 dupa jajko - {}", opcUaSlotsProvider.isAfterInit());
        while (!opcUaSlotsProvider.isAfterInit()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        processThread.start();
        this.opcActivatorThread = new Thread(() -> {
            opcSlotsActivator.run();
        });
        opcActivatorThread.setName("OPC_METHOD_THREAD");
        opcActivatorThread.start();
    }
}
