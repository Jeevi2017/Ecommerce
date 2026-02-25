package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.ProductUploadEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class KafkaProducerService {

    private static final Logger logger =
            LoggerFactory.getLogger(KafkaProducerService.class);

    private static final String TOPIC_NAME = "product-bulk-upload";

    private final KafkaTemplate<String, ProductUploadEvent> kafkaTemplate;

    // ✅ Constructor Injection (SonarQube compliant)
    public KafkaProducerService(KafkaTemplate<String, ProductUploadEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendProductUploadEvent(ProductUploadEvent event) {
        kafkaTemplate.send(TOPIC_NAME, event.getName(), event);
        logger.info(
                "Sent Product Upload Event to Kafka. Product name: {}",
                event.getName()
        );
    }
}
