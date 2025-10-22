package com.divorceai.crawl;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class JudicialCrawler {

    // 세션 생성용 랜딩 페이지 (쿠키 확보)
    private static final String LANDING =
            "https://portal.scourt.go.kr/pgp/pgp1011/jdcpctSrchPage.do";

    // 목록 JSON
    private static final String LIST =
            "https://portal.scourt.go.kr/pgp/pgp1011/selectJdcpctSrchRsltLst.on";

    // 상세 JSON (전문)
    private static final String DETAIL =
            "https://portal.scourt.go.kr/pgp/pgp1011/selectJdcpctSrchRsltDtl.on";

    private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .cookieHandler(cookieManager)
            .build();

    /** 목록 조회: keyword/page/pageSize → JSON 문자열 */
    public String search(String keyword, int pageNo, int pageSize) throws Exception {
        warmup(); // 쿠키/세션 확보

        String jsonBody = """
        {
          "dma_searchParam": {
            "srchwd": "%s",
            "sort": "jis_jdcpc_instn_dvs_cd_s asc, $relevance desc, prnjdg_ymd_o desc, jdcpct_gr_cd_s asc",
            "sortType": "정확도",
            "pageNo": "%d",
            "pageSize": "%d",
            "jdcpctGrCd": "111|112|130|141|180|182|232|235|201",
            "category": "jdcpct",
            "isKwdSearch": "N"
          }
        }
        """.formatted(escape(keyword), pageNo, pageSize);

        HttpRequest req = HttpRequest.newBuilder(URI.create(LIST))
                .timeout(Duration.ofSeconds(30))
                .headers(headers())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        // 혹시 HTML(차단/리다이렉트)일 경우 빈 문자열 반환
        String head = res.body().substring(0, Math.min(res.body().length(), 200)).toLowerCase();
        if (head.contains("<!doctype html") || head.contains("<html")) {
            return "";
        }
        return res.body();
    }

    /** 상세 조회: jisCntntsSrno → 전문 JSON 문자열 */
    public String fetchDetail(String jisCntntsSrno) throws Exception {
        warmup();

        String jsonBody = """
        {"dma_searchParam":{"jisCntntsSrno":"%s"}}
        """.formatted(escape(jisCntntsSrno));

        HttpRequest req = HttpRequest.newBuilder(URI.create(DETAIL))
                .timeout(Duration.ofSeconds(30))
                .headers(headers())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return res.body();
    }

    /** 사람이 열어볼 수 있는 상세 페이지 URL */
    public static String buildViewUrl(String jisCntntsSrno) {
        return "https://portal.scourt.go.kr/pgp/pgp1011/jdcpctInqire.do?jisCntntsSrno=" + jisCntntsSrno;
    }

    // --------------------------------------

    private void warmup() throws Exception {
        HttpRequest warm = HttpRequest.newBuilder(URI.create(LANDING))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", ua())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .GET()
                .build();
        http.send(warm, HttpResponse.BodyHandlers.discarding());
    }

    private static String[] headers() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Content-Type", "application/json; charset=UTF-8");
        m.put("Accept", "application/json, text/plain, */*");
        m.put("User-Agent", ua());
        m.put("Origin", "https://portal.scourt.go.kr");
        m.put("Referer", "https://portal.scourt.go.kr/pgp/pgp1011/jdcpctSrchPage.do");
        m.put("X-Requested-With", "XMLHttpRequest");
        m.put("Accept-Language", "ko-KR,ko;q=0.9");
        return m.entrySet().stream().flatMap(e -> Stream.of(e.getKey(), e.getValue())).toArray(String[]::new);
    }

    private static String ua() {
        return "Mozilla/5.0 (Macintosh; Intel Mac OS X) AppleWebKit/537.36 (KHTML, like Gecko) Chrome Safari DivorceAI";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
