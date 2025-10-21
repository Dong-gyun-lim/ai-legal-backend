package com.divorceai.crawl.dto;

import java.util.List;

public record JudicialResult(int totalCount, List<Item> list) {
    public record Item(
        String caseNo, String title, String summary,
        String court, String date, String url
    ) {}
}
