package com.aiwork.helper.ai.agent;

import com.aiwork.helper.ai.tools.TodoTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 工作流子 Agent
 *
 * 职责：处理所有审批（请假/补卡/外出）和待办任务的创建与查询
 * 特点：无状态（不持有记忆），由 OrchestratorAgent 调用
 *
 * 工具集（8个）：
 *   - createTodo / findTodos
 *   - createLeaveApproval / createPunchApproval / createGoOutApproval / findApprovals
 *   - parseTime / getCurrentTime
 *   - getUserIdByName
 */
@Slf4j
@Service
public class WorkflowAgent {

    private final ChatClient workflowChatClient;

    public WorkflowAgent(@Qualifier("workflowChatClient") ChatClient workflowChatClient) {
        this.workflowChatClient = workflowChatClient;
    }

    /**
     * 异步执行工作流任务
     *
     * @param userId 当前用户 ID（用于待办执行人、审批提交人）
     * @param task   由 OrchestratorAgent 打包好的任务描述
     * @return 执行结果（异步）
     */
    @Async("agentTaskExecutor")
    public CompletableFuture<String> execute(String userId, String task) {
        log.info("WorkflowAgent 开始执行: userId={}, task={}", userId, task);

        try {
            TodoTools.setCurrentUserId(userId);

            String response = workflowChatClient.prompt()
                    .system(buildSystemPrompt())
                    .user(task)
                    .call()
                    .content();

            log.info("WorkflowAgent 执行完成: userId={}, responseLength={}",
                    userId, response != null ? response.length() : 0);

            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("WorkflowAgent 执行失败: userId={}, task={}", userId, task, e);
            return CompletableFuture.completedFuture("工作流任务处理失败: " + e.getMessage());
        } finally {
            TodoTools.clearCurrentUserId();
        }
    }

    private String buildSystemPrompt() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        long tomorrowMidnight = today.plusDays(1).atStartOfDay()
                .atZone(ZoneId.systemDefault()).toEpochSecond();

        return String.format("""
                你是一个工作流专员，专门处理审批申请和待办任务，不处理其他类型的请求。

                ## 当前时间
                - 当前时间: %s
                - 明天 00:00 时间戳: %d
                - 时间计算: 明天某小时 = 明天00:00时间戳 + 小时数 × 3600

                ## 使用工具的规则
                1. 涉及时间的操作，必须先调用 parseTime 获取准确时间戳，不得自己估算
                2. 涉及指定执行人/审批人姓名，必须先调用 getUserIdByName 获取 ID
                3. 如果用户没有指定待办执行人，默认为当前用户，无需查询 ID

                ## 审批类型说明
                - 请假(leaveType): 1=事假 2=调休 3=病假 4=年假 5=产假 6=陪产假 7=婚假 8=丧假 9=哺乳假
                - 打卡(checkType): 1=上班卡 2=下班卡
                - 提到"感冒""生病"等，请假类型默认为病假(3)

                ## 回复要求
                - 使用中文，简洁友好
                - 操作成功后告知用户结果（ID、时间等关键信息）
                - 操作失败时给出明确原因
                """,
                now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                tomorrowMidnight);
    }
}
