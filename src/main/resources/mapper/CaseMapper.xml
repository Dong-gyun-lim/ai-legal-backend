package com.divorceai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CaseMapper {
    int upsertCase(@Param("caseNo") String caseNo,
                   @Param("title") String title,
                   @Param("summary") String summary,
                   @Param("court") String court,
                   @Param("judgedAt") String judgedAt, // 간단히 String → 나중에 LocalDate
                   @Param("url") String url);
}
