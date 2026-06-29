package com.arok2.dev_agent.implementation.service;

import com.arok2.dev_agent.common.exception.ImplementationAlreadyExistsException;
import com.arok2.dev_agent.common.exception.ImplementationNotFoundException;
import com.arok2.dev_agent.implementation.domain.Implementation;
import com.arok2.dev_agent.implementation.dto.ImplementationCreateRequest;
import com.arok2.dev_agent.implementation.dto.ImplementationResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ImplementationService {

    private final Map<String, Implementation> implById = new ConcurrentHashMap<>();
    private final Map<String, Implementation> implByTaskId = new ConcurrentHashMap<>();

    public ImplementationResponse createImplementation(ImplementationCreateRequest request) {
        if (implById.containsKey(request.implId())) {
            throw new ImplementationAlreadyExistsException(request.implId());
        }
        Implementation impl = new Implementation(
                request.implId(),
                request.taskId(),
                request.designId(),
                request.status(),
                request.generatedFiles(),
                Instant.now()
        );
        implById.put(impl.implId(), impl);
        implByTaskId.put(impl.taskId(), impl);
        return toResponse(impl);
    }

    public List<ImplementationResponse> getAllImplementations() {
        return implById.values().stream()
                .map(this::toResponse)
                .toList();
    }

    public ImplementationResponse getImplementation(String implId) {
        Implementation impl = implById.get(implId);
        if (impl == null) {
            throw new ImplementationNotFoundException("Implementation not found: " + implId);
        }
        return toResponse(impl);
    }

    public ImplementationResponse getImplementationByTaskId(String taskId) {
        Implementation impl = implByTaskId.get(taskId);
        if (impl == null) {
            throw new ImplementationNotFoundException("Implementation not found for task: " + taskId);
        }
        return toResponse(impl);
    }

    private ImplementationResponse toResponse(Implementation impl) {
        return new ImplementationResponse(
                impl.implId(),
                impl.taskId(),
                impl.designId(),
                impl.status(),
                impl.generatedFiles(),
                impl.createdAt()
        );
    }
}
