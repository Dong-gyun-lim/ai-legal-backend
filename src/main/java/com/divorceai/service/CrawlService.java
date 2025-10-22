package com.divorceai.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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

    /**
     * 대법원 판례 포털에서 keyword로 검색 → JSON 파싱 → precedents 저장
     * - summary: clean() (보기용)
     * - full_text: HTML 원본(목록에 짧으면 상세 API로 보강)
     */
    public int crawlOnce(String keyword, int page, int pageSize) throws Exception {
        System.out.println("🔎 [Crawl] keyword=" + keyword + ", page=" + page + ", size=" + pageSize);

        String body = crawler.search(keyword, page, pageSize);
        if (body != null) {
            System.out.println("🧾 [Crawl Raw Response]\n" + body.substring(0, Math.min(body.length(), 1200)));
        }

        if (body == null || body.isBlank()) {
            System.out.println("❌ [Crawl] empty or HTML-like response");
            return 0;
        }

        JsonNode root;
        try {
            root = om.readTree(body);
        } catch (Exception e) {
            System.out.println("❌ [Crawl] JSON parse error: " + e.getMessage());
            return 0;
        }

        // 실제 리스트 경로
        String[] candidates = new String[] {
                "/data/dlt_jdcpctRslt",
                "/result/list", "/dma_result/list", "/data/list",
                "/list", "/items", "/result/items", "/content"
        };

        JsonNode items = null;
        String hit = null;
        for (String p : candidates) {
            JsonNode node = root.at(p);
            if (!node.isMissingNode() && node.isArray()) {
                items = node;
                hit = p;
                break;
            }
        }

        if (items == null) {
            System.out.println("❌ [Crawl] list not found in JSON");
            return 0;
        }

        System.out.println("📦 [Crawl] items path=" + hit + " size=" + items.size());

        int saved = 0;

        for (JsonNode n : items) {
            String caseNo = text(n, "csNoLstCtt", "caseNo", "case_no");
            String court = text(n, "cortNm", "court", "courtName");
            String dateRaw = text(n, "prnjdgYmd", "judgment_date");
            String summary = clean(text(n, "jdcpctSumrCtt", "summary", "sumry")); // 보기용
            String fullText = text(n, "jdcpctXmlCtt"); // 목록에 있는 전문(보통 짧음)
            String srno = text(n, "jisCntntsSrno"); // 상세 조회 키
            String url = text(n, "source_url", "url");
            if (url.isBlank() && !srno.isBlank()) {
                url = JudicialCrawler.buildViewUrl(srno);
            }

            // 전문이 짧으면 상세 API로 보강 (보통 수천~수만자)
            if ((fullText == null || fullText.length() < 2000) && !srno.isBlank()) {
                try {
                    String detailJson = crawler.fetchDetail(srno);
                    if (detailJson != null && !detailJson.isBlank()) {
                        JsonNode detailRoot = om.readTree(detailJson);
                        String[] detailPaths = new String[] {
                                "/data/jdcpctXmlCtt", "/result/jdcpctXmlCtt", "/jdcpctXmlCtt"
                        };
                        for (String p : detailPaths) {
                            JsonNode dn = detailRoot.at(p);
                            if (!dn.isMissingNode() && !dn.isNull()) {
                                String v = dn.asText("");
                                if (!v.isBlank()) {
                                    fullText = v;
                                    break;
                                }
                            }
                        }
                        if (fullText == null || fullText.isBlank()) {
                            JsonNode dlist = detailRoot.at("/data/dlt_jdcpctRslt");
                            if (dlist.isArray() && dlist.size() > 0) {
                                String v = dlist.get(0).path("jdcpctXmlCtt").asText("");
                                if (!v.isBlank())
                                    fullText = v;
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.out.println("⚠️ [Crawl] detail fetch failed srno=" + srno + " -> " + ex.getMessage());
                }
            }

            String judgedAt = normalizeDate(dateRaw);
            String type = keyword; // 예: "이혼"

            if (caseNo != null && !caseNo.isBlank()) {
                try {
                    saved += caseMapper.upsertCase(
                            caseNo, court, judgedAt, type, summary, url, fullText);
                } catch (Exception ex) {
                    System.out.println("⚠️ [Crawl] upsert failed caseNo=" + caseNo + " -> " + ex.getMessage());
                }
            }
        }

        System.out.println("💾 [Crawl] saved=" + saved);
        return saved;
    }

    // ------------------ helpers ------------------

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
