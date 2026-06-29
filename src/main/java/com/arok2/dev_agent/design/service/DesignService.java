package com.arok2.dev_agent.design.service;

import com.arok2.dev_agent.common.exception.DesignAlreadyExistsException;
import com.arok2.dev_agent.common.exception.DesignNotFoundException;
import com.arok2.dev_agent.design.domain.Design;
import com.arok2.dev_agent.design.dto.DesignCreateRequest;
import com.arok2.dev_agent.design.dto.DesignResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DesignService {

    private final Map<String, Design> designById = new ConcurrentHashMap<>();
    private final Map<String, Design> designByTaskId = new ConcurrentHashMap<>();

    public DesignResponse createDesign(DesignCreateRequest request) {
        if (designById.containsKey(request.designId())) {
            throw new DesignAlreadyExistsException(request.designId());
        }

        Design design = new Design(
                request.designId(),
                request.taskId(),
                request.content(),
                Instant.now()
        );
        designById.put(design.designId(), design);
        designByTaskId.put(design.taskId(), design);

        return toResponse(design);
    }

    public List<DesignResponse> getAllDesigns() {
        return designById.values().stream()
                .map(this::toResponse)
                .toList();
    }

    public DesignResponse getDesign(String designId) {
        Design design = designById.get(designId);
        if (design == null) {
            throw new DesignNotFoundException("Design not found: " + designId);
        }
        return toResponse(design);
    }

    public DesignResponse getDesignByTaskId(String taskId) {
        Design design = designByTaskId.get(taskId);
        if (design == null) {
            throw new DesignNotFoundException("Design not found for task: " + taskId);
        }
        return toResponse(design);
    }

    private DesignResponse toResponse(Design design) {
        return new DesignResponse(design.designId(), design.taskId(), design.createdAt());
    }
}
