package com.example.demo.controller;

import com.example.demo.dto.IdentifyRequest;
import com.example.demo.dto.IdentifyResponse;
import com.example.demo.service.ContactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/identify")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService service;

    @PostMapping
    public ResponseEntity<IdentifyResponse> identify(@RequestBody IdentifyRequest request) {
        IdentifyResponse resp = service.identify(request);
        return ResponseEntity.ok(resp);
    }
}
