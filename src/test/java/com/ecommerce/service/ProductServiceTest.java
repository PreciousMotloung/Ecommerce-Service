package com.ecommerce.service;

import com.ecommerce.dto.request.CreateProductRequest;
import com.ecommerce.dto.response.ProductDto;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.exception.CategoryNotFoundException;
import com.ecommerce.exception.ProductNotFoundException;
import com.ecommerce.repository.CategoryRepository;
import com.ecommerce.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepo;

    @Mock
    private CategoryRepository categoryRepo;

    @InjectMocks
    private ProductService productService;

    private Category category;
    private Product product;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic goods")
                .createdAt(LocalDateTime.now())
                .build();

        product = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("A powerful laptop")
                .price(new BigDecimal("999.99"))
                .stockQuantity(10)
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getAllProducts_returnsListOfProductDtos() {
        when(productRepo.findAll()).thenReturn(List.of(product));

        List<ProductDto> result = productService.getAllProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Laptop");
        assertThat(result.get(0).categoryId()).isEqualTo(1L);
        assertThat(result.get(0).categoryName()).isEqualTo("Electronics");
    }

    @Test
    void getAllProducts_returnsEmptyList_whenNoProductsExist() {
        when(productRepo.findAll()).thenReturn(List.of());

        List<ProductDto> result = productService.getAllProducts();

        assertThat(result).isEmpty();
    }

    @Test
    void getProductById_returnsProductDto_whenProductExists() {
        when(productRepo.findById(1L)).thenReturn(Optional.of(product));

        ProductDto result = productService.getProductById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Laptop");
        assertThat(result.price()).isEqualByComparingTo("999.99");
        assertThat(result.stockQuantity()).isEqualTo(10);
    }

    @Test
    void getProductById_throwsProductNotFoundException_whenNotFound() {
        when(productRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getProductsByCategory_returnsProductsForGivenCategory() {
        when(productRepo.findByCategoryId(1L)).thenReturn(List.of(product));

        List<ProductDto> result = productService.getProductsByCategory(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryId()).isEqualTo(1L);
    }

    @Test
    void getProductsByCategory_returnsEmptyList_whenNoneInCategory() {
        when(productRepo.findByCategoryId(99L)).thenReturn(List.of());

        List<ProductDto> result = productService.getProductsByCategory(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void createProduct_withoutCategory_returnsProductDto() {
        CreateProductRequest request = new CreateProductRequest(
                "Tablet", "A tablet", new BigDecimal("499.99"), 20, null);

        Product saved = Product.builder()
                .id(2L).name("Tablet").description("A tablet")
                .price(new BigDecimal("499.99")).stockQuantity(20)
                .category(null).createdAt(LocalDateTime.now()).build();

        when(productRepo.save(any(Product.class))).thenReturn(saved);

        ProductDto result = productService.createProduct(request);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.name()).isEqualTo("Tablet");
        assertThat(result.categoryId()).isNull();
        assertThat(result.categoryName()).isNull();
        verify(categoryRepo, never()).findById(any());
    }

    @Test
    void createProduct_withCategory_returnsProductDtoWithCategory() {
        CreateProductRequest request = new CreateProductRequest(
                "Laptop", "A powerful laptop", new BigDecimal("999.99"), 10, 1L);

        when(categoryRepo.findById(1L)).thenReturn(Optional.of(category));
        when(productRepo.save(any(Product.class))).thenReturn(product);

        ProductDto result = productService.createProduct(request);

        assertThat(result.categoryId()).isEqualTo(1L);
        assertThat(result.categoryName()).isEqualTo("Electronics");
        verify(categoryRepo).findById(1L);
    }

    @Test
    void createProduct_throwsCategoryNotFoundException_whenCategoryNotFound() {
        CreateProductRequest request = new CreateProductRequest(
                "Laptop", "A laptop", new BigDecimal("999.99"), 10, 99L);

        when(categoryRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateStock_updatesStockQuantityAndReturnsUpdatedDto() {
        Product updated = Product.builder()
                .id(1L).name("Laptop").description("A powerful laptop")
                .price(new BigDecimal("999.99")).stockQuantity(50)
                .category(category).createdAt(LocalDateTime.now()).build();

        when(productRepo.findById(1L)).thenReturn(Optional.of(product));
        when(productRepo.save(any(Product.class))).thenReturn(updated);

        ProductDto result = productService.updateStock(1L, 50);

        assertThat(result.stockQuantity()).isEqualTo(50);
        verify(productRepo).save(any(Product.class));
    }

    @Test
    void updateStock_throwsProductNotFoundException_whenProductNotFound() {
        when(productRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateStock(99L, 10))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }
}