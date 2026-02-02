package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.ProductUploadEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {
    private static final String TOPIC_NAME = "product-bulk-upload";

    @Autowired
    private KafkaTemplate<String, ProductUploadEvent> kafkaTemplate;

    public void sendProductUploadEvent(ProductUploadEvent event) {
       
        kafkaTemplate.send(TOPIC_NAME, event.getName(), event);
        System.out.println("Sent Product Upload Event to Kafka: " + event.getName());
    }
}
