package com.example.redismysqldemo.service;

import com.example.redismysqldemo.exception.ResourceNotFoundException;
import com.example.redismysqldemo.model.Product;
import com.example.redismysqldemo.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RedisTemplate<String, Product> redisTemplate;

    @Mock
    private ValueOperations<String, Product> valueOperations;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        productService = new ProductService(productRepository, redisTemplate, 10);
    }

    @Test
    void getById_shouldReturnFromCache_whenPresent() {
        Product cached = sampleProduct(1L, "Cached Product");
        when(valueOperations.get("product::1")).thenReturn(cached);

        Product result = productService.getById(1L);

        assertEquals("Cached Product", result.getName());
        verify(productRepository, never()).findById(any());
    }

    @Test
    void getById_shouldReadDatabaseAndPopulateCache_whenCacheMiss() {
        Product dbProduct = sampleProduct(2L, "DB Product");
        when(valueOperations.get("product::2")).thenReturn(null);
        when(productRepository.findById(2L)).thenReturn(Optional.of(dbProduct));

        Product result = productService.getById(2L);

        assertEquals("DB Product", result.getName());
        verify(valueOperations).set(eq("product::2"), eq(dbProduct), eq(Duration.ofMinutes(10)));
    }

    @Test
    void delete_shouldRemoveFromDatabaseAndCache() {
        when(productRepository.existsById(5L)).thenReturn(true);

        productService.delete(5L);

        verify(productRepository).deleteById(5L);
        verify(redisTemplate).delete("product::5");
    }

    @Test
    void update_shouldThrow_whenProductNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.update(99L, sampleProduct(null, "Any")));
    }

    private Product sampleProduct(Long id, String name) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription("Sample description");
        product.setPrice(new BigDecimal("1200.00"));
        product.setQuantity(5);
        return product;
    }
}
