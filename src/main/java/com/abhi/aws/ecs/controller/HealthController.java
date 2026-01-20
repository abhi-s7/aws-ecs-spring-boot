package com.abhi.aws.ecs.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Value("${spring.application.name}")
    private String appName;

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> home() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Gift Card Purchase Service");
        response.put("description", "ECS Demo - Track and manage gift card purchases");
        response.put("application", appName);
        response.put("status", "running");
        response.put("timestamp", LocalDateTime.now());
        
        try {
            InetAddress ip = InetAddress.getLocalHost();
            response.put("hostname", ip.getHostName());
            response.put("ip", ip.getHostAddress());
        } catch (Exception e) {
            response.put("hostname", "unknown");
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
