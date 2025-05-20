package com.example.demo.service;

import com.example.demo.dto.IdentifyRequest;
import com.example.demo.dto.IdentifyResponse;

public interface ContactService {
    IdentifyResponse identify(IdentifyRequest request);
}
