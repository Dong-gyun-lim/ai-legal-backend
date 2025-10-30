package com.divorceai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.divorceai.domain.dto.AnalyzeRequest;
import com.divorceai.domain.dto.AnalyzeResponse;
import com.divorceai.service.AiAnalysisService;

import lombok.RequiredArgsConstructor;

/**
 * 🎯 /api/analyze 엔드포인트
 */
@RestController
@RequestMapping("/api/analyze")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 개발 단계: 전체 허용 (배포 시 도메인 제한)
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping
    public ResponseEntity<?> analyze(@RequestBody AnalyzeRequest req) {
        try {
            AnalyzeResponse result = aiAnalysisService.analyze(req);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            AnalyzeResponse err = new AnalyzeResponse();
            err.setOk(false);
            err.setError("AI 분석 중 오류 발생: " + e.getMessage());
            return ResponseEntity.internalServerError().body(err);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok("{\"ok\": true, \"message\": \"Spring <-> Flask 연결 확인\"}");
    }
}
