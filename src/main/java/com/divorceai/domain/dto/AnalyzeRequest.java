package com.divorceai.domain.dto;

import java.util.List;
import java.util.Map;

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
public class AnalyzeRequest {
    // ===== 자유 입력 =====
    private String question; // 자유 질문 (없으면 아래 정형값으로 질문 자동 생성)
    private Integer topK; // 유사 판례 K (기본 3)
    private String userEmail;
    private String summary; // 자유 요약(있으면 질문 생성 시 가중 반영)

    // ===== 기본 사건 정보 =====
    private String gender; // "남"/"여"
    private Integer age; // 나이
    private Integer marriageYears; // 혼인기간(년)
    private Integer childCount; // 자녀 수

    // ===== 사건 성격 / 청구 항목 =====
    private List<String> caseTypes; // ["이혼","위자료","양육","재산분할"] 등 다중 선택
    private String role; // "가해자"/"피해자" 또는 "원고"/"피고"
    private List<String> mainCauses; // ["외도","폭행","경제적 문제","성격 차이","별거","기타"]

    private Boolean hasAlimonyClaim; // 위자료 청구
    private Boolean hasCustodyClaim; // 양육권 청구
    private Boolean hasPropertyClaim; // 재산분할 청구
    private Boolean wantsWinPrediction;// 승소 가능성 예측 희망 여부

    // ===== 소송/절차 단계 =====
    private String stage; // "협의이혼","조정","소장 접수","1심","항소" ...
    private Boolean triedMediation; // 조정/화해 시도 여부

    // ===== 증거 / 정황 =====
    private List<String> evidence; // ["메시지","사진","진단서","통화녹취","계좌내역","기타"]
    private String evidenceNote; // 증거 설명

    // ===== 경제/양육 정보(요약 수준) =====
    private Integer monthlyIncomeSelf; // 본인 월소득(만원)
    private Integer monthlyIncomeSpouse; // 배우자 월소득(만원)
    private Integer debtTotal; // 부채 총액(만원)
    private Integer assetApprox; // 자산 대략(만원)
    private String caregivingStatus; // "주 양육자(본인)"/"배우자"/"분담"
    private String childMainAgeBand; // "영유아/초등/중등/고등" 등

    // ===== 기타 확장 필드 (프론트 임시 값 수용용) =====
    private Map<String, Object> extras; // UI에서 새로 생기는 값 임시 수용
}
