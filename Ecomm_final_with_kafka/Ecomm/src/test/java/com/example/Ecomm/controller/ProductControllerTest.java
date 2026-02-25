package com.example.Ecomm.controller;

import com.example.Ecomm.dto.ProductDTO;
import com.example.Ecomm.dto.ProductUploadEvent;
import com.example.Ecomm.service.ProductService;
import com.example.Ecomm.serviceImpl.KafkaProducerService;
import com.example.Ecomm.util.CsvHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @InjectMocks
    private ProductController productController;

    @Mock
    private ProductService productService;

    @Mock
    private KafkaProducerService kafkaProducerService;

    // ---------- HELPERS ----------

    private ProductDTO mockProductDTO() {
        ProductDTO dto = new ProductDTO();
        dto.setId(1L);
        dto.setName("Product");
        dto.setPrice(BigDecimal.valueOf(100)); // ✅ FIX
        dto.setCategoryId(1L);
        return dto;
    }

    // ---------- TESTS ----------

    @Test
    void getAllProducts() {
        when(productService.getAllProducts())
                .thenReturn(List.of(mockProductDTO()));

        ResponseEntity<List<ProductDTO>> response =
                productController.getAllProducts();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getProductById() {
        when(productService.getProductById(1L))
                .thenReturn(mockProductDTO());

        ResponseEntity<ProductDTO> response =
                productController.getProductById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    void createProduct() {
        when(productService.createProduct(any()))
                .thenReturn(mockProductDTO());

        ResponseEntity<ProductDTO> response =
                productController.createProduct(mockProductDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void updateProduct() {
        when(productService.updateProduct(eq(1L), any()))
                .thenReturn(mockProductDTO());

        ResponseEntity<ProductDTO> response =
                productController.updateProduct(1L, mockProductDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void deleteProduct() {
        doNothing().when(productService).deleteProduct(1L);

        ResponseEntity<Void> response =
                productController.deleteProduct(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void getProductsByCategoryId() {
        when(productService.getProductsByCategoryId(1L))
                .thenReturn(List.of(mockProductDTO()));

        ResponseEntity<List<ProductDTO>> response =
                productController.getProductsByCategoryId(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createMultilpeProducts() {
        when(productService.createMultipleProducts(any()))
                .thenReturn(List.of(mockProductDTO()));

        ResponseEntity<List<ProductDTO>> response =
                productController.createMultilpeProducts(
                        List.of(mockProductDTO())
                );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void uploadCsvFile_success() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file",
                "products.csv",
                "text/csv",
                "name,price\nTest,100".getBytes()
        );

        try (MockedStatic<CsvHelper> mockedCsv = mockStatic(CsvHelper.class)) {

            mockedCsv.when(() -> CsvHelper.hasCsvFormat(file))
                    .thenReturn(true);

            mockedCsv.when(() ->
                            CsvHelper.csvToProductDTOs(any(ByteArrayInputStream.class)))
                    .thenReturn(List.of(mockProductDTO()));

            ResponseEntity<String> response =
                    productController.uploadCsvFile(file);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(kafkaProducerService, times(1))
                    .sendProductUploadEvent(any(ProductUploadEvent.class));
        }
    }
}
