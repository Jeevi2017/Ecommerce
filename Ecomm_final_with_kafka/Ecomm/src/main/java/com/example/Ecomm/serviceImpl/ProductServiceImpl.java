package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.BulkUploadResultDTO;
import com.example.Ecomm.dto.ProductDTO;
import com.example.Ecomm.entitiy.CartItem;
import com.example.Ecomm.entitiy.Category;
import com.example.Ecomm.entitiy.Product;
import com.example.Ecomm.entitiy.ProductSize;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.CartItemRepository;
import com.example.Ecomm.repository.CategoryRepository;
import com.example.Ecomm.repository.ProductRepository;
import com.example.Ecomm.service.ProductService;
import com.example.Ecomm.util.CsvHelper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    // ================= READ =================

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::mapProductToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "Id", id));
        return mapProductToDTO(product);
    }

    // ================= CREATE =================

    @Override
    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = mapDTOToProduct(productDTO);
        Product savedProduct = productRepository.save(product);
        return mapProductToDTO(savedProduct);
    }

    // ================= UPDATE =================

    @Override
    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "Id", id));

        existingProduct.setName(productDTO.getName());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setImages(productDTO.getImages());

        // 🔥 FIX: UPDATE PRICE & STOCK
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setStockQuantity(productDTO.getStockQuantity());

        // ✅ SAFE SIZE UPDATE
        if (productDTO.getAvailableSizes() != null) {
            existingProduct.setAvailableSizes(productDTO.getAvailableSizes());
        }

        if (productDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "Id", productDTO.getCategoryId()));
            existingProduct.setCategory(category);
        } else {
            throw new IllegalArgumentException("Category ID is required for product update.");
        }

        Product updatedProduct = productRepository.save(existingProduct);
        return mapProductToDTO(updatedProduct);
    }

    // ================= CATEGORY =================

    @Override
    @Transactional(readOnly = true)
    public List<ProductDTO> getProductsByCategoryId(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "Id", categoryId));

        return productRepository.findByCategory(category)
                .stream()
                .map(this::mapProductToDTO)
                .collect(Collectors.toList());
    }

    // ================= DELETE =================

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "Id", id));

        List<CartItem> associatedCartItems = cartItemRepository.findByProduct(product);
        if (!associatedCartItems.isEmpty()) {
            cartItemRepository.deleteAll(associatedCartItems);
        }

        productRepository.delete(product);
    }

    // ================= BULK UPLOAD =================

    @Override
    @Transactional
    public BulkUploadResultDTO uploadProductsFromCsv(MultipartFile file) {
        int totalProcessed = 0;
        int addedCount = 0;
        int skippedCount = 0;

        try {
            List<ProductDTO> productDTOs = CsvHelper.csvToProductDTOs(file.getInputStream());
            totalProcessed = productDTOs.size();

            for (ProductDTO productDTO : productDTOs) {
                Category category = categoryRepository.findById(productDTO.getCategoryId()).orElse(null);

                if (category == null) {
                    skippedCount++;
                    continue;
                }

                boolean exists = productRepository
                        .findByNameIgnoreCaseAndCategoryId(
                                productDTO.getName(),
                                productDTO.getCategoryId()
                        ).isPresent();

                if (exists) {
                    skippedCount++;
                } else {
                    Product product = mapDTOToProduct(productDTO);
                    product.setCategory(category);
                    productRepository.save(product);
                    addedCount++;
                }
            }

            return new BulkUploadResultDTO(
                    totalProcessed,
                    addedCount,
                    skippedCount,
                    "CSV processed successfully"
            );

        } catch (IOException e) {
            return new BulkUploadResultDTO(0, 0, 0, "CSV read failed");
        }
    }

    // ================= OTHER =================

    @Override
    @Transactional
    public List<ProductDTO> createMultipleProducts(List<ProductDTO> productDTOs) {
        List<Product> products = productDTOs.stream()
                .map(this::mapDTOToProduct)
                .collect(Collectors.toList());

        return productRepository.saveAll(products)
                .stream()
                .map(this::mapProductToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProductDTO> getProductByName(String name) {
        return productRepository.findByName(name)
                .map(this::mapProductToDTO);
    }

    // ================= MAPPERS =================

    // 🔥 FIX: SEND PRICE & STOCK TO FRONTEND
    private ProductDTO mapProductToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setImages(product.getImages());

        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());

        dto.setAvailableSizes(product.getAvailableSizes());

        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
            dto.setCategoryName(product.getCategory().getName());
        }
        return dto;
    }

    // 🔥 FIX: SAVE PRICE & STOCK TO DB
    private Product mapDTOToProduct(ProductDTO dto) {
        Product product = new Product();
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setImages(dto.getImages());

        product.setPrice(dto.getPrice());
        product.setStockQuantity(dto.getStockQuantity());

        Set<ProductSize> sizes = dto.getAvailableSizes();
        product.setAvailableSizes(sizes);

        if (dto.getCategoryId() != null) {
            Category category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "Id", dto.getCategoryId()));
            product.setCategory(category);
        } else {
            throw new IllegalArgumentException("Category ID is required.");
        }

        return product;
    }
}
