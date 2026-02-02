package com.example.Ecomm.controller;

import com.example.Ecomm.dto.BulkUploadResultDTO; 
import com.example.Ecomm.dto.ProductDTO;
import com.example.Ecomm.dto.ProductUploadEvent; 
import com.example.Ecomm.service.ProductService;
import com.example.Ecomm.serviceImpl.KafkaProducerService;
import com.example.Ecomm.util.CsvHelper;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired 
    private KafkaProducerService kafkaProducerService;

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        List<ProductDTO> products = productService.getAllProducts();
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        ProductDTO product = productService.getProductById(id);
        return new ResponseEntity<>(product, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductDTO> createProduct(@Validated @RequestBody ProductDTO productDTO) {
        ProductDTO createdProduct = productService.createProduct(productDTO);
        return new ResponseEntity<>(createdProduct, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @Validated @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(id, productDTO);
        return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategoryId(@PathVariable Long categoryId) {
        List<ProductDTO> products = productService.getProductsByCategoryId(categoryId);
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ProductDTO>> createMultilpeProducts(@Validated @RequestBody List<ProductDTO> productDTOs) {
        List<ProductDTO> createdProducts = productService.createMultipleProducts(productDTOs);
        return new ResponseEntity<>(createdProducts, HttpStatus.CREATED);
    }
    
    @PostMapping("/upload-csv")
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<String> uploadCsvFile(@RequestParam("file") MultipartFile file) {
        if (!CsvHelper.hasCsvFormat(file)) {
            return new ResponseEntity<>("Please upload a CSV file!", HttpStatus.BAD_REQUEST);
        }
        try {
            List<ProductDTO> productDTOs = CsvHelper.csvToProductDTOs(file.getInputStream());
            String batchId = UUID.randomUUID().toString();
            for (ProductDTO productDTO : productDTOs) {
                ProductUploadEvent event = new ProductUploadEvent();
                event.setBatchId(batchId); 
                event.setName(productDTO.getName());
                event.setDescription(productDTO.getDescription());
                event.setPrice(productDTO.getPrice());
                event.setStockQuantity(productDTO.getStockQuantity());
                event.setCategoryId(productDTO.getCategoryId());
                event.setImageUrls(productDTO.getImages()); 

                kafkaProducerService.sendProductUploadEvent(event);
            }

            return new ResponseEntity<>("Product bulk upload initiated successfully (Batch ID: " + batchId + "). Processing in background.", HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Could not upload the file: " + file.getOriginalFilename() + "! " + e.getMessage(), HttpStatus.EXPECTATION_FAILED);
        }
    }

    
    
    
}

