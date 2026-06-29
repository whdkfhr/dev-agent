package com.arok2.dev_agent.implementation.controller;

import com.arok2.dev_agent.implementation.dto.ImplementationCreateRequest;
import com.arok2.dev_agent.implementation.dto.ImplementationResponse;
import com.arok2.dev_agent.implementation.service.ImplementationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/implementations")
public class ImplementationController {

    private final ImplementationService implementationService;

    public ImplementationController(ImplementationService implementationService) {
        this.implementationService = implementationService;
    }

    @PostMapping
    public ResponseEntity<ImplementationResponse> createImplementation(@RequestBody ImplementationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(implementationService.createImplementation(request));
    }

    @GetMapping
    public ResponseEntity<List<ImplementationResponse>> getAllImplementations() {
        return ResponseEntity.ok(implementationService.getAllImplementations());
    }

    @GetMapping("/{implId}")
    public ResponseEntity<ImplementationResponse> getImplementation(@PathVariable String implId) {
        return ResponseEntity.ok(implementationService.getImplementation(implId));
    }

    @GetMapping("/by-task/{taskId}")
    public ResponseEntity<ImplementationResponse> getImplementationByTaskId(@PathVariable String taskId) {
        return ResponseEntity.ok(implementationService.getImplementationByTaskId(taskId));
    }
}
