package com.divorceai.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.divorceai.mapper.CaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 🔹 Flask 연동 기반 크롤링 서비스
 * - Spring은 목록 수집만 담당하고
 * - 상세 본문은 Flask 서버에 요청하여 받아옴
 * - 받아온 본문을 MariaDB에 저장
 */
@Service
public class CrawlService {

    private final CaseMapper caseMapper;
    private final ObjectMapper om = new ObjectMapper();
    private final RestTemplate rest = new RestTemplate();

    @Value("${flask.base-url}")
    private String flaskBaseUrl; // e.g. http://127.0.0.1:5001

    public CrawlService(CaseMapper caseMapper) {
        this.caseMapper = caseMapper;
    }

    /**
     * 🔸 Flask 서버로 판례 상세 HTML 요청 → DB 저장
     * 
     * @param keyword  검색 키워드 (예: "이혼")
     * @param page     페이지 번호 (기본 1)
     * @param pageSize 한 페이지당 결과 수
     */
    public int crawlOnce(String keyword, int page, int pageSize) throws Exception {
        System.out.println("🔎 [Crawl] keyword=" + keyword + ", page=" + page + ", size=" + pageSize);

        // Flask 서버에 요청할 URL
        String url = String.format("%s/crawl_list?keyword=%s&page=%d&size=%d",
                flaskBaseUrl, keyword, page, pageSize);

        ResponseEntity<String> res = rest.exchange(url, HttpMethod.GET, null, String.class);
        if (res.getStatusCode() != HttpStatus.OK || res.getBody() == null) {
            System.out.println("❌ [Flask] 목록 요청 실패: " + res.getStatusCode());
            return 0;
        }

        JsonNode root = om.readTree(res.getBody());
        JsonNode items = root.at("/data");
        if (items.isMissingNode() || !items.isArray()) {
            System.out.println("❌ [Crawl] 목록 데이터 없음");
            return 0;
        }

        int saved = 0;
        System.out.println("📦 [Crawl] items size=" + items.size());

        for (JsonNode n : items) {
            String caseNo = text(n, "case_no", "caseNo");
            String court = text(n, "court");
            String dateRaw = text(n, "judgment_date", "date");
            String summary = clean(text(n, "summary"));
            String srno = text(n, "srno");

            String judgedAt = normalizeDate(dateRaw);
            String type = keyword;
            String urlDetail = flaskBaseUrl + "/crawl_detail?srno=" + srno + "&keyword=" + keyword;

            try {
                // Flask에 상세 요청 보내기
                String html = fetchDetailFromFlask(srno, keyword);
                if (html == null || html.isBlank()) {
                    System.out.println("⛔ [Detail] empty for caseNo=" + caseNo);
                    continue;
                }

                // DB 저장
                saved += caseMapper.upsertCase(caseNo, court, judgedAt, type, summary, urlDetail, html);
                System.out.println("💾 [Save] " + caseNo + " inserted.");

                // 요청 간 지연 (차단 방지)
                Thread.sleep(300 + (long) (Math.random() * 600));

            } catch (Exception ex) {
                System.out.println("⚠️ [Detail] fetch failed srno=" + srno + " -> " + ex.getMessage());
            }
        }

        System.out.println("✅ [Crawl Done] saved=" + saved);
        return saved;
    }

    /** 🔹 Flask 서버에서 상세 본문 HTML 받아오기 */
    private String fetchDetailFromFlask(String srno, String keyword) {
        String url = String.format("%s/crawl_detail?srno=%s&keyword=%s", flaskBaseUrl, srno, keyword);
        ResponseEntity<String> res = rest.exchange(url, HttpMethod.GET, null, String.class);
        if (res.getStatusCode() != HttpStatus.OK || res.getBody() == null)
            return "";
        return res.getBody();
    }

    /** 🔹 JSON 텍스트 추출 */
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

    /** 🔹 날짜 YYYY-MM-DD 변환 */
    private static String normalizeDate(String s) {
        if (s == null)
            return "";
        s = s.trim();
        if (s.isBlank())
            return "";
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.length() == 8) {
            return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8);
        }
        try {
            LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
            return s;
        } catch (Exception ignore) {
            return s;
        }
    }

    /** 🔹 HTML 태그 제거 */
    private static String clean(String s) {
        if (s == null)
            return "";
        return s.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
