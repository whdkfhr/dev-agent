package com.arok2.dev_agent.design.service;

import com.arok2.dev_agent.common.exception.DesignAlreadyExistsException;
import com.arok2.dev_agent.common.exception.DesignNotFoundException;
import com.arok2.dev_agent.design.dto.DesignCreateRequest;
import com.arok2.dev_agent.design.dto.DesignResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DesignServiceTest {

    private DesignService designService;

    @BeforeEach
    void setUp() {
        designService = new DesignService();
    }

    @Test
    void createDesign_저장_후_응답_반환() {
        DesignCreateRequest request = new DesignCreateRequest("DESIGN-001", "TASK-001", "설계 내용");

        DesignResponse response = designService.createDesign(request);

        assertThat(response.designId()).isEqualTo("DESIGN-001");
        assertThat(response.taskId()).isEqualTo("TASK-001");
        assertThat(response.createdAt()).isNotNull();
    }

    @Test
    void createDesign_중복_designId_예외() {
        DesignCreateRequest request = new DesignCreateRequest("DESIGN-001", "TASK-001", "내용");
        designService.createDesign(request);

        assertThatThrownBy(() -> designService.createDesign(request))
                .isInstanceOf(DesignAlreadyExistsException.class)
                .hasMessageContaining("DESIGN-001");
    }

    @Test
    void getAllDesigns_전체_목록_반환() {
        designService.createDesign(new DesignCreateRequest("DESIGN-001", "TASK-001", "내용1"));
        designService.createDesign(new DesignCreateRequest("DESIGN-002", "TASK-002", "내용2"));

        List<DesignResponse> designs = designService.getAllDesigns();

        assertThat(designs).hasSize(2);
    }

    @Test
    void getDesign_존재하는_designId_반환() {
        designService.createDesign(new DesignCreateRequest("DESIGN-001", "TASK-001", "내용"));

        DesignResponse response = designService.getDesign("DESIGN-001");

        assertThat(response.designId()).isEqualTo("DESIGN-001");
    }

    @Test
    void getDesign_존재하지않는_designId_예외() {
        assertThatThrownBy(() -> designService.getDesign("DESIGN-999"))
                .isInstanceOf(DesignNotFoundException.class)
                .hasMessageContaining("DESIGN-999");
    }

    @Test
    void getDesignByTaskId_연관_Design_반환() {
        designService.createDesign(new DesignCreateRequest("DESIGN-001", "TASK-001", "내용"));

        DesignResponse response = designService.getDesignByTaskId("TASK-001");

        assertThat(response.taskId()).isEqualTo("TASK-001");
        assertThat(response.designId()).isEqualTo("DESIGN-001");
    }

    @Test
    void getDesignByTaskId_존재하지않는_taskId_예외() {
        assertThatThrownBy(() -> designService.getDesignByTaskId("TASK-999"))
                .isInstanceOf(DesignNotFoundException.class)
                .hasMessageContaining("TASK-999");
    }
}
