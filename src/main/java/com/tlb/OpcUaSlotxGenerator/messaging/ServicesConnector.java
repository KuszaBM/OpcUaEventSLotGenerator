package com.tlb.OpcUaSlotxGenerator.messaging;

public class ServicesConnector {
//    private Sinks.Many<DecisionResp> sink;
//    private Flux<DecisionResp> decisionsOutput;
//    Logger logger = LoggerFactory.getLogger(ServicesConnector.class);
//    private int reqTid;
//    private final Otputter otputter;
//
//    private static ServicesConnector instance;
//
//    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    public Flux<DecisionResp> streamDecisions(){
//        return decisionsOutput;
//    }
//    public static ServicesConnector getInstance(KafkaTemplate<String, String> kafkaTemplate) {
//        if(instance == null) {
//            instance = new ServicesConnector(kafkaTemplate);
//        }
//        return instance;
//    }
//
//    private ServicesConnector(KafkaTemplate<String, String> kafkaTemplate) {
//
//        HashMap<String, Object> props = new HashMap<>();
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "0.0.0.0:9092");
//        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(props);
//
//        this.otputter = new Otputter();
//        this.kafkaTemplate = new KafkaTemplate<>(producerFactory);
//        this.sink = Sinks.many().multicast().onBackpressureBuffer();
//        this.decisionsOutput = sink.asFlux().doOnNext((l) -> {
//            logger.info(" new msg - {}", l.getDecision());
//        });
//        Thread t = new Thread(otputter::run);
//        t.start();
//    }
//
//    public void sendToService(DecisionReq decisionReq) {
//        logger.info("sending to kafka new request for tid - {}", decisionReq.getTrackId());
//        try {
//            kafkaTemplate.send("phs", "req_" + decisionReq.getTrackId());
//        } catch (Exception e) {
//            logger.info("exc - ", e);
//        }
//
//    }
//    public void start() {
//        decisionsOutput.subscribe((z) -> {
//            logger.info("SUBED TO DUPA !% 1241");
//        });
//    }
//
//    public Otputter getOtputter() {
//        return otputter;
//    }
//
//    @KafkaListener(topics = "phs",groupId = "x-Id")
//    void listener(String data) {
//        logger.info("decision - Received msg: - " + data);
//        otputter.add(data);
//        sendDecisionToOutput(data);
//    }
//
//    public void sendDecisionToOutput(String data) {
//        String[] dataArray = data.split("_");
//        int z = Integer.parseInt(dataArray[1]);
//        logger.info("afhksafh " + z);
//        short a = (short) z;
//        sink.tryEmitNext(new DecisionResp(a));
//    }
}
