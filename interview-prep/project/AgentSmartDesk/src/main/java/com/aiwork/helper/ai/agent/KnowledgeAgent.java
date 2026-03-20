package com.aiwork.helper.ai.agent;

import com.aiwork.helper.ai.tools.TodoTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 知识库子 Agent
 *
 * 职责：处理知识库的查询、更新与清空操作，以及用户上传文件的管理
 * 特点：无状态（不持有记忆），由 OrchestratorAgent 调用
 *
 * 工具集（5个）：
 *   - queryKnowledge / updateKnowledge / clearKnowledge
 *   - getRecentUploadedFiles / getLatestUploadedFilePath
 */
@Slf4j
@Service
public class KnowledgeAgent {

    private final ChatClient knowledgeChatClient;

    public KnowledgeAgent(@Qualifier("knowledgeChatClient") ChatClient knowledgeChatClient) {
        this.knowledgeChatClient = knowledgeChatClient;
    }

    /**
     * 异步执行知识库任务
     *
     * @param userId 当前用户 ID（用于获取用户上传的文件）
     * @param task   由 OrchestratorAgent 打包好的任务描述
     */
    @Async("agentTaskExecutor")
    public CompletableFuture<String> execute(String userId, String task) {
        log.info("KnowledgeAgent 开始执行: userId={}, task={}", userId, task);

        try {
            // 设置当前用户（FileTools 内部通过 TodoTools.getCurrentUserId() 读取）
            TodoTools.setCurrentUserId(userId);

            String response = knowledgeChatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(task)
                    .call()
                    .content();

            log.info("KnowledgeAgent 执行完成: userId={}, responseLength={}",
                    userId, response != null ? response.length() : 0);

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("KnowledgeAgent 执行失败: userId={}, task={}", userId, task, e);
            return CompletableFuture.completedFuture("知识库任务处理失败: " + e.getMessage());
        } finally {
            TodoTools.clearCurrentUserId();
        }
    }

    private String buildSystemPrompt() {
        return """
                你是一个知识库专员，专门负责知识库的查询和维护，不处理其他类型的请求。

                ## 工具使用规则

                ### 查询场景
                - 用户询问公司制度、规章、员工手册、政策相关问题 → 调用 queryKnowledge
                - 知识库为空时，告知用户需要先上传 PDF 文件

                ### 更新场景
                - 用户说"根据我上传的文件更新知识库" → 先调用 getLatestUploadedFilePath 获取路径，再调用 updateKnowledge
                - 用户直接提供文件路径 → 直接调用 updateKnowledge 并传入路径

                ### 清空场景
                - 用户要清空或重置知识库 → 调用 clearKnowledge

                ## 回复要求
                - 查询结果需要根据检索内容给出准确回答，不要原样返回文档内容
                - 更新/清空操作后告知操作结果
                - 使用中文，简洁专业
                """;
    }
}
