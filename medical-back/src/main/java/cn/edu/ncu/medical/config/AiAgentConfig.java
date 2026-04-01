package cn.edu.ncu.medical.config;

import cn.edu.ncu.medical.ai.DeepSeekProperties;
import cn.edu.ncu.medical.ai.DoctorCopilotAgent;
import cn.edu.ncu.medical.ai.DoctorCopilotTools;
import cn.edu.ncu.medical.ai.PatientTriageAgent;
import cn.edu.ncu.medical.ai.TriageDepartmentTools;
import cn.edu.ncu.medical.ai.TriageExternalSearchTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class AiAgentConfig {

    /**
     * 创建分诊 Agent 使用的底层聊天模型。
     *
     * 这里虽然用的是 OpenAI 兼容模型接口，但实际可接 DeepSeek 这类兼容 OpenAI 协议的模型服务。
     * 之所以单独抽成 Bean，而不是在业务代码里临时 new，一是便于 Spring 统一管理，
     * 二是后续如果要切模型、调温度、补日志或切多模型路由，只需要改这一处配置。
     *
     * 注意：
     * 1. 这里即使没有配置真实 API Key，也会先把 Bean 创建出来，避免应用启动阶段直接失败。
     * 2. 真正决定“走在线 Agent 还是本地回退”的逻辑不在这里，而在 AiServiceImpl。
     */
    @Bean
    public ChatModel triageChatModel(DeepSeekProperties deepSeekProperties) {
        return OpenAiChatModel.builder()
                // LangChain4j 的 OpenAI 兼容模型要求 baseUrl 以 /v1 结尾，这里统一做一次标准化。
                .baseUrl(normalizeBaseUrl(deepSeekProperties.getBaseUrl()))
                // 未配置 key 时先给一个占位值，保证 Spring 能完成依赖注入；
                // 真正发请求前会在业务层判断是否启用在线 Agent。
                .apiKey(StringUtils.hasText(deepSeekProperties.getApiKey()) ? deepSeekProperties.getApiKey() : "missing-api-key")
                .modelName(StringUtils.hasText(deepSeekProperties.getModel()) ? deepSeekProperties.getModel() : "deepseek-chat")
                // 分诊类任务更需要稳定和收敛，temperature 不宜过高。
                .temperature(0.2)
                // 开启请求/响应日志，便于联调时查看 prompt、tool call 和模型返回内容。
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 创建真正对外使用的“患者分诊 Agent”。
     *
     * 这里的 Agent 不是一个普通聊天对象，而是 LangChain4j 的 AI Service：
     * 1. 它会读取 PatientTriageAgent 接口上的系统提示词和方法签名；
     * 2. 它会自动维护多轮对话记忆；
     * 3. 它可以在推理过程中调用工具，例如院内科室查询和院外知识检索。
     *
     * 当前挂上的两个工具分别是：
     * - TriageDepartmentTools：优先查询院内真实科室、诊疗范围、医生数和未来排班
     * - TriageExternalSearchTool：院内信息不足时，补充查询院外公开医疗知识
     */
    @Bean
    public PatientTriageAgent patientTriageAgent(ChatModel triageChatModel,
                                                 TriageDepartmentTools triageDepartmentTools,
                                                 TriageExternalSearchTool triageExternalSearchTool) {
        return AiServices.builder(PatientTriageAgent.class)
                .chatModel(triageChatModel)
                // 以 sessionId 作为 memoryId，为每个用户会话保留最近 12 条消息上下文。
                // 这样前端只需要持续传 sessionId，Agent 就能做多轮追问和信息收敛。
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(12))
                .tools(triageDepartmentTools, triageExternalSearchTool)
                .build();
    }

    /**
     * 创建医生端问诊协作 Agent。
     *
     * 这个 Agent 和患者分诊 Agent 的职责明显不同：
     * - 患者分诊 Agent 解决“挂哪个科”
     * - 医生协作 Agent 解决“当前问诊信息是否完整、有哪些风险、病历如何结构化沉淀”
     *
     * 这里给医生 Agent 挂了更多面向业务流的工具：
     * - 当前房间/挂号/聊天上下文
     * - 患者历史病历
     * - 临床风险规则
     * - 药品库检索
     */
    @Bean
    public DoctorCopilotAgent doctorCopilotAgent(ChatModel triageChatModel,
                                                 DoctorCopilotTools doctorCopilotTools) {
        return AiServices.builder(DoctorCopilotAgent.class)
                .chatModel(triageChatModel)
                // 医生协作通常围绕同一个 roomId 多次调用，保留最近 10 条 Agent 对话足够。
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .tools(doctorCopilotTools)
                .build();
    }

    /**
     * 统一标准化模型服务地址。
     *
     * 例如：
     * - 配置为 https://api.deepseek.com
     * - 最终会转成 https://api.deepseek.com/v1
     *
     * 这样可以避免不同环境下因为 URL 末尾是否带 /、是否显式写 /v1 造成调用失败。
     */
    private String normalizeBaseUrl(String baseUrl) {
        String resolved = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://api.deepseek.com";
        if (resolved.endsWith("/")) {
            resolved = resolved.substring(0, resolved.length() - 1);
        }
        if (!resolved.endsWith("/v1")) {
            resolved = resolved + "/v1";
        }
        return resolved;
    }
}
