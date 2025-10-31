package com.divorceai.domain.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AnalyzeResponse {
    private Boolean ok; // true/false
    private String error; // 에러 메시지(있으면)
    private String answer; // LLM 요약 답변(한국어)
    private Explanation explanation; // 왜 그런 결론인지

    private Double similarity; // 평균 유사도(%)
    private Integer damages; // 추정 위자료(만원)
    private String custody; // 양육권 귀속
    private List<ReferenceCase> references; // 근거 판례들

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Explanation {
        private String reasoning; // 한 단락 설명
        private List<Factor> factors; // 핵심 요인
        private List<Highlight> highlights; // 하이라이트 스니펫
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Factor {
        private String name;
        private Double weight; // 0~1
        private String evidence;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class Highlight {
        private String caseNo;
        private Integer chunkIndex;
        private String span;
        private String tag;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @ToString
    public static class ReferenceCase {
        private String caseNo;
        private String court;
        private String judgmentDate;
        private Integer score; // 0~100
        private String sectionName; // (선택)
        private String text; // (선택)
    }
}
