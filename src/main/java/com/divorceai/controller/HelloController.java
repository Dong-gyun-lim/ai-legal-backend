package com.divorceai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

/**
 * 간단한 헬스/헬로 엔드포인트.
 * - /api/health : 서버가 살아있는지 확인 (가벼운 JSON)
 * - /api/hello  : 테스트/디버깅용 예시 응답
 */
@RestController
public class HelloController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        // DB 등 외부 의존성 확인 안 함 (가벼운 liveness 용도)
        return Map.of(
                "ok", true,
                "service", "divorce-ai",
                "profile", System.getProperty("spring.profiles.active", "dev")
        );
    }

    @GetMapping("/api/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hello from backend!");
    }
}
