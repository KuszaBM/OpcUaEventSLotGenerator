package com.tlb.OpcUaSlotxGenerator.config;

import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaClientProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.OpcUaSlotsProvider;
import com.tlb.OpcUaSlotxGenerator.opcUa.UaNotifierSingle;
import com.tlb.OpcUaSlotxGenerator.websocket.SinksHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class OpcUaConfig {
    @Bean
    public SinksHolder sinksHolder() {
        return SinksHolder.getInstance();
    }

    @Bean
    public UaNotifierSingle uaNotifierSingle() {
        return UaNotifierSingle.getInstance(false);
    }
    @Bean
    public OpcUaClientProvider opcUaClientProvider() {
        OpcUaClientProvider clientProvider = OpcUaClientProvider.getInstance(
                "opc.tcp://192.168.19.121:4840",
                "\"PHS_COMM\"",
                3,
                false
        );
//        OpcUaClientProvider clientProvider = OpcUaClientProvider.getInstance(
//         "opc.tcp://127.0.0.1:12686/milo/discovery",
//                "PlcSim/slots",
//                2,
//                true
//        );
        return clientProvider;
    }
    @Bean
    public OpcUaSlotsProvider opcUaSlotsProvider() {
//        OpcUaSlotsProvider provider =
//                OpcUaSlotsProvider.getInstance(
//                        "opc.tcp://127.0.0.1:12686/milo/discovery"
//                        ,
//                        "PlcSim/slots"
//                        ,
//                        2, true, opcUaClientProvider(), sinksHolder());
        OpcUaSlotsProvider provider =
                OpcUaSlotsProvider.getInstance(
                        "opc.tcp://192.168.19.121:4840"
                        ,
                        "\"PHS_COMM\""
                        ,
                        3, false, opcUaClientProvider(), sinksHolder());
        UaNotifierSingle u = uaNotifierSingle();
        provider.setUaNotifierSingle(u);

        return provider;
    }

}
