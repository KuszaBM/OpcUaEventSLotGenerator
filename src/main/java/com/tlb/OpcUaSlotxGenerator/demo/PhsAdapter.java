package com.tlb.OpcUaSlotxGenerator.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PhsAdapter {
    private final WebClient webClient;

    @Autowired
    public PhsAdapter(WebClient webClient) {
        this.webClient = webClient;
    }

}
