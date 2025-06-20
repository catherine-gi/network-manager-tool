package com.cath.producer_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public void sendMessage(String topic, Object message) {
        try {
            System.out.println("Sending to Kafka topic: " + topic);
            System.out.println("Payload: " + message);
            String jsonMessage = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, jsonMessage);
        } catch (Exception e) {
            System.err.println("Kafka send failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send message to Kafka from Producer", e);
        }
    }

}