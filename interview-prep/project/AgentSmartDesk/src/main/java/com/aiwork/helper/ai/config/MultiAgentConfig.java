package com.aiwork.helper.ai.config;

import com.aiwork.helper.ai.agent.RoutingTools;
import com.aiwork.helper.ai.tools.*;
import com.aiwork.helper.ai.tools.ChatTools;
import com.aiwork.helper.ai.tools.FileTools;
import com.aiwork.helper.ai.tools.KnowledgeTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 多 Agent 架构配置
 * - 注册各子 Agent 专属的 ChatClient（工具集隔离、无记忆）
 * - 配置子 Agent 异步执行线程池
 */
@Slf4j
@EnableAsync
@Configuration
public class MultiAgentConfig {

    /**
     * 子 Agent 异步执行线程池
     * OrchestratorAgent 并行调用多个子 Agent 时使用
     */
    @Bean("agentTaskExecutor")
    public Executor agentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("agent-worker-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("Agent 线程池初始化完成: coreSize=4, maxSize=10");
        return executor;
    }

    /**
     * WorkflowAgent 专属 ChatClient
     * 只注册工作流相关工具（8个），不挂记忆 Advisor（无状态）
     */
    @Bean("workflowChatClient")
    public ChatClient workflowChatClient(
            ChatModel chatModel,
            TodoTools todoTools,
            ApprovalTools approvalTools,
            TimeParserTool timeParserTool,
            UserQueryTool userQueryTool
    ) {
        log.info("初始化 WorkflowAgent ChatClient，注册工具: TodoTools, ApprovalTools, TimeParserTool, UserQueryTool");
        return ChatClient.builder(chatModel)
                .defaultTools(todoTools, approvalTools, timeParserTool, userQueryTool)
                .build();
    }

    /**
     * KnowledgeAgent 专属 ChatClient
     * 只注册知识库相关工具（5个），不挂记忆 Advisor（无状态）
     */
    @Bean("knowledgeChatClient")
    public ChatClient knowledgeChatClient(
            ChatModel chatModel,
            KnowledgeTools knowledgeTools,
            FileTools fileTools
    ) {
        log.info("初始化 KnowledgeAgent ChatClient，注册工具: KnowledgeTools, FileTools");
        return ChatClient.builder(chatModel)
                .defaultTools(knowledgeTools, fileTools)
                .build();
    }

    /**
     * ChatAnalysisAgent 专属 ChatClient
     * 只注册聊天分析工具（2个），不挂记忆 Advisor（无状态）
     */
    @Bean("chatAnalysisChatClient")
    public ChatClient chatAnalysisChatClient(
            ChatModel chatModel,
            ChatTools chatTools
    ) {
        log.info("初始化 ChatAnalysisAgent ChatClient，注册工具: ChatTools");
        return ChatClient.builder(chatModel)
                .defaultTools(chatTools)
                .build();
    }

    /**
     * OrchestratorAgent 专属 ChatClient
     * 只注册路由工具（RoutingTools），挂载记忆 Advisor（有状态）
     * 注意：会话 ID 使用 "orch:" 前缀，与旧 AgentService 的记忆隔离
     */
    @Bean("orchestratorChatClient")
    public ChatClient orchestratorChatClient(
            ChatModel chatModel,
            RoutingTools routingTools,
            MessageChatMemoryAdvisor messageChatMemoryAdvisor
    ) {
        log.info("初始化 OrchestratorAgent ChatClient，注册工具: RoutingTools，挂载 Memory Advisor");
        return ChatClient.builder(chatModel)
                .defaultTools(routingTools)
                .defaultAdvisors(messageChatMemoryAdvisor)
                .build();
    }
}
