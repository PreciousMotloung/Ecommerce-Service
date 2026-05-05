package com.ecommerce.service;

import com.ecommerce.dto.request.CreateCategoryRequest;
import com.ecommerce.dto.response.CategoryDto;
import com.ecommerce.entity.Category;
import com.ecommerce.exception.CategoryNotFoundException;
import com.ecommerce.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepo;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic goods")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllCategories_returnsAllCategories() {
        when(categoryRepo.findAll()).thenReturn(List.of(category));

        List<CategoryDto> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Electronics");
        assertThat(result.get(0).description()).isEqualTo("Electronic goods");
    }

    @Test
    void getAllCategories_returnsEmptyList_whenNoCategoriesExist() {
        when(categoryRepo.findAll()).thenReturn(List.of());

        List<CategoryDto> result = categoryService.getAllCategories();

        assertThat(result).isEmpty();
    }

    @Test
    void getCategoryById_returnsCategoryDto_whenFound() {
        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));

        CategoryDto result = categoryService.getCategoryById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Electronics");
        assertThat(result.description()).isEqualTo("Electronic goods");
    }

    @Test
    void getCategoryById_throwsCategoryNotFoundException_whenNotFound() {
        when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createCategory_returnsCreatedCategoryDto() {
        CreateCategoryRequest request = new CreateCategoryRequest("Books", "All kinds of books");
        Category saved = Category.builder()
                .id(2L)
                .name("Books")
                .description("All kinds of books")
                .createdAt(LocalDateTime.now())
                .build();
        when(categoryRepo.save(any(Category.class))).thenReturn(saved);

        CategoryDto result = categoryService.createCategory(request);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.name()).isEqualTo("Books");
        assertThat(result.description()).isEqualTo("All kinds of books");
        verify(categoryRepo).save(any(Category.class));
    }
}