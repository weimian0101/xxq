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

    // 密码测试端点（仅开发环境使用）
    @PostMapping("/test-password")
    public ResponseEntity<Map<String, Object>> testPassword(@RequestBody Map<String, String> payload) {
        String password = payload.get("password");
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "密码不能为空"));
        }

        // 这里可以注入PasswordGenerator来测试密码哈希
        // 但为了安全起见，这个端点只在开发环境中启用

        return ResponseEntity.ok(Map.of(
            "message", "密码测试完成",
            "password_length", password.length(),
            "contains_uppercase", password.matches(".*[A-Z].*"),
            "contains_lowercase", password.matches(".*[a-z].*"),
            "contains_digit", password.matches(".*[0-9].*"),
            "contains_special", password.matches(".*[^A-Za-z0-9].*")
        ));
    }

}

