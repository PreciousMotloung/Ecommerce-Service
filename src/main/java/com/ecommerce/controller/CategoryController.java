package com.ecommerce.controller;

import com.ecommerce.dto.request.CreateCategoryRequest;
import com.ecommerce.dto.response.CategoryDto;
import com.ecommerce.dto.response.ErrorResponse;
import com.ecommerce.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category management — GET requires auth, POST requires ADMIN role")
@SecurityRequirement(name = "Bearer Authentication")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories",
            responses = @ApiResponse(responseCode = "200", description = "List of categories"))
    public List<CategoryDto> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Category found"),
                    @ApiResponse(responseCode = "404", description = "Not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public CategoryDto getCategoryById(@PathVariable Long id) {
        return categoryService.getCategoryById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a category (ADMIN only)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Category created"),
                    @ApiResponse(responseCode = "400", description = "Validation error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Admin role required",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public CategoryDto createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.createCategory(request);
    }
}