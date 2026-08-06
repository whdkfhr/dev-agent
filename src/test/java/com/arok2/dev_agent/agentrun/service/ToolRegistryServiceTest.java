package com.arok2.dev_agent.agentrun.service;

import com.arok2.dev_agent.agentrun.dto.ToolDefinitionResponse;
import com.arok2.dev_agent.common.exception.ToolNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolRegistryServiceTest {

    private ToolRegistryService toolRegistryService;

    @BeforeEach
    void setUp() {
        toolRegistryService = new ToolRegistryService();
    }

    @Test
    void getAllTools_등록된_7개_tool_반환() {
        List<ToolDefinitionResponse> tools = toolRegistryService.getAllTools();

        assertThat(tools).hasSize(7);
        assertThat(tools)
                .extracting(ToolDefinitionResponse::name)
                .containsExactly(
                        "RepositoryContextTool",
                        "TaskDocumentTool",
                        "DesignDocumentTool",
                        "CodePatchTool",
                        "TestRunnerTool",
                        "ReviewTool",
                        "PullRequestTool"
                );
    }

    @Test
    void getTool_존재하는_tool_반환() {
        ToolDefinitionResponse tool = toolRegistryService.getTool("TestRunnerTool");

        assertThat(tool.name()).isEqualTo("TestRunnerTool");
        assertThat(tool.description()).contains("./gradlew test");
        assertThat(tool.outputSchema()).contains("exitCode");
    }

    @Test
    void getTool_존재하지않는_tool_예외() {
        assertThatThrownBy(() -> toolRegistryService.getTool("UnknownTool"))
                .isInstanceOf(ToolNotFoundException.class)
                .hasMessageContaining("UnknownTool");
    }
}
