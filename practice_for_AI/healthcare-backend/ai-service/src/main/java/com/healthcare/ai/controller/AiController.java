package com.healthcare.ai.controller;

import com.healthcare.ai.dto.AiQueryRequest;
import com.healthcare.ai.dto.AiQueryResponse;
import com.healthcare.ai.service.AiAssistantService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiAssistantService aiAssistantService;

    public AiController(AiAssistantService aiAssistantService) {
        this.aiAssistantService = aiAssistantService;
    }

    @PostMapping("/ask")
    public AiQueryResponse ask(@Valid @RequestBody AiQueryRequest request) {
        return aiAssistantService.ask(request);
    }
}
