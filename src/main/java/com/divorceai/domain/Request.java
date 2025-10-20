package com.divorceai.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Request {
    private Long id;
    private String caseType;
    private String gender;
    private String ageRange;
    private Integer marriageYears;
    private Integer childCount;
    private String reason;
    private Boolean claimDamages;
    private Boolean claimCustody;
    private Boolean claimProperty;
    private String freeText;
    private LocalDateTime createdAt;
    private String summary;
    private String title;
}
