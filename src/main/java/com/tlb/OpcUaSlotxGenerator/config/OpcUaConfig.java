package com.tlb.OpcUaSlotxGenerator.config;

import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpcUaConfig {
    @Bean
    public OpcUaSlotsProvider opcUaSlotsProvider() {
        OpcUaSlotsProvider provider = new OpcUaSlotsProvider("opc.tcp://172.27.110.10:4840", "PHS_OPC_COMM", 3);
        return provider;
    }
}
