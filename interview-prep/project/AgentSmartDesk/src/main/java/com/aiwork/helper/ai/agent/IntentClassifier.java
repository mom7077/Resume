package com.aiwork.helper.ai.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

/**
 * 意图分类器
 * 在Agent主循环之前，先用一次轻量LLM调用判断用户意图是否明确。
 * 如果意图模糊（可能同时匹配多个工具），返回反问文本让用户澄清。
 */
@Slf4j
@Component
public class IntentClassifier {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    /**
     * 意图分类的提示词模板
     * 指导LLM将用户输入分类为具体意图或AMBIGUOUS
     */
    private static final String CLASSIFY_PROMPT_TEMPLATE = """
            你是一个意图分类器。请分析用户输入，判断意图属于哪个类别。

            ## 类别定义
            - TODO: 待办事项相关（创建/查询/完成/删除待办）
            - APPROVAL: 审批相关（请假/补卡/外出/查审批记录）
            - KNOWLEDGE: 知识库问答（公司制度/规章/员工手册/政策）
            - CHAT: 聊天记录相关（查看/总结群聊消息）
            - FILE: 文件相关（查看上传文件/文件列表）
            - GENERAL: 一般对话（问候/闲聊/与工具无关的问题）
            - AMBIGUOUS: 意图不明确，可能属于多个类别

            ## 判断为 AMBIGUOUS 的情况
            - 用户说"查一下记录"（可能是待办记录、审批记录、聊天记录）
            - 用户说"帮我处理一下"（不知道处理什么）
            - 用户说"看看最近的东西"（太模糊）
            - 用户说"帮我查一下"（没说查什么）

            ## 不应判断为 AMBIGUOUS 的情况
            - 用户说"帮我请个假" → APPROVAL（明确是审批）
            - 用户说"创建一个待办" → TODO（明确是待办）
            - 用户说"你好" → GENERAL（明确是问候）
            - 用户说"年假有几天" → KNOWLEDGE（明确是查制度）
            - 用户说"查一下我的待办" → TODO（虽然说了"查"，但明确是待办）
            - 用户说"帮我查审批记录" → APPROVAL（明确是审批）

            ## 输出格式
            请严格用以下JSON格式回复，不要输出其他内容：
            {"intent": "类别名", "ambiguous": true或false, "clarifyQuestion": "反问文本或空字符串"}

            ambiguous为true时，clarifyQuestion必须是一个友好的中文反问句，帮助用户澄清意图。
            ambiguous为false时，clarifyQuestion设为空字符串""。

            ## 用户输入
            %s
            """;

    public IntentClassifier(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 分类用户意图
     *
     * @param userInput 用户输入文本
     * @return 分类结果，包含意图类别和是否模糊
     */
    public IntentResult classify(String userInput) {
        log.info("意图分类开始: input={}", userInput);

        try {
            String prompt = String.format(CLASSIFY_PROMPT_TEMPLATE, userInput);
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String resultText = response.getResult().getOutput().getText();

            log.debug("意图分类LLM原始返回: {}", resultText);

            // 解析JSON结果
            IntentResult result = parseResult(resultText);
            log.info("意图分类完成: input={}, intent={}, ambiguous={}", 
                    userInput, result.getIntent(), result.isAmbiguous());
            return result;

        } catch (Exception e) {
            log.warn("意图分类失败，默认放行: input={}, error={}", userInput, e.getMessage());
            // 分类失败时默认不阻断，让Agent正常处理
            return IntentResult.clear("GENERAL");
        }
    }

    /**
     * 解析LLM返回的JSON结果
     */
    private IntentResult parseResult(String resultText) {
        try {
            // 提取JSON部分（LLM可能在JSON前后加了多余文字）
            String json = extractJson(resultText);
            return objectMapper.readValue(json, IntentResult.class);
        } catch (Exception e) {
            log.warn("JSON解析失败，尝试简单判断: text={}", resultText);
            // JSON解析失败时，检查是否包含AMBIGUOUS关键词
            if (resultText != null && resultText.toUpperCase().contains("AMBIGUOUS")) {
                return IntentResult.ambiguous("您的需求我不太确定，能再具体描述一下吗？");
            }
            return IntentResult.clear("GENERAL");
        }
    }

    /**
     * 从LLM返回文本中提取JSON字符串
     */
    private String extractJson(String text) {
        if (text == null) {
            return "{}";
        }
        // 找到第一个 { 和最后一个 }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text.trim();
    }

    /**
     * 意图分类结果
     */
    @Data
    public static class IntentResult {
        /** 意图类别 */
        @JsonProperty("intent")
        private String intent;

        /** 是否意图模糊 */
        @JsonProperty("ambiguous")
        private boolean ambiguous;

        /** 反问文本（仅 ambiguous=true 时有值） */
        @JsonProperty("clarifyQuestion")
        private String clarifyQuestion;

        /**
         * 创建一个明确意图的结果
         */
        public static IntentResult clear(String intent) {
            IntentResult result = new IntentResult();
            result.setIntent(intent);
            result.setAmbiguous(false);
            result.setClarifyQuestion("");
            return result;
        }

        /**
         * 创建一个模糊意图的结果
         */
        public static IntentResult ambiguous(String clarifyQuestion) {
            IntentResult result = new IntentResult();
            result.setIntent("AMBIGUOUS");
            result.setAmbiguous(true);
            result.setClarifyQuestion(clarifyQuestion);
            return result;
        }
    }
}
