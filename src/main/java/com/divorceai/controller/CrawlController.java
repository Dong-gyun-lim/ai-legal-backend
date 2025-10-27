package com.divorceai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.divorceai.service.CrawlService;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {

    private final CrawlService crawlService;

    public CrawlController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    /**
     * 🔹 Flask 연동 크롤링 트리거
     * 예시:
     * curl -X POST "http://localhost:9090/api/crawl/run?keyword=이혼&page=1&size=10"
     */
    @PostMapping("/run")
    public ResponseEntity<?> runCrawl(
            @RequestParam(defaultValue = "이혼") String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            System.out.println("🚀 [API] /crawl/run triggered with keyword=" + keyword);
            int savedCount = crawlService.crawlOnce(keyword, page, size);

            return ResponseEntity.ok().body(String.format(
                    "✅ Flask 연동 크롤링 완료: 저장된 판례 수 = %d", savedCount));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    "❌ 크롤링 실패: " + e.getMessage());
        }
    }

    /**
     * 🔸 상태 확인용 (헬스체크)
     * 예시: GET http://localhost:9090/api/crawl/health
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("✅ CrawlController is active");
    }
}
