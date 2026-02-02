package com.example.Ecomm;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.example.Ecomm.serviceImpl.ProductElasticsearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.Ecomm.repository")
@EnableElasticsearchRepositories(basePackages = "com.example.Ecomm.repository.elasticsearch")

public class EcommApplication {

    private static final Logger logger = LoggerFactory.getLogger(EcommApplication.class);

    @Autowired
    private ProductElasticsearchService productElasticsearchService;

	public static void main(String[] args) {
		SpringApplication.run(EcommApplication.class, args);
	}
	@Bean
    public CommandLineRunner synchronizeData() {
        return args -> {
            logger.info("Starting synchronization of product data to Elasticsearch...");
            productElasticsearchService.saveAllProductsToElasticsearch();
            logger.info("Synchronization complete.");
        };
    }

}
