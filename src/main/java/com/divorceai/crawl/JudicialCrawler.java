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

    private static final String LANDING = "https://portal.scourt.go.kr/pgp/pgp1011/jdcpctSrchPage.do";

    // ✅ 목록 JSON
    private static final String LIST_ENDPOINT = "https://portal.scourt.go.kr/pgp/pgp1011/selectJdcpctSrchRsltLst.on";

    // ✅ 상세 HTML (판시사항/전문 탭이 들어있는 페이지)
    private static final String DETAIL_PAGE = "https://portal.scourt.go.kr/pgp/main.on?w2xPath=PGP1011M04&jisCntntsSrno=%s&N2Nc=900&srchwd=%s&m=1&pgDvrs=1";

    private final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .cookieHandler(cookieManager)
            .build();

    public String fetchListJson(String keyword, int pageNo, int pageSize) throws Exception {
        warmup();

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

        HttpRequest req = HttpRequest.newBuilder(URI.create(LIST_ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .headers(headers())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = res.body();
        String head = body.substring(0, Math.min(body.length(), 200)).toLowerCase();
        if (head.contains("<!doctype html") || head.contains("<html"))
            return "";
        return body;
    }

    public String fetchDetailHtml(String srno, String keyword) throws Exception {
        warmup();
        String url = String.format(DETAIL_PAGE, srno, urlEncode(keyword));
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", ua())
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .GET()
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return res.body();
    }

    private void warmup() throws Exception {
        HttpRequest warm = HttpRequest.newBuilder(URI.create(LANDING))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", ua())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9")
                .GET().build();
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
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return s;
        }
    }
}
