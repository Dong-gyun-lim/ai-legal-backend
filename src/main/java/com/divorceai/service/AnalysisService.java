package com.divorceai.service;

import com.divorceai.domain.Request;
import com.divorceai.mapper.RequestMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AnalysisService {

    private final RestTemplate restTemplate;
    private final RequestMapper requestMapper;

    @Value("${flask.base-url:http://127.0.0.1:5001}")
    private String flaskBaseUrl;

    public AnalysisService(RestTemplate restTemplate, RequestMapper requestMapper) {
        this.restTemplate = restTemplate;
        this.requestMapper = requestMapper;
    }

    /** Flask /health 프록시 */
    public Map<String, Object> health() {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.getForObject("/health", Map.class);
            if (res == null) res = new HashMap<>();
            res.putIfAbsent("ok", true);
            res.putIfAbsent("source", "flask");
            return res;
        } catch (RestClientException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", "Flask health request failed: " + e.getMessage());
            err.put("flaskBaseUrl", flaskBaseUrl);
            return err;
        }
    }

    /**
     * Flask /predict 호출
     * - 우선 DB에서 requestId로 Request를 읽어 payload 생성
     * - 만약 body가 들어오면(프론트 직결 테스트) DB 대신 body를 그대로 전달
     */
    public Map<String, Object> predict(Long requestId, Map<String, Object> body) {
        Map<String, Object> payload;

        if (body != null && !body.isEmpty()) {
            // 프론트에서 직접 보낸 payload가 있으면 우선 사용
            payload = new HashMap<>(body);
            payload.putIfAbsent("requestId", requestId);
        } else {
            // DB에서 불러와 payload 구성
            Optional<Request> reqOpt = Optional.ofNullable(requestMapper.findById(requestId));
            if (reqOpt.isEmpty()) {
                Map<String, Object> err = new HashMap<>();
                err.put("ok", false);
                err.put("error", "Request not found: id=" + requestId);
                return err;
            }
            Request r = reqOpt.get();

            payload = new HashMap<>();
            payload.put("requestId", requestId);
            payload.put("caseType", r.getCaseType());
            payload.put("gender", r.getGender());
            payload.put("ageRange", r.getAgeRange());
            payload.put("marriageYears", r.getMarriageYears());
            payload.put("childCount", r.getChildCount());
            payload.put("reason", r.getReason());
            payload.put("title", r.getTitle());     // 도메인에 title 필드가 있지 않다면 제거해도 OK
            payload.put("summary", r.getSummary()); // 도메인에 summary 필드가 없으면 제거

            // Flask 쪽이 원하는 키를 동시에 제공(유연성)
            Map<String, Object> facts = new HashMap<>();
            facts.put("case_type", r.getCaseType());
            facts.put("years", r.getMarriageYears());
            facts.put("children", r.getChildCount());
            facts.put("reason", r.getReason());
            payload.put("facts", facts);
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> res = restTemplate.postForObject("/predict", payload, Map.class);
            if (res == null) res = new HashMap<>();
            res.put("ok", true);
            res.putIfAbsent("source", "flask");
            res.putIfAbsent("requestId", requestId);
            return res;
        } catch (RestClientException e) {
            Map<String, Object> err = new HashMap<>();
            err.put("ok", false);
            err.put("error", "Flask predict request failed: " + e.getMessage());
            err.put("payloadSent", payload);
            err.put("flaskBaseUrl", flaskBaseUrl);
            return err;
        }
    }
}
