package com.divorceai.crawl.job;

import com.divorceai.service.CrawlService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CrawlJob {
    private final CrawlService service;
    public CrawlJob(CrawlService service) { this.service = service; }

    // 매일 새벽 3:00 (서울시간)
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void nightly() {
        try { service.crawlOnce("이혼", 1, 20); } catch (Exception ignore) {}
    }
}
