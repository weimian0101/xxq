package com.example.gdms.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PlatformController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/demo/topics")
    public ResponseEntity<List<Map<String, Object>>> listTopics() {
        return ResponseEntity.ok(List.of(
                Map.of("id", 1, "title", "AI Thesis Management", "status", "approved"),
                Map.of("id", 2, "title", "IoT Security", "status", "pending")
        ));
    }

    @PostMapping("/demo/topics")
    public ResponseEntity<Map<String, Object>> createTopic(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of("created", true, "topic", payload));
    }

    @PostMapping("/selections")
    public ResponseEntity<Map<String, Object>> selectTopic(@RequestBody Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of("selected", true, "selection", payload));
    }

}

