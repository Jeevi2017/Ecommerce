package com.example.Ecomm.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.Ecomm.document.ProductElasticsearch;
import com.example.Ecomm.serviceImpl.ProductElasticsearchService;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200") 
public class ProductSearchController {

    @Autowired
    private ProductElasticsearchService productElasticsearchService;

    
    @GetMapping("/search")
    public List<ProductElasticsearch> searchProducts(@RequestParam String query) {
        return productElasticsearchService.searchProducts(query);
    }

    @PostMapping("/reindex")
    public ResponseEntity<String> reindexProducts() {
        productElasticsearchService.saveAllProductsToElasticsearch();
        return ResponseEntity.ok("Products re-indexed successfully!");
    }
}
