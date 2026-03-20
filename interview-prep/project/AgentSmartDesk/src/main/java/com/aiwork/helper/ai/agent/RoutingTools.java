package com.aiwork.helper.ai.agent;

import com.aiwork.helper.ai.tools.ChatTools;
import com.aiwork.helper.ai.tools.TodoTools;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Orchestrator 路由工具集
 *
 * 每个 @Tool 方法对应一个子 Agent。
 * LLM 判断意图后调用对应的路由工具，工具内部转发给子 Agent 执行。
 *
 * 用户 ID 通过 TodoTools.getCurrentUserId() 读取
 * 聊天上下文通过 ChatTools.getChatContext() 读取
 * （均由 OrchestratorAgent 在调用前通过 ThreadLocal 注入）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoutingTools {

    private final WorkflowAgent workflowAgent;
    private final KnowledgeAgent knowledgeAgent;
    private final ChatAnalysisAgent chatAnalysisAgent;

    /**
     * 路由到工作流子 Agent
     * 处理：请假/补卡/外出审批，待办任务创建与查询
     */
    @Tool(description = """
            处理所有审批和待办任务，包括：
            - 请假申请（事假/病假/年假等）
            - 补卡申请
            - 外出申请
            - 创建待办事项
            - 查询审批记录或待办列表
            """)
    public String handleWorkflow(
            @ToolParam(description = "用户的完整任务描述，包含所有细节") String task
    ) {
        String userId = TodoTools.getCurrentUserId();
        log.info("路由到 WorkflowAgent: userId={}, task={}", userId, task);

        try {
            return workflowAgent.execute(userId, task)
                    .get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("WorkflowAgent 执行超时: userId={}", userId);
            return "工作流处理超时，请稍后重试。";
        } catch (Exception e) {
            log.error("WorkflowAgent 执行异常: userId={}", userId, e);
            return "工作流处理失败: " + e.getMessage();
        }
    }

    /**
     * 路由到知识库子 Agent
     * 处理：公司制度查询、知识库更新/清空
     */
    @Tool(description = """
            查询或维护知识库，包括：
            - 查询公司制度、规章、员工手册、请假政策、考勤制度等
            - 根据上传的 PDF 文件更新知识库
            - 清空知识库
            """)
    public String handleKnowledge(
            @ToolParam(description = "用户的完整任务描述，包含所有细节") String task
    ) {
        String userId = TodoTools.getCurrentUserId();
        log.info("路由到 KnowledgeAgent: userId={}, task={}", userId, task);

        try {
            return knowledgeAgent.execute(userId, task)
                    .get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("KnowledgeAgent 执行超时: userId={}", userId);
            return "知识库处理超时，请稍后重试。";
        } catch (Exception e) {
            log.error("KnowledgeAgent 执行异常: userId={}", userId, e);
            return "知识库处理失败: " + e.getMessage();
        }
    }

    /**
     * 路由到聊天分析子 Agent
     * 处理：获取群聊/私聊记录、总结聊天内容
     */
    @Tool(description = """
            获取或总结群聊/私聊的聊天记录，包括：
            - 总结群聊消息
            - 获取聊天记录
            - 归纳聊天中的待办事项或重要内容
            """)
    public String handleChatAnalysis(
            @ToolParam(description = "用户的完整任务描述，包含所有细节") String task
    ) {
        String userId = TodoTools.getCurrentUserId();
        ChatTools.ChatContext ctx = ChatTools.getChatContext();

        String relationId = ctx != null ? ctx.getRelationId() : null;
        Long startTime   = ctx != null ? ctx.getStartTime()   : null;
        Long endTime     = ctx != null ? ctx.getEndTime()     : null;

        log.info("路由到 ChatAnalysisAgent: userId={}, relationId={}, task={}", userId, relationId, task);

        try {
            return chatAnalysisAgent.execute(userId, task, relationId, startTime, endTime)
                    .get(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("ChatAnalysisAgent 执行超时: userId={}", userId);
            return "聊天分析超时，请稍后重试。";
        } catch (Exception e) {
            log.error("ChatAnalysisAgent 执行异常: userId={}", userId, e);
            return "聊天分析失败: " + e.getMessage();
        }
    }
}
