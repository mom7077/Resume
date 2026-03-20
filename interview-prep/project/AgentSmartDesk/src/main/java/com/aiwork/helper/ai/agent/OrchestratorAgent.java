package com.aiwork.helper.ai.agent;

import com.aiwork.helper.ai.tools.ChatTools;
import com.aiwork.helper.ai.tools.TodoTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Orchestrator Agent（主 Agent）
 *
 * 职责：
 * 1. 持有用户会话记忆（Memory）
 * 2. 理解用户意图，路由给对应子 Agent
 * 3. 汇总子 Agent 结果，组织最终回复
 *
 * 与旧 AgentService 的区别：
 * - 不直接持有业务工具，只持有路由工具（RoutingTools）
 * - 工具数量从 16 个降为当前 1 个（随子 Agent 增加而扩展到 3 个）
 * - System Prompt 聚焦在"如何路由"，不描述具体工具参数
 */
@Slf4j
@Service
public class OrchestratorAgent {

    private final ChatClient orchestratorChatClient;

    public OrchestratorAgent(@Qualifier("orchestratorChatClient") ChatClient orchestratorChatClient) {
        this.orchestratorChatClient = orchestratorChatClient;
    }

    /**
     * 对话入口（替代旧 AgentService.chat）
     *
     * @param userId     当前用户 ID，同时作为会话记忆 ID
     * @param userInput  用户输入
     * @param relationId 关联 ID（群聊 ID 等，用于聊天记录查询）
     * @param startTime  时间范围起点（可选）
     * @param endTime    时间范围终点（可选）
     */
    public String chat(String userId, String userInput, String relationId, Long startTime, Long endTime) {
        log.info("OrchestratorAgent 收到请求: userId={}, input={}", userId, userInput);

        try {
            // 注入上下文到 ThreadLocal，供 RoutingTools 内部转发给子 Agent 时读取
            TodoTools.setCurrentUserId(userId);
            ChatTools.setChatContext(relationId, startTime, endTime);

            String response = orchestratorChatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(userInput)
                    .advisors(spec -> spec.param("chat_memory_conversation_id", "orch:" + userId))
                    .call()
                    .content();

            log.info("OrchestratorAgent 响应完成: userId={}", userId);
            return response;

        } catch (Exception e) {
            log.error("OrchestratorAgent 处理失败: userId={}", userId, e);
            return "抱歉，处理请求时发生错误: " + e.getMessage();
        } finally {
            TodoTools.clearCurrentUserId();
            ChatTools.clearChatContext();
        }
    }

    private String buildSystemPrompt() {
        return """
                你是一个智能工作助手，负责理解用户意图并分配给合适的专员处理。

                ## 你可以调用的专员

                - handleWorkflow：处理审批（请假/补卡/外出）和待办任务的创建与查询
                - handleKnowledge：查询公司制度/员工手册，或维护知识库（上传PDF/清空）
                - handleChatAnalysis：获取或总结群聊/私聊的聊天记录

                ## 路由规则

                1. 用户说请假、补卡、外出、待办、任务 → handleWorkflow
                2. 用户问公司制度、规章、政策、手册，或要更新/清空知识库 → handleKnowledge
                3. 用户要总结群聊、查看聊天记录、归纳消息 → handleChatAnalysis
                4. 请求同时涉及多个专员（如"总结群聊后帮我创建待办"）→ 同时调用多个路由工具，结果并行返回后汇总
                5. 纯聊天或问候 → 直接回复，不调用任何工具

                ## 注意事项
                - 把用户的完整描述（包括时间、原因、人名）完整传给专员，不要省略细节
                - 多个专员的结果都返回后，整合成一个连贯的回复给用户
                - 用中文回复，保持友好专业
                """;
    }
}
