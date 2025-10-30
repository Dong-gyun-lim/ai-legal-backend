package com.divorceai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.divorceai.domain.dto.AnalyzeRequest;
import com.divorceai.domain.dto.AnalyzeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

/**
 * 🤖 Flask RAG 서버(/rag) 호출 전용 서비스
 */
@Service
@RequiredArgsConstructor
public class AiAnalysisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${flask.base-url:http://127.0.0.1:5001}")
    private String flaskBaseUrl;

    public AnalyzeResponse analyze(AnalyzeRequest req) throws Exception {
        // 1) 질문 생성
        String question = (req.getQuestion() != null && !req.getQuestion().isBlank())
                ? req.getQuestion()
                : buildQuestionFromStruct(req);

        // 2) Flask 요청
        String url = flaskBaseUrl + "/rag";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String payload = objectMapper.writeValueAsString(new FlaskPayload(question, 3));
        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Flask 서버 호출 실패: " + response.getStatusCode());
        }

        // 3) 응답 파싱
        JsonNode json = objectMapper.readTree(response.getBody());

        AnalyzeResponse result = new AnalyzeResponse();
        result.setOk(true);
        result.setAnswer(json.path("answer").asText(""));
        result.setExplanation(json.path("explanation").asText(""));

        // 유사도 평균(%)
        double avgSim = 0.0;
        if (json.has("scores") && json.get("scores").isArray() && json.get("scores").size() > 0) {
            double sum = 0;
            for (JsonNode n : json.get("scores")) sum += n.asDouble();
            avgSim = sum / json.get("scores").size();
        }
        result.setSimilarity(Math.round(avgSim * 100.0) / 100.0);

        // 초기 mock (나중에 Flask가 계산/반환하도록 이관)
        result.setDamages(1800);
        result.setCustody("모(母)");

        // 판례 목록 매핑
        List<AnalyzeResponse.ReferenceCase> refs = new ArrayList<>();
        if (json.has("references") && json.get("references").isArray()) {
            for (JsonNode ref : json.get("references")) {
                AnalyzeResponse.ReferenceCase r = new AnalyzeResponse.ReferenceCase();
                r.setCaseNo(ref.path("case_no").asText(null));
                r.setCourt(ref.path("court").asText(null));
                r.setJudgmentDate(ref.path("judgment_date").asText(null));
                r.setScore((int) Math.round(ref.path("score").asDouble(0.0)));
                r.setSectionName(ref.path("section_name").asText(null));
                r.setText(ref.path("text").asText(null));
                refs.add(r);
            }
        }
        result.setReferences(refs);

        return result;
    }

    /** Flask 요청용 내부 DTO */
    record FlaskPayload(String question, int top_k) {}

    /** summary 없을 때 정형 데이터로 질문 자동 생성 */
    private String buildQuestionFromStruct(AnalyzeRequest req) {
        StringBuilder sb = new StringBuilder();
        if (req.getGender() != null) sb.append("성별 ").append(req.getGender()).append(", ");
        if (req.getAge() != null) sb.append("나이 ").append(req.getAge()).append("세, ");
        if (req.getMarriageYears() != null) sb.append("혼인 ").append(req.getMarriageYears()).append("년, ");
        if (req.getChildCount() != null) sb.append("자녀 ").append(req.getChildCount()).append("명, ");
        if (req.getMainCauses() != null && req.getMainCauses().length > 0)
            sb.append("주요 사유: ").append(String.join(", ", req.getMainCauses())).append(", ");
        if (Boolean.TRUE.equals(req.getHasAlimonyClaim()) || Boolean.TRUE.equals(req.getHasCustodyClaim())) {
            sb.append("청구: ");
            if (Boolean.TRUE.equals(req.getHasAlimonyClaim())) sb.append("위자료 ");
            if (Boolean.TRUE.equals(req.getHasCustodyClaim())) sb.append("양육권 ");
        }
        return sb.toString().trim().replaceAll(", $", "") + " 상황에서 유사 판례와 판단 기준을 알려주세요.";
    }
}
