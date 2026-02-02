
package com.example.Ecomm.serviceImpl;

import co.elastic.clients.elasticsearch._types.query_dsl.MultiMatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.example.Ecomm.document.ProductElasticsearch;
import com.example.Ecomm.entitiy.Product;
import com.example.Ecomm.repository.ProductRepository;
import com.example.Ecomm.repository.elasticsearch.ProductElasticsearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ProductElasticsearchService {

    private static final Logger logger = LoggerFactory.getLogger(ProductElasticsearchService.class);

    @Autowired
    private ProductElasticsearchRepository productElasticsearchRepository;

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Transactional(readOnly = true)
    public void saveAllProductsToElasticsearch() {
        try {
            List<Product> allProducts = productRepository.findAll();
            List<ProductElasticsearch> elasticProducts = allProducts.stream()
                .map(this::toProductElasticsearch)
                .collect(Collectors.toList());
            
            productElasticsearchRepository.saveAll(elasticProducts);
            logger.info("Successfully synchronized {} products to Elasticsearch.", elasticProducts.size());
        } catch (Exception e) {
            logger.error("Error during product synchronization to Elasticsearch: {}", e.getMessage(), e);
        }
    }

    
    public List<ProductElasticsearch> searchProducts(String query) {
        
        String lowercaseQuery = query.toLowerCase();

        Query multiMatchQuery = MultiMatchQuery.of(m -> m
            .fields("name", "description", "category")  
            .query(lowercaseQuery)
            .fuzziness("AUTO") 
            .prefixLength(2)                  
        )._toQuery();

        
        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(multiMatchQuery)
            .build();
        
        
        SearchHits<ProductElasticsearch> searchHits = elasticsearchOperations.search(nativeQuery, ProductElasticsearch.class);
        
        
        return searchHits.getSearchHits().stream()
            .map(searchHit -> searchHit.getContent())
            .collect(Collectors.toList());
    }

    
    private ProductElasticsearch toProductElasticsearch(Product product) {
        ProductElasticsearch elasticProduct = new ProductElasticsearch();
        elasticProduct.setId(String.valueOf(product.getId()));
        elasticProduct.setName(product.getName());
        elasticProduct.setDescription(product.getDescription());
        elasticProduct.setImages(product.getImages());
        elasticProduct.setPrice(product.getPrice());
        elasticProduct.setCategory(product.getCategory().getName().toLowerCase());
        elasticProduct.setStockQuantity(product.getStockQuantity());
        return elasticProduct;
    }
}
