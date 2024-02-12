package com.tlb.OpcUaSlotxGenerator.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaProcedureConfig {
//    @Value("${spring.kafka.bootstrap-servers}")
//    private String bootstrapServer;
//
//    public Map<String, Object> producerConfig() {
//        HashMap<String, Object> props = new HashMap<>();
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
//        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        return props;
//    }
//
//    @Bean
//    public ProducerFactory<String, String> producerFactory() {
//        return new DefaultKafkaProducerFactory<>(producerConfig());
//    }
//
//    @Bean
//    public ServicesConnector servicesConnector(ProducerFactory<String, String> producerFactory) {
//        return ServicesConnector.getInstance(new KafkaTemplate<>(producerFactory));
//    }
}
