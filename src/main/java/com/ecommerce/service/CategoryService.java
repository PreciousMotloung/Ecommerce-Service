package com.ecommerce.service;

import com.ecommerce.dto.request.CreateCategoryRequest;
import com.ecommerce.dto.response.CategoryDto;
import com.ecommerce.entity.Category;
import com.ecommerce.exception.CategoryNotFoundException;
import com.ecommerce.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepo;

    public List<CategoryDto> getAllCategories() {
        return categoryRepo.findAll().stream()
                .map(this::toCategoryDto)
                .toList();
    }

    public CategoryDto getCategoryById(Long id) {
        return categoryRepo.findById(id)
                .map(this::toCategoryDto)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    public CategoryDto createCategory(CreateCategoryRequest request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toCategoryDto(categoryRepo.save(category));
    }

    private CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getCreatedAt()
        );
    }
}