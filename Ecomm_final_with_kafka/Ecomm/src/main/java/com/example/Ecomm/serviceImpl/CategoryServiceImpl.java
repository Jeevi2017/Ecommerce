package com.example.Ecomm.serviceImpl;

import com.example.Ecomm.dto.CategoryDTO;
import com.example.Ecomm.entitiy.Category;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.repository.CategoryRepository;
import com.example.Ecomm.service.CategoryService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private static final String ENTITY_NAME = "Category";

    private final CategoryRepository categoryRepository;

    // ✅ Constructor Injection (SonarQube compliant)
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapCategoryToDTO)
                .toList(); // ✅ unmodifiable list
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_NAME, "Id", id));
        return mapCategoryToDTO(category);
    }

    @Override
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        if (categoryRepository.findByName(categoryDTO.getName()).isPresent()) {
            throw new IllegalArgumentException(
                    ENTITY_NAME + " with name '" + categoryDTO.getName() + "' already exists.");
        }

        Category category = mapDTOToCategory(categoryDTO);
        Category savedCategory = categoryRepository.save(category);
        return mapCategoryToDTO(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {

        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_NAME, "Id", id));

        if (!existingCategory.getName().equalsIgnoreCase(categoryDTO.getName())
                && categoryRepository.findByName(categoryDTO.getName()).isPresent()) {

            throw new IllegalArgumentException(
                    ENTITY_NAME + " with name '" + categoryDTO.getName() + "' already exists.");
        }

        existingCategory.setName(categoryDTO.getName());
        existingCategory.setDescription(categoryDTO.getDescription());

        Category updatedCategory = categoryRepository.save(existingCategory);
        return mapCategoryToDTO(updatedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_NAME, "Id", id));

        categoryRepository.delete(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryByName(String name) {
        Category category = categoryRepository.findByName(name)
                .orElseThrow(() ->
                        new ResourceNotFoundException(ENTITY_NAME, "Name", name));
        return mapCategoryToDTO(category);
    }

    // ================= MAPPERS =================

    private CategoryDTO mapCategoryToDTO(Category category) {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(category.getId());
        categoryDTO.setName(category.getName());
        categoryDTO.setDescription(category.getDescription());
        return categoryDTO;
    }

    private Category mapDTOToCategory(CategoryDTO categoryDTO) {
        Category category = new Category();
        category.setId(categoryDTO.getId());
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        return category;
    }
}
