
package com.example.Ecomm.repository.elasticsearch;

import com.example.Ecomm.document.ProductElasticsearch;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductElasticsearchRepository extends ElasticsearchRepository<ProductElasticsearch, String> {

    
}
