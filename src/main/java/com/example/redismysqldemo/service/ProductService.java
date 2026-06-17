package com.example.redismysqldemo.service;

import com.example.redismysqldemo.exception.ResourceNotFoundException;
import com.example.redismysqldemo.model.Product;
import com.example.redismysqldemo.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final RedisTemplate<String, Product> redisTemplate;
    private final Duration cacheTtl;

    public ProductService(ProductRepository productRepository,
                          RedisTemplate<String, Product> redisTemplate,
                          @Value("${app.cache.ttl-minutes:10}") long cacheTtlMinutes) {
        this.productRepository = productRepository;
        this.redisTemplate = redisTemplate;
        this.cacheTtl = Duration.ofMinutes(cacheTtlMinutes);
    }

    // ── Manual RedisTemplate approach ──────────────────────────────────────

    // Scenario: CACHE WRITE on create
    public Product createManual(Product product) {
        Product saved = productRepository.save(product);
        redisTemplate.opsForValue().set(cacheKey(saved.getId()), saved, cacheTtl);
        log.info("[MANUAL] CACHE WRITE  - product saved to Redis with key: {}", cacheKey(saved.getId()));
        return saved;
    }

    // Scenario: CACHE HIT vs CACHE MISS
    public Product getByIdManual(Long id) {
        String key = cacheKey(id);
        ValueOperations<String, Product> ops = redisTemplate.opsForValue();

        Product cached = ops.get(key);
        if (cached != null) {
            log.info("[MANUAL] CACHE HIT   - returned from Redis, DB NOT called. key: {}", key);
            return cached;
        }

        log.info("[MANUAL] CACHE MISS  - not in Redis, hitting DB for id: {}", id);
        Product dbProduct = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        ops.set(key, dbProduct, cacheTtl);
        log.info("[MANUAL] CACHE WRITE - stored in Redis with TTL: {} mins. key: {}", cacheTtl.toMinutes(), key);
        return dbProduct;
    }

    // Scenario: CACHE UPDATE on update
    public Product updateManual(Long id, Product request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        existing.setQuantity(request.getQuantity());
        Product updated = productRepository.save(existing);
        redisTemplate.opsForValue().set(cacheKey(updated.getId()), updated, cacheTtl);
        log.info("[MANUAL] CACHE UPDATE - Redis entry refreshed for key: {}", cacheKey(updated.getId()));
        return updated;
    }

    // Scenario: CACHE EVICT on delete
    public void deleteManual(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        redisTemplate.delete(cacheKey(id));
        log.info("[MANUAL] CACHE EVICT - removed from Redis. key: {}", cacheKey(id));
    }

    // ── Annotation-based approach ──────────────────────────────────────────

    // Scenario: CACHE WRITE on create (@CachePut always writes to cache)
    @CachePut(value = "products", key = "#result.id")
    public Product create(Product product) {
        log.info("[ANNOTATION] CACHE WRITE - saving new product to DB and Redis");
        return productRepository.save(product);
    }

    // Scenario: CACHE HIT (method skipped) vs CACHE MISS (method runs)
    @Cacheable(value = "products", key = "#id")
    public Product getById(Long id) {
        // This log only prints on CACHE MISS — proves DB is NOT called on cache hit
        log.info("[ANNOTATION] CACHE MISS - DB called for id: {}. Will cache result.", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    // Scenario: NO CACHE - getAll always hits DB
    public List<Product> getAll() {
        log.info("[ANNOTATION] NO CACHE - getAll always fetches from DB");
        return productRepository.findAll();
    }

    // Scenario: CACHE UPDATE - @CachePut always updates cache after DB update
    @CachePut(value = "products", key = "#id")
    public Product update(Long id, Product request) {
        log.info("[ANNOTATION] CACHE UPDATE - updating DB and refreshing Redis for id: {}", id);
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        existing.setName(request.getName());
        existing.setDescription(request.getDescription());
        existing.setPrice(request.getPrice());
        existing.setQuantity(request.getQuantity());
        return productRepository.save(existing);
    }

    // Scenario: CACHE EVICT - removes from Redis on delete
    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        log.info("[ANNOTATION] CACHE EVICT - removing from Redis and DB for id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    // ── Shared helper ──────────────────────────────────────────────────────

    private String cacheKey(Long id) {
        return "product::" + id;
    }
}
