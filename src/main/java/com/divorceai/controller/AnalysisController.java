package com.divorceai.controller;

import com.divorceai.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Analysis", description = "Flask AI 분석 연동 API")
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /** 헬스체크: Spring → Flask(/health) */
    @Operation(
        summary = "Flask 헬스체크",
        description = "Spring이 내부에서 Flask의 /health 엔드포인트를 호출해 상태를 반환합니다.",
        responses = {
            @ApiResponse(responseCode = "200", description = "정상 응답",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                    {
                      "ok": true,
                      "status": "healthy",
                      "source": "flask"
                    }
                    """)))
        }
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(analysisService.health());
    }

    /**
     * 분석 실행: Spring → Flask(/predict)
     * - URL의 {id}는 DB에서 Request를 불러올 때 사용
     * - Body가 있으면(DB 없이) 그걸 그대로 Flask로 전달해서도 테스트 가능
     */
    @Operation(
        summary = "분석 실행",
        description = """
            DB의 request {id}를 조회해 Flask /predict로 전달합니다.
            Body를 함께 보내면(DB 미사용 테스트) 해당 Body를 그대로 Flask로 전달합니다.
            """,
        requestBody = @RequestBody(required = false,
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = """
                {
                  "caseType": "이혼",
                  "marriageYears": 6,
                  "childCount": 1,
                  "reason": "상습 폭언 및 경제적 방임",
                  "summary": "혼인 6년, 미성년 1명. 최근 2년간 폭언·생활비 미지급."
                }
                """)
            )
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "Flask 분석 결과 또는 에러 JSON",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(example = """
                    {
                      "ok": true,
                      "requestId": 1,
                      "source": "flask",
                      "similar_cases": [
                        { "case_id": "2020Da12345", "score": 0.87 }
                      ],
                      "trend": {
                        "alimony_range": [3000000, 10000000],
                        "custody_likelihood": { "mother": 0.7, "father": 0.3 }
                      }
                    }
                    """)))
        }
    )
    @PostMapping("/{id}")
    public ResponseEntity<Map<String, Object>> analyze(
            @PathVariable("id") Long id,
            @org.springframework.web.bind.annotation.RequestBody(required = false) Map<String, Object> body
    ) {
        Map<String, Object> result = analysisService.predict(id, body);
        return ResponseEntity.ok(result);
    }
}
