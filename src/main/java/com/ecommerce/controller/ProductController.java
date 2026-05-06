package com.ecommerce.controller;

import com.ecommerce.dto.request.CreateProductRequest;
import com.ecommerce.dto.response.ErrorResponse;
import com.ecommerce.dto.response.ProductDto;
import com.ecommerce.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product management — GET requires auth, POST requires ADMIN role")
@SecurityRequirement(name = "Bearer Authentication")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products",
            responses = @ApiResponse(responseCode = "200", description = "List of products"))
    public List<ProductDto> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Product found"),
                    @ApiResponse(responseCode = "404", description = "Not found",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ProductDto getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category",
            responses = @ApiResponse(responseCode = "200", description = "Products in category"))
    public List<ProductDto> getProductsByCategory(@PathVariable Long categoryId) {
        return productService.getProductsByCategory(categoryId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a product (ADMIN only)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Product created"),
                    @ApiResponse(responseCode = "400", description = "Validation error",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Admin role required",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            })
    public ProductDto createProduct(@Valid @RequestBody CreateProductRequest request) {
        return productService.createProduct(request);
    }
}