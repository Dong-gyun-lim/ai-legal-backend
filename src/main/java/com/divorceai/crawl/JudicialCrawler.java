package com.divorceai.crawl;

import java.net.http.*;
import java.net.*;
import java.time.Duration;

public class JudicialCrawler {
    private static final String ENDPOINT =
        "https://portal.scourt.go.kr/pgp/pgp1011/selectJdcpctSrchRsltLst.on";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public String search(String keyword, int pageNo, int pageSize) throws Exception {
        String json = """
        {"dma_searchParam":{
          "srchwd":"%s",
          "sort":"jis_jdcpc_instn_dvs_cd_s asc, $relevance desc, prnjdg_ymd_o desc, jdcpct_gr_cd_s asc",
          "sortType":"정확도","pageNo":"%d","pageSize":"%d",
          "jdcpctGrCd":"111|112|130|141|180|182|232|235|201",
          "category":"jdcpct","isKwdSearch":"N"
        }}""".formatted(keyword, pageNo, pageSize);

        HttpRequest req = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json, text/plain, */*")
                .header("User-Agent", "Mozilla/5.0 (DivorceAI Crawler)")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode()/100 != 2) {
            throw new RuntimeException("HTTP " + res.statusCode() + " : " + res.body());
        }
        return res.body(); // JSON string
    }
}
