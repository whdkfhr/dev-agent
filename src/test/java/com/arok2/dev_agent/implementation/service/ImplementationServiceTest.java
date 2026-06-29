package com.arok2.dev_agent.implementation.service;

import com.arok2.dev_agent.common.exception.ImplementationAlreadyExistsException;
import com.arok2.dev_agent.common.exception.ImplementationNotFoundException;
import com.arok2.dev_agent.implementation.dto.ImplementationCreateRequest;
import com.arok2.dev_agent.implementation.dto.ImplementationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImplementationServiceTest {

    private ImplementationService implementationService;

    @BeforeEach
    void setUp() {
        implementationService = new ImplementationService();
    }

    @Test
    void createImplementation_저장_후_응답_반환() {
        ImplementationCreateRequest request = new ImplementationCreateRequest(
                "IMPL-001", "TASK-001", "DESIGN-001", "GENERATED",
                List.of("src/main/java/Foo.java")
        );

        ImplementationResponse response = implementationService.createImplementation(request);

        assertThat(response.implId()).isEqualTo("IMPL-001");
        assertThat(response.taskId()).isEqualTo("TASK-001");
        assertThat(response.designId()).isEqualTo("DESIGN-001");
        assertThat(response.status()).isEqualTo("GENERATED");
        assertThat(response.generatedFiles()).containsExactly("src/main/java/Foo.java");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void createImplementation_중복_implId_예외() {
        ImplementationCreateRequest request = new ImplementationCreateRequest(
                "IMPL-001", "TASK-001", "DESIGN-001", "GENERATED", List.of()
        );
        implementationService.createImplementation(request);

        assertThatThrownBy(() -> implementationService.createImplementation(request))
                .isInstanceOf(ImplementationAlreadyExistsException.class)
                .hasMessageContaining("IMPL-001");
    }

    @Test
    void getAllImplementations_전체_목록_반환() {
        implementationService.createImplementation(new ImplementationCreateRequest(
                "IMPL-001", "TASK-001", "DESIGN-001", "GENERATED", List.of()
        ));
        implementationService.createImplementation(new ImplementationCreateRequest(
                "IMPL-002", "TASK-002", "DESIGN-002", "GENERATED", List.of()
        ));

        List<ImplementationResponse> implementations = implementationService.getAllImplementations();

        assertThat(implementations).hasSize(2);
    }

    @Test
    void getImplementation_존재하는_implId_반환() {
        implementationService.createImplementation(new ImplementationCreateRequest(
                "IMPL-001", "TASK-001", "DESIGN-001", "GENERATED", List.of()
        ));

        ImplementationResponse response = implementationService.getImplementation("IMPL-001");

        assertThat(response.implId()).isEqualTo("IMPL-001");
    }

    @Test
    void getImplementation_존재하지않는_implId_예외() {
        assertThatThrownBy(() -> implementationService.getImplementation("IMPL-999"))
                .isInstanceOf(ImplementationNotFoundException.class)
                .hasMessageContaining("IMPL-999");
    }

    @Test
    void getImplementationByTaskId_연관_Implementation_반환() {
        implementationService.createImplementation(new ImplementationCreateRequest(
                "IMPL-001", "TASK-001", "DESIGN-001", "GENERATED", List.of()
        ));

        ImplementationResponse response = implementationService.getImplementationByTaskId("TASK-001");

        assertThat(response.taskId()).isEqualTo("TASK-001");
        assertThat(response.implId()).isEqualTo("IMPL-001");
    }

    @Test
    void getImplementationByTaskId_존재하지않는_taskId_예외() {
        assertThatThrownBy(() -> implementationService.getImplementationByTaskId("TASK-999"))
                .isInstanceOf(ImplementationNotFoundException.class)
                .hasMessageContaining("TASK-999");
    }
}
