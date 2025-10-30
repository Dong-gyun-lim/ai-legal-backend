package com.divorceai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.divorceai.domain.dto.AnalyzeResponse;

@Mapper
public interface AnalyzeMapper {

    void insertAnalyzeResult(
            @Param("requestId") Long requestId,
            @Param("response") AnalyzeResponse response);
}
