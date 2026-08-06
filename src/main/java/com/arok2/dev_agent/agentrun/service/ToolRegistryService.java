package com.arok2.dev_agent.agentrun.service;

import com.arok2.dev_agent.agentrun.domain.AgentTool;
import com.arok2.dev_agent.agentrun.dto.ToolDefinitionResponse;
import com.arok2.dev_agent.common.exception.ToolNotFoundException;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ToolRegistryService {

    private final Map<String, AgentTool> tools = new LinkedHashMap<>();

    public ToolRegistryService() {
        register(new AgentTool(
                "RepositoryContextTool",
                "README, build.gradle, package structure를 조회한다.",
                "repositoryRoot",
                "repositoryContext"
        ));
        register(new AgentTool(
                "TaskDocumentTool",
                "TASK 문서를 생성하고 필수 섹션을 검증한다.",
                "issueTitle, issueBody",
                "taskDocumentPath, validationResult"
        ));
        register(new AgentTool(
                "DesignDocumentTool",
                "TASK 문서를 기반으로 DESIGN 문서를 생성하고 검증한다.",
                "taskDocumentPath",
                "designDocumentPath, validationResult"
        ));
        register(new AgentTool(
                "CodePatchTool",
                "Agent가 생성한 파일 블록을 파싱하고 코드 패치 계획으로 변환한다.",
                "generatedFileBlocks",
                "patchPlan, changedFiles"
        ));
        register(new AgentTool(
                "TestRunnerTool",
                "./gradlew test 실행 결과를 수집한다.",
                "testCommand",
                "exitCode, stdout, stderr"
        ));
        register(new AgentTool(
                "ReviewTool",
                "diff와 테스트 결과를 기반으로 구조화된 리뷰를 생성한다.",
                "diff, testResult",
                "reviewStatus, comments"
        ));
        register(new AgentTool(
                "PullRequestTool",
                "승인된 구현 결과로 PR 제목과 본문을 생성한다.",
                "taskDocumentPath, designDocumentPath, reviewResult",
                "title, body"
        ));
    }

    public List<ToolDefinitionResponse> getAllTools() {
        return tools.values().stream()
                .map(this::toResponse)
                .toList();
    }

    public ToolDefinitionResponse getTool(String toolName) {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            throw new ToolNotFoundException(toolName);
        }
        return toResponse(tool);
    }

    private void register(AgentTool tool) {
        tools.put(tool.name(), tool);
    }

    private ToolDefinitionResponse toResponse(AgentTool tool) {
        return new ToolDefinitionResponse(
                tool.name(),
                tool.description(),
                tool.inputSchema(),
                tool.outputSchema()
        );
    }
}
