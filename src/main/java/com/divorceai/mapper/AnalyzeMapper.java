package com.divorceai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnalyzeMapper {

    Long insertAnalysisResult(
            @Param("userId") String userId,
            @Param("isGuest") boolean isGuest,
            @Param("intakeJson") String intakeJson,
            @Param("similarity") Integer similarity, // NULL 가능
            @Param("damages") Integer damages, // NULL 가능
            @Param("custody") String custody, // NULL 가능
            @Param("aiSummary") String aiSummary, // answer
            @Param("caseListJson") String caseListJson);
}
