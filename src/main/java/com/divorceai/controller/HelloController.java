package com.divorceai.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 단순 핑/디버깅용 */
@RestController
public class HelloController {

    /** GET /api/hello */
    @GetMapping("/api/hello")
    public Map<String, Object> hello() {
        return Map.of(
            "ok", true,
            "message", "Hello from backend!"
        );
    }
}
