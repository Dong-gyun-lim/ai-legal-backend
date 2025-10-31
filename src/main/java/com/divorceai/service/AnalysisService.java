package com.divorceai.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import com.divorceai.domain.dto.AnalyzeResponse.Explanation;
import com.divorceai.domain.dto.AnalyzeResponse.Factor;
import com.divorceai.domain.dto.AnalyzeResponse.Highlight;
import com.divorceai.domain.dto.AnalyzeResponse.ReferenceCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${flask.base-url:http://127.0.0.1:5001}")
    private String flaskBaseUrl;

    /** Spring → Flask /health 프록시 */
    public Map<String, Object> health() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject(flaskBaseUrl + "/health", Map.class);
            if (res == null)
                res = new HashMap<>();
            res.putIfAbsent("ok", true);
            res.put("source", "flask");
            return res;
        } catch (Exception e) {
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", "Flask health request failed: " + e.getMessage());
            err.put("flaskBaseUrl", flaskBaseUrl);
            return err;
        }
    }

    /** Spring → Flask /rag 호출(권장 경로) */
    public AnalyzeResponse analyze(AnalyzeRequest req) {
        AnalyzeResponse result = new AnalyzeResponse();
        try {
            String question = (req.getQuestion() != null && !req.getQuestion().isBlank())
                    ? req.getQuestion()
                    : buildQuestionFromStruct(req);

            int topK = (req.getTopK() != null && req.getTopK() > 0) ? req.getTopK() : 3;

            Map<String, Object> payload = Map.of("question", question, "top_k", topK);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    flaskBaseUrl + "/rag", HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                result.setOk(false);
                result.setError("Flask 호출 실패: " + resp.getStatusCode());
                return result;
            }

            JsonNode json = objectMapper.readTree(resp.getBody());

            // 기본 필드
            result.setOk(true);
            result.setAnswer(json.path("answer").asText(""));

            // explanation (reasoning + factors + highlights)
            Explanation exp = new Explanation();
            JsonNode expNode = json.path("explanation");
            exp.setReasoning(expNode.path("reasoning").asText(""));
            // factors
            if (expNode.has("factors") && expNode.get("factors").isArray()) {
                List<Factor> factors = new ArrayList<>();
                for (JsonNode f : expNode.get("factors")) {
                    Factor ff = new Factor(
                            f.path("name").asText(""),
                            f.path("weight").asDouble(0.0),
                            f.path("evidence").asText(""));
                    factors.add(ff);
                }
                exp.setFactors(factors);
            } else {
                exp.setFactors(Collections.emptyList());
            }
            // highlights
            if (expNode.has("highlights") && expNode.get("highlights").isArray()) {
                List<Highlight> highs = new ArrayList<>();
                for (JsonNode h : expNode.get("highlights")) {
                    Highlight hh = new Highlight(
                            h.path("case_no").asText(null),
                            h.hasNonNull("chunk_index") ? h.get("chunk_index").asInt() : null,
                            h.path("span").asText(null),
                            h.path("tag").asText(null));
                    highs.add(hh);
                }
                exp.setHighlights(highs);
            } else {
                exp.setHighlights(Collections.emptyList());
            }
            result.setExplanation(exp);

            // 평균 유사도(%)
            double avg = 0.0;
            if (json.has("scores") && json.get("scores").isArray() && json.get("scores").size() > 0) {
                avg = json.get("scores")
                        .findValuesAsText("") // not used
                        .isEmpty()
                                ? averageFromArray(json.get("scores"))
                                : averageFromArray(json.get("scores"));
            }
            result.setSimilarity(Math.round(avg * 100.0) / 100.0);

            // (임시) 위자료/양육권 — 나중에 Flask가 계산해서 내려주면 그대로 매핑
            result.setDamages(1800);
            result.setCustody("모(母)");

            // 근거 판례
            List<ReferenceCase> refs = new ArrayList<>();
            if (json.has("references") && json.get("references").isArray()) {
                for (JsonNode r : json.get("references")) {
                    ReferenceCase rc = new ReferenceCase(
                            r.path("case_no").asText(null),
                            r.path("court").asText(null),
                            r.path("judgment_date").asText(null),
                            (int) Math.round(r.path("score").asDouble(0.0)),
                            r.path("section_name").asText(null),
                            r.path("text").asText(null));
                    refs.add(rc);
                }
            }
            result.setReferences(refs);

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            result.setOk(false);
            result.setError("AI 분석 중 오류: " + e.getMessage());
            return result;
        }
    }

    private static double averageFromArray(JsonNode arr) {
        double sum = 0.0;
        int n = 0;
        for (JsonNode v : arr) {
            sum += v.asDouble(0.0);
            n++;
        }
        return n == 0 ? 0.0 : sum / n;
    }

    /** summary 없을 때 정형 데이터로 질문 자동 생성 */
    private String buildQuestionFromStruct(AnalyzeRequest req) {
        // 사용자가 입력한 구조화 값으로 간결 질문 생성
        List<String> parts = new ArrayList<>();
        if (req.getGender() != null)
            parts.add("성별 " + req.getGender());
        if (req.getAge() != null)
            parts.add("나이 " + req.getAge() + "세");
        if (req.getMarriageYears() != null)
            parts.add("혼인 " + req.getMarriageYears() + "년");
        if (req.getChildCount() != null)
            parts.add("자녀 " + req.getChildCount() + "명");
        if (req.getMainCauses() != null && !req.getMainCauses().isEmpty())
            parts.add("주요 사유: " + String.join(", ", req.getMainCauses()));
        List<String> claims = new ArrayList<>();
        if (Boolean.TRUE.equals(req.getHasAlimonyClaim()))
            claims.add("위자료");
        if (Boolean.TRUE.equals(req.getHasCustodyClaim()))
            claims.add("양육권");
        if (!claims.isEmpty())
            parts.add("청구: " + claims.stream().collect(Collectors.joining(" ")));
        String base = String.join(", ", parts);
        return (base.isBlank() ? "이혼 분쟁" : base) + " 상황에서 유사 판례와 판단 기준을 알려주세요.";
    }
}
