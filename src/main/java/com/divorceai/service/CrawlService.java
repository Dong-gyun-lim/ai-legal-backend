package com.divorceai.service;

import com.divorceai.crawl.JudicialCrawler;
import com.divorceai.mapper.CaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class CrawlService {
    private final JudicialCrawler crawler = new JudicialCrawler(); // 간단 주입
    private final CaseMapper caseMapper;
    private final ObjectMapper om = new ObjectMapper();

    public CrawlService(CaseMapper caseMapper) { this.caseMapper = caseMapper; }

    public int crawlOnce(String keyword, int page, int pageSize) throws Exception {
        String json = crawler.search(keyword, page, pageSize);
        JsonNode root = om.readTree(json);

        // 실제 경로는 응답 JSON 구조에 맞게 수정
        JsonNode items = root.at("/result/list"); // 예: /result/list
        if (items.isMissingNode() || !items.isArray()) return 0;

        int saved = 0;
        for (JsonNode n : items) {
            String caseNo  = n.path("caseNo").asText();
            String title   = clean(n.path("title").asText());
            String summary = clean(n.path("summary").asText(""));
            String court   = n.path("court").asText("");
            String date    = n.path("date").asText(""); // "YYYY-MM-DD"로 변환 필요 시 여기서
            String url     = n.path("url").asText("");

            if (!caseNo.isBlank()) {
                saved += caseMapper.upsertCase(caseNo, title, summary, court, date, url);
            }
        }
        return saved;
    }

    private String clean(String s){
        if (s == null) return "";
        return s.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }
}
