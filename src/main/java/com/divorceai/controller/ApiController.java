package com.divorceai.controller;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.divorceai.domain.dto.AnalyzeRequest;
import com.divorceai.domain.dto.AnalyzeResponse;
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
        res.put("flask", analysisService.health());
        return ResponseEntity.ok(res);
    }

    /** POST /api/analyze : 분석 실행 */
    @PostMapping("/analyze")
    public ResponseEntity<AnalyzeResponse> analyze(@RequestBody AnalyzeRequest req) {
        AnalyzeResponse result = analysisService.analyze(req);
        if (Boolean.TRUE.equals(result.getOk()))
            return ResponseEntity.ok(result);
        return ResponseEntity.internalServerError().body(result);
    }
}
