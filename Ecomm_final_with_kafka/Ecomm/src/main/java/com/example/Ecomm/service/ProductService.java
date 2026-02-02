package com.example.Ecomm.service;

import com.example.Ecomm.dto.BulkUploadResultDTO;
import com.example.Ecomm.dto.ProductDTO;
import java.util.List;
import java.util.Optional;

import org.springframework.web.multipart.MultipartFile;

public interface ProductService {

    List<ProductDTO> getAllProducts();

    ProductDTO getProductById(Long id);

    ProductDTO createProduct(ProductDTO productDTO);

    ProductDTO updateProduct(Long id, ProductDTO productDTO);

    void deleteProduct(Long id);

    List<ProductDTO> getProductsByCategoryId(Long categoryId);

    List<ProductDTO> createMultipleProducts(List<ProductDTO> productDTOs);

    BulkUploadResultDTO uploadProductsFromCsv(MultipartFile file);

    Optional<ProductDTO> getProductByName(String name);
}
