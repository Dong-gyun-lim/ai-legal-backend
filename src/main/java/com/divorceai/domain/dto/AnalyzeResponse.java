package com.divorceai.domain.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🤖 AI 분석 결과 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeResponse {

    private boolean ok; // 성공 여부
    private String error; // ⬅️ 오류 메시지(실패 시 세팅)
    private String answer; // 요약 답변(LLM)
    private String explanation; // 근거/해설 텍스트
    private double similarity; // 평균 유사도(%)
    private int damages; // 위자료(만원) - 초기 mock
    private String custody; // 양육권 귀속 - 초기 mock
    private List<ReferenceCase> references; // 근거 판례 목록

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceCase {
        private String caseNo; // 판례 번호
        private String court; // 법원
        private String judgmentDate; // 선고일
        private int score; // 유사도(%)
        private String sectionName; // 섹션명(이유/주문 등)
        private String text; // 근거 텍스트
    }
}
