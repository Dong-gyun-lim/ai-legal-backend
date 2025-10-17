package com.divorceai.mapper;

import com.divorceai.domain.Request;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RequestMapper {
    void insertRequest(Request request);
    List<Request> findAll();
    Request findById(@Param("id") Long id);
}
