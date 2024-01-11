package com.tlb.OpcUaSlotxGenerator.config;

import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaClientProvider;
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
    public OpcUaClientProvider opcUaClientProvider() {
        OpcUaClientProvider clientProvider = new  OpcUaClientProvider("opc.tcp://192.168.19.121:4840", "PHS_OPC_COMM", 3);
        return clientProvider;
    }
    @Bean
    public OpcUaSlotsProvider opcUaSlotsProvider() {
        //OpcUaSlotsProvider provider = new OpcUaSlotsProvider("opc.tcp://127.0.0.1:4850/freeopcua/server/", "na", 1);
        OpcUaSlotsProvider provider =
                new
                        OpcUaSlotsProvider(
                        "opc.tcp://192.168.19.121:4840"
                        ,
                        "PHS_OPC_COMM"
                        ,
                        3, opcUaClientProvider());
        UaNotifierSingle u = uaNotifierSingle();
        provider.setUaNotifierSingle(u);

        return provider;
    }

}
