package com.tlb.OpcUaSlotxGenerator.config;

import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.UaNotifierSingle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpcUaConfig {

    @Bean
    public UaNotifierSingle uaNotifierSingle() {
        return new UaNotifierSingle();
    }
    @Bean
    public OpcUaSlotsProvider opcUaSlotsProvider() {
        OpcUaSlotsProvider provider = new OpcUaSlotsProvider("opc.tcp://192.168.19.209:4840", "PHS_OPC_COMM", 3);
        UaNotifierSingle u = uaNotifierSingle();
        u.setClient(provider.getClient());
        provider.setUaNotifierSingle(u);

        return provider;
    }
}
