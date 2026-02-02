package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.ProductDTO;
import com.example.Ecomm.dto.ProductUploadEvent;
import com.example.Ecomm.entitiy.Category;
import com.example.Ecomm.entitiy.Product;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.CategoryRepository;
import com.example.Ecomm.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory; 

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
public class KafkaConsumerService {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    private static final String TOPIC_NAME = "product-bulk-upload";
    private static final String GROUP_ID = "product-upload-group";

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @KafkaListener(topics = TOPIC_NAME, groupId = GROUP_ID, containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void listenProductUploadEvents(ProductUploadEvent event) {
        logger.info("Received Product Upload Event from Kafka (Batch ID: {}): {}", event.getBatchId(), event.getName());
        try {
            if (productService.getProductByName(event.getName()).isPresent()) {
                logger.warn("SKIPPED (Duplicate): Product with name '{}' already exists. Batch ID: {}", event.getName(), event.getBatchId());
                return;
            }

            ProductDTO productDTO = mapEventToProductDTO(event);
            productService.createProduct(productDTO);
            logger.info("SUCCESSFULLY ADDED: Product '{}' saved to DB. Batch ID: {}", event.getName(), event.getBatchId());

        } catch (ResourceNotFoundException e) {
            logger.error("Category not found for product '{}'. Batch ID: {}", event.getName(), event.getBatchId(), e);
        } catch (Exception e) {
            logger.error("Failed to process product '{}'. Batch ID: {}", event.getName(), event.getBatchId(), e);
        }
    }

    private ProductDTO mapEventToProductDTO(ProductUploadEvent event) {
        ProductDTO productDTO = new ProductDTO();
        productDTO.setName(event.getName());
        productDTO.setDescription(event.getDescription());
        productDTO.setPrice(event.getPrice());
        productDTO.setStockQuantity(event.getStockQuantity());
        productDTO.setCategoryId(event.getCategoryId());
        productDTO.setImages(event.getImageUrls() != null ? event.getImageUrls() : new ArrayList<>());
        return productDTO;
    }

    private Product convertEventToProductEntity(ProductUploadEvent event) {
        Product product = new Product();
        product.setName(event.getName());
        product.setDescription(event.getDescription());
        product.setPrice(event.getPrice());
        product.setStockQuantity(event.getStockQuantity());
        product.setImages(event.getImageUrls());
        
        if (event.getCategoryId() != null) {
            Category category = categoryRepository.findById(event.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "Id", event.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }
        return product;
    }
}
