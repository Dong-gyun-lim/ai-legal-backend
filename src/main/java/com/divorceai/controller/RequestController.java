package com.divorceai.controller;

import com.divorceai.domain.Request;
import com.divorceai.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService service;

    @PostMapping
    public ResponseEntity<String> create(@RequestBody Request request) {
        service.save(request);
        return ResponseEntity.ok("created");
    }

    @GetMapping
    public List<Request> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public Request getById(@PathVariable Long id) {
        return service.getById(id);
    }
}
