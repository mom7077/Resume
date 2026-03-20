package com.aiwork.helper.ai.agent;

import com.aiwork.helper.ai.tools.ChatTools;
import com.aiwork.helper.ai.tools.TodoTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 聊天分析子 Agent
 *
 * 职责：获取群聊/私聊记录并进行总结分析
 * 特点：无状态（不持有记忆），由 OrchestratorAgent 调用
 *
 * 工具集（2个）：
 *   - getChatLogs
 *   - summarizeChatLogs
 */
@Slf4j
@Service
public class ChatAnalysisAgent {

    private final ChatClient chatAnalysisChatClient;

    public ChatAnalysisAgent(@Qualifier("chatAnalysisChatClient") ChatClient chatAnalysisChatClient) {
        this.chatAnalysisChatClient = chatAnalysisChatClient;
    }

    /**
     * 异步执行聊天分析任务
     *
     * @param userId     当前用户 ID
     * @param task       由 OrchestratorAgent 打包好的任务描述
     * @param relationId 关联会话 ID（群聊 ID 等）
     * @param startTime  查询时间范围起点（Unix 时间戳，秒）
     * @param endTime    查询时间范围终点（Unix 时间戳，秒）
     */
    @Async("agentTaskExecutor")
    public CompletableFuture<String> execute(
            String userId, String task, String relationId, Long startTime, Long endTime) {

        log.info("ChatAnalysisAgent 开始执行: userId={}, relationId={}, task={}", userId, relationId, task);

        try {
            // 在本线程重新设置 ThreadLocal（@Async 跨线程，父线程的 ThreadLocal 不继承）
            TodoTools.setCurrentUserId(userId);
            ChatTools.setChatContext(relationId, startTime, endTime);

            String response = chatAnalysisChatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(task)
                    .call()
                    .content();

            log.info("ChatAnalysisAgent 执行完成: userId={}, responseLength={}",
                    userId, response != null ? response.length() : 0);

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("ChatAnalysisAgent 执行失败: userId={}, task={}", userId, task, e);
            return CompletableFuture.completedFuture("聊天分析任务处理失败: " + e.getMessage());
        } finally {
            TodoTools.clearCurrentUserId();
            ChatTools.clearChatContext();
        }
    }

    private String buildSystemPrompt() {
        return """
                你是一个聊天记录分析专员，专门负责获取和总结群聊/私聊消息，不处理其他类型的请求。

                ## 工具使用规则
                - 用户要求"总结群聊"、"归纳聊天内容"、"看看群里说了什么" → 调用 summarizeChatLogs
                - 用户要求"获取聊天记录"、"查看消息" → 调用 getChatLogs

                ## 总结要求
                总结时需要提取以下关键信息（如有）：
                1. 重要决策或结论
                2. 待办事项或行动计划
                3. 需要审批或跟进的事项
                4. 关键时间节点

                ## 回复要求
                - 总结内容结构清晰，用分点列出
                - 没有聊天记录时明确告知
                - 使用中文，简洁专业
                """;
    }
}
