package com.arok2.dev_agent.agentrun.controller;

import com.arok2.dev_agent.agentrun.dto.ToolDefinitionResponse;
import com.arok2.dev_agent.agentrun.service.ToolRegistryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tools")
public class ToolRegistryController {

    private final ToolRegistryService toolRegistryService;

    public ToolRegistryController(ToolRegistryService toolRegistryService) {
        this.toolRegistryService = toolRegistryService;
    }

    @GetMapping
    public ResponseEntity<List<ToolDefinitionResponse>> getAllTools() {
        return ResponseEntity.ok(toolRegistryService.getAllTools());
    }

    @GetMapping("/{toolName}")
    public ResponseEntity<ToolDefinitionResponse> getTool(@PathVariable String toolName) {
        return ResponseEntity.ok(toolRegistryService.getTool(toolName));
    }
}
