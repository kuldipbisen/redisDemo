package com.example.redismysqldemo.controller;

import com.example.redismysqldemo.service.RedisDataStructureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/redis")
public class RedisDataStructureController {

    private final RedisDataStructureService service;

    public RedisDataStructureController(RedisDataStructureService service) {
        this.service = service;
    }

    // String — Session token + Page view counter
    @GetMapping("/string")
    public Map<String, Object> stringDemo() {
        return service.stringDemo();
    }

    // Hash — User profile with partial field updates
    @GetMapping("/hash")
    public Map<String, Object> hashDemo() {
        return service.hashDemo();
    }

    // List — Recent activity feed (latest 5 actions)
    @GetMapping("/list")
    public Map<String, Object> listDemo() {
        return service.listDemo();
    }

    // Set — Unique visitor tracking + set operations
    @GetMapping("/set")
    public Map<String, Object> setDemo() {
        return service.setDemo();
    }

    // Sorted Set — Real-time game leaderboard
    @GetMapping("/sorted-set")
    public Map<String, Object> sortedSetDemo() {
        return service.sortedSetDemo();
    }

    // Stream — Order lifecycle event log
    @GetMapping("/stream")
    public Map<String, Object> streamDemo() {
        return service.streamDemo();
    }

    // Bitmap — Daily login tracking (memory efficient)
    @GetMapping("/bitmap")
    public Map<String, Object> bitmapDemo() {
        return service.bitmapDemo();
    }

    // HyperLogLog — Approximate unique page visitor count
    @GetMapping("/hyperloglog")
    public Map<String, Object> hyperLogLogDemo() {
        return service.hyperLogLogDemo();
    }
}
