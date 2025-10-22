package com.divorceai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CaseMapper {
    int upsertCase(@Param("caseNo") String caseNo,
            @Param("court") String court,
            @Param("judgedAt") String judgedAt,
            @Param("type") String type,
            @Param("summary") String summary,
            @Param("url") String url,
            @Param("fullText") String fullText);
}
