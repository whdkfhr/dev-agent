package com.arok2.dev_agent.design.controller;

import com.arok2.dev_agent.design.dto.DesignCreateRequest;
import com.arok2.dev_agent.design.dto.DesignResponse;
import com.arok2.dev_agent.design.service.DesignService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/designs")
public class DesignController {

    private final DesignService designService;

    public DesignController(DesignService designService) {
        this.designService = designService;
    }

    @PostMapping
    public ResponseEntity<DesignResponse> createDesign(@RequestBody DesignCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(designService.createDesign(request));
    }

    @GetMapping
    public ResponseEntity<List<DesignResponse>> getAllDesigns() {
        return ResponseEntity.ok(designService.getAllDesigns());
    }

    @GetMapping("/{designId}")
    public ResponseEntity<DesignResponse> getDesign(@PathVariable String designId) {
        return ResponseEntity.ok(designService.getDesign(designId));
    }

    @GetMapping("/by-task/{taskId}")
    public ResponseEntity<DesignResponse> getDesignByTaskId(@PathVariable String taskId) {
        return ResponseEntity.ok(designService.getDesignByTaskId(taskId));
    }
}
