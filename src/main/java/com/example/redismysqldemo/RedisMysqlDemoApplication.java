package com.example.redismysqldemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class RedisMysqlDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedisMysqlDemoApplication.class, args);
    }
}
