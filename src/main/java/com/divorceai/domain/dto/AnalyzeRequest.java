package com.divorceai.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 🧠 AI 분석 요청 DTO (/api/analyze)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRequest {

    private String requestId; // 클라이언트 생성 UUID (선택)
    private String userEmail; // 사용자 이메일 (선택)

    // 자유서술
    private String summary; // 자유 입력 요약

    // 정형 입력
    private String gender;
    private Integer age;
    private Integer marriageYears;
    private Integer childCount;
    private String[] mainCauses;
    private Boolean hasAlimonyClaim;
    private Boolean hasCustodyClaim;

    // 직접 질의문(있으면 우선)
    private String question;
}
