package com.divorceai.service;

import com.divorceai.domain.Request;
import com.divorceai.mapper.RequestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestService {
    private final RequestMapper mapper;

    public void save(Request request) {
        mapper.insertRequest(request);
    }

    public List<Request> getAll() {
        return mapper.findAll();
    }

    public Request getById(Long id) {
        return mapper.findById(id);
    }
}
