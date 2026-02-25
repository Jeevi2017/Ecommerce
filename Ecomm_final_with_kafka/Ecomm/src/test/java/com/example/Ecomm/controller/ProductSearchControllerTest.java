package com.example.Ecomm.controller;

import com.example.Ecomm.document.ProductElasticsearch;
import com.example.Ecomm.serviceImpl.ProductElasticsearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductSearchControllerTest {

    @InjectMocks
    private ProductSearchController productSearchController;

    @Mock
    private ProductElasticsearchService productElasticsearchService;

    // ---------- HELPERS ----------

    private ProductElasticsearch mockProduct() {
        ProductElasticsearch product = new ProductElasticsearch();
        product.setId("1");
        product.setName("Test Product");
        return product;
    }

    // ---------- TESTS ----------

    @Test
    void searchProducts() {
        when(productElasticsearchService.searchProducts("phone"))
                .thenReturn(List.of(mockProduct()));

        List<ProductElasticsearch> result =
                productSearchController.searchProducts("phone");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Product", result.get(0).getName());

        verify(productElasticsearchService)
                .searchProducts("phone");
    }

    @Test
    void reindexProducts() {
        doNothing().when(productElasticsearchService)
                .saveAllProductsToElasticsearch();

        ResponseEntity<String> response =
                productSearchController.reindexProducts();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Products re-indexed successfully!", response.getBody());

        verify(productElasticsearchService)
                .saveAllProductsToElasticsearch();
    }
}
