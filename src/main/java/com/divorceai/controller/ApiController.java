package com.divorceai.controller;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.divorceai.service.AnalysisService;

import lombok.RequiredArgsConstructor;

/** 공용 API 루트 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

    private final AnalysisService analysisService;
    private final Environment env;

    /** GET /api/health : Spring + (옵션) Flask 상태를 함께 반환 */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> apiHealth() {
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        res.put("service", "divorce-ai");
        res.put("profile", String.join(",", env.getActiveProfiles()));
        res.put("time", OffsetDateTime.now().toString());

        // Flask 상태 합치기 (AnalysisService.health()가 내부에서 /health 프록시 호출)
        Map<String, Object> flask = analysisService.health();
        res.put("flask", flask);

        return ResponseEntity.ok(res);
    }
}
