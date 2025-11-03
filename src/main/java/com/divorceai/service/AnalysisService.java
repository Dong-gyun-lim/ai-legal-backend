package com.divorceai.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import com.divorceai.mapper.AnalyzeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AnalyzeMapper analyzeMapper;

    @Value("${flask.base-url:http://127.0.0.1:5001}")
    private String flaskBaseUrl;

    /**
     * Flask /health 프록시 (ApiController에서 합쳐서 보여줌)
     */
    public Map<String, Object> health() {
        Map<String, Object> r = new HashMap<>();
        try {
            String s = restTemplate.getForObject(flaskBaseUrl + "/health", String.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> m = objectMapper.readValue(s, Map.class);
            r.put("ok", true);
            r.putAll(m);
        } catch (Exception e) {
            r.put("ok", false);
            r.put("error", "Flask health request failed: " + e.getMessage());
            r.put("flaskBaseUrl", flaskBaseUrl);
        }
        return r;
    }

    /**
     * 분석 실행: Flask /rag 호출 → DTO 매핑 → DB 저장(analysis_results)
     */
    public AnalyzeResponse analyze(AnalyzeRequest req) {
        AnalyzeResponse out = new AnalyzeResponse();
        try {
            // 1) Flask 호출 페이로드
            Map<String, Object> payload = new HashMap<>();
            payload.put("question", buildQuestion(req));
            payload.put("top_k", req.getTopK() != null ? req.getTopK() : 5);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);

            // 2) 호출
            ResponseEntity<String> resp = restTemplate.exchange(
                    flaskBaseUrl + "/rag", HttpMethod.POST, entity, String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                out.setOk(false);
                out.setError("Flask returned " + resp.getStatusCode());
                return out;
            }

            // 3) 응답 매핑
            JsonNode json = objectMapper.readTree(resp.getBody());

            out.setOk(true);
            out.setAnswer(json.path("answer").asText(""));
            // 선택 수치들(없으면 기본값)
            if (json.has("avg_similarity"))
                out.setSimilarity(json.get("avg_similarity").asDouble(0));
            if (json.has("damages"))
                out.setDamages(json.get("damages").asInt(0));
            if (json.has("custody"))
                out.setCustody(json.get("custody").asText(""));

            // references
            // references
            if (json.has("references") && json.get("references").isArray()) {
                List<AnalyzeResponse.ReferenceCase> refs = new ArrayList<>();
                for (JsonNode n : json.get("references")) {
                    AnalyzeResponse.ReferenceCase rc = new AnalyzeResponse.ReferenceCase();
                    rc.setCaseNo(n.path("case_no").asText(null));
                    rc.setCourt(n.path("court").asText(null));
                    rc.setJudgmentDate(n.path("judgment_date").asText(null));

                    int score = 0;
                    if (n.has("score")) {
                        if (n.get("score").isNumber()) {
                            double val = n.get("score").asDouble();
                            score = (val <= 1.0) ? (int) Math.round(val * 100)
                                    : (int) Math.round(val);
                        }
                    }
                    rc.setScore(score);

                    rc.setSectionName(n.path("section_name").asText(null));
                    rc.setText(n.path("text").asText(null));
                    refs.add(rc);
                }
                out.setReferences(refs);
            }

            // explanation
            if (json.has("explanation")) {
                AnalyzeResponse.Explanation exp = new AnalyzeResponse.Explanation();
                JsonNode expNode = json.get("explanation");
                exp.setReasoning(expNode.path("reasoning").asText(""));
                // factors
                if (expNode.has("factors") && expNode.get("factors").isArray()) {
                    List<AnalyzeResponse.Factor> fs = new ArrayList<>();
                    for (JsonNode f : expNode.get("factors")) {
                        fs.add(new AnalyzeResponse.Factor(
                                f.path("name").asText(""),
                                f.path("weight").asDouble(0),
                                f.path("evidence").asText("")));
                    }
                    exp.setFactors(fs);
                }
                // highlights
                if (expNode.has("highlights") && expNode.get("highlights").isArray()) {
                    List<AnalyzeResponse.Highlight> hs = new ArrayList<>();
                    for (JsonNode h : expNode.get("highlights")) {
                        hs.add(new AnalyzeResponse.Highlight(
                                h.path("case_no").asText(null),
                                h.path("chunk_index").asInt(-1),
                                h.path("span").asText(""),
                                h.path("tag").asText("")));
                    }
                    exp.setHighlights(hs);
                }
                out.setExplanation(exp);
            }

            // 4) DB 저장 (analysis_results 스키마에 맞춰)
            // user_id / is_guest / intake_json / similarity / damages / custody /
            // ai_summary / case_list_json
            String userId = req.getUserEmail(); // 로그인 이메일(없으면 null)
            boolean isGuest = (userId == null || userId.isBlank());

            Map<String, Object> intakeMap = new LinkedHashMap<>();
            // 정형 입력 요약을 intake_json으로 남김 (필요한 것만)
            intakeMap.put("gender", req.getGender());
            intakeMap.put("age", req.getAge());
            intakeMap.put("marriageYears", req.getMarriageYears());
            intakeMap.put("childCount", req.getChildCount());
            intakeMap.put("caseTypes", req.getCaseTypes());
            intakeMap.put("role", req.getRole());
            intakeMap.put("mainCauses", req.getMainCauses());

            Long id = analyzeMapper.insertAnalysisResult(
                    userId,
                    isGuest,
                    objectMapper.writeValueAsString(intakeMap),
                    out.getSimilarity() == null ? null : out.getSimilarity().intValue(),
                    out.getDamages(),
                    out.getCustody(),
                    out.getAnswer(),
                    objectMapper.writeValueAsString(out.getReferences() == null ? List.of() : out.getReferences()));
            // id가 필요하면 out에 추가 필드 만들어 넣어도 됨.

            return out;
        } catch (Exception e) {
            out.setOk(false);
            out.setError(e.getMessage());
            return out;
        }
    }

    /** 자유질문 없을 때 정형입력으로 간단 질문 생성 */
    private String buildQuestion(AnalyzeRequest r) {
        if (r.getQuestion() != null && !r.getQuestion().isBlank())
            return r.getQuestion();
        List<String> parts = new ArrayList<>();
        if (r.getGender() != null)
            parts.add("성별=" + r.getGender());
        if (r.getAge() != null)
            parts.add("나이=" + r.getAge());
        if (r.getMarriageYears() != null)
            parts.add("혼인기간=" + r.getMarriageYears() + "년");
        if (r.getChildCount() != null)
            parts.add("자녀=" + r.getChildCount());
        if (r.getMainCauses() != null && !r.getMainCauses().isEmpty())
            parts.add("사유=" + String.join(",", r.getMainCauses()));
        String base = String.join(" | ", parts);
        return "다음 사건 정보를 바탕으로 위자료/양육/재산분할 경향을 요약해줘.\n" + base;
    }
}
