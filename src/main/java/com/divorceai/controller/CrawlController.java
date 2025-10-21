package com.divorceai.controller;

import com.divorceai.service.CrawlService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crawl")
public class CrawlController {
    private final CrawlService service;
    public CrawlController(CrawlService service) { this.service = service; }

    @PostMapping("/run")
    public String run(@RequestParam(defaultValue = "이혼") String keyword,
                      @RequestParam(defaultValue = "1") int page,
                      @RequestParam(defaultValue = "20") int size) throws Exception {
        int n = service.crawlOnce(keyword, page, size);
        return "saved=" + n;
    }
}
