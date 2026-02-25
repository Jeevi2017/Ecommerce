package com.example.Ecomm.controller;

import com.example.Ecomm.dto.CategoryDTO;
import com.example.Ecomm.exception.ResourceNotFoundException;
import com.example.Ecomm.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @InjectMocks
    private CategoryController categoryController;

    @Mock
    private CategoryService categoryService;

    // ---------- HELPERS ----------

    private CategoryDTO mockCategoryDTO() {
        CategoryDTO dto = new CategoryDTO();
        dto.setId(1L);
        dto.setName("Electronics");
        return dto;
    }

    // ---------- TESTS ----------

    @Test
    void getAllCategories() {
        when(categoryService.getAllCategories())
                .thenReturn(List.of(mockCategoryDTO()));

        ResponseEntity<List<CategoryDTO>> response =
                categoryController.getAllCategories();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getCategoryById() {
        when(categoryService.getCategoryById(1L))
                .thenReturn(mockCategoryDTO());

        ResponseEntity<CategoryDTO> response =
                categoryController.getCategoryById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Electronics", response.getBody().getName());
    }

    @Test
    void createCategory() {
        when(categoryService.createCategory(any()))
                .thenReturn(mockCategoryDTO());

        ResponseEntity<CategoryDTO> response =
                categoryController.createCategory(mockCategoryDTO());

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void updateCategory() {
        when(categoryService.updateCategory(eq(1L), any()))
                .thenReturn(mockCategoryDTO());

        ResponseEntity<CategoryDTO> response =
                categoryController.updateCategory(1L, mockCategoryDTO());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Electronics", response.getBody().getName());
    }

    @Test
    void deleteCategory() {
        doNothing().when(categoryService).deleteCategory(1L);

        ResponseEntity<Void> response =
                categoryController.deleteCategory(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void handleIllegalArgumentException() {
        IllegalArgumentException ex =
                new IllegalArgumentException("Invalid category");

        ResponseEntity<String> response =
                categoryController.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid category", response.getBody());
    }

    @Test
    void handleResourceNotFoundException() {
        ResourceNotFoundException ex =
                new ResourceNotFoundException("Category", "id", 1L);

        ResponseEntity<String> response =
                categoryController.handleResourceNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("Category"));
    }
}
