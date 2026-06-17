package com.example.redismysqldemo.repository;

import com.example.redismysqldemo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
