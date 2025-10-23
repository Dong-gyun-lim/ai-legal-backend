package com.divorceai.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// ✅ Jsoup (중요: java.lang.model.util.Elements 가 아니라 아래 3개!)
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import com.divorceai.crawl.JudicialCrawler;
import com.divorceai.mapper.CaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CrawlService {

    private final JudicialCrawler crawler = new JudicialCrawler();
    private final CaseMapper caseMapper;
    private final ObjectMapper om = new ObjectMapper();

    public CrawlService(CaseMapper caseMapper) {
        this.caseMapper = caseMapper;
    }

    public int crawlOnce(String keyword, int page, int pageSize) throws Exception {
        System.out.println("🔎 [Crawl] keyword=" + keyword + ", page=" + page + ", size=" + pageSize);

        String body = crawler.fetchListJson(keyword, page, pageSize);
        if (body != null && !body.isBlank()) {
            int b = Math.min(900, body.length());
            System.out.println("🧾 [Crawl Raw Response]\n" + body.substring(0, b));
        }
        if (body == null || body.isBlank()) {
            System.out.println("❌ [Crawl] list response empty");
            return 0;
        }

        JsonNode root = om.readTree(body);
        JsonNode items = root.at("/data/dlt_jdcpctRslt");
        if (items.isMissingNode() || !items.isArray()) {
            System.out.println("❌ [Crawl] items not found");
            return 0;
        }

        System.out.println("📦 [Crawl] items size=" + items.size());

        int saved = 0;
        for (JsonNode n : items) {
            String caseNo = text(n, "csNoLstCtt");
            String court = text(n, "cortNm");
            String dateRaw = text(n, "prnjdgYmd");
            String summary = clean(text(n, "jdcpctSumrCtt"));
            String srno = text(n, "jisCntntsSrno"); // 상세페이지 키
            String inst = text(n, "jisJdcpcInstnDvsCd"); // 기관 코드
            String type = keyword;
            String judgedAt = normalizeDate(dateRaw);

            String fullText = "";
            if (!srno.isBlank()) {
                System.out.printf("🔎 caseNo=%s srno=%s inst=%s | ", caseNo, srno, inst);
                String detailHtml = crawler.fetchDetailHtml(srno, keyword);
                fullText = extractMainHtml(detailHtml); // ✅ 상세페이지에서 본문 HTML 캡쳐
                System.out.println("list.len=" + fullText.length());
            }

            try {
                saved += caseMapper.upsertCase(
                        caseNo, court, judgedAt, type, summary, null /* source_url */,
                        fullText // ✅ 전문 HTML
                );
            } catch (Exception ex) {
                System.out.println("⚠️ [Crawl] upsert failed caseNo=" + caseNo + " -> " + ex.getMessage());
            }
        }

        System.out.println("💾 [Crawl] saved=" + saved);
        return saved;
    }

    /** 상세 HTML 안에서 본문 영역만 뽑아오기 */
    private static String extractMainHtml(String html) {
        if (html == null || html.isBlank())
            return "";
        try {
            Document doc = Jsoup.parse(html);

            // 후보 선택자들 (페이지 구조 변동 대비)
            List<Element> candidates = new ArrayList<>();
            candidates.add(doc.selectFirst("div#judgmentNote")); // 판시사항
            candidates.add(doc.selectFirst("div#judgmentReason")); // 판결요지
            candidates.add(doc.selectFirst("div#judgmentNote, div#txtview")); // 일반 텍스트 영역
            candidates.add(doc.selectFirst("div.ctxCntnts, div.cntnts")); // 내부 컨텐츠
            candidates.add(doc.selectFirst("div#wf_pgpDtlMain, .cntntsArea"));// 최상 부모 컨테이너

            Element best = null;
            int bestLen = 0;
            for (Element e : candidates) {
                if (e == null)
                    continue;
                int len = e.text().length();
                if (len > bestLen) {
                    best = e;
                    bestLen = len;
                }
            }

            if (best != null) {
                return best.html(); // ✅ HTML 그대로 (태그 유지)
            }

            // 마지막 안전장치: 넓은 범위
            Element main = doc.selectFirst("div#content, div.mainFrame, div.cntntsArea, div#wf_pgpDtlMain");
            if (main != null)
                return main.html();

            return "";
        } catch (Exception e) {
            return "";
        }
    }

    private static String text(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && !v.isNull()) {
                String s = v.asText("");
                if (!s.isBlank())
                    return s;
            }
        }
        return "";
    }

    private static String normalizeDate(String s) {
        if (s == null)
            return "";
        s = s.trim();
        if (s.isBlank())
            return "";
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.length() == 8) {
            String y = digits.substring(0, 4);
            String m = digits.substring(4, 6);
            String d = digits.substring(6, 8);
            return y + "-" + m + "-" + d;
        }
        try {
            LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return s;
        } catch (Exception ignore) {
            return s;
        }
    }

    private static String clean(String s) {
        if (s == null)
            return "";
        return s.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
