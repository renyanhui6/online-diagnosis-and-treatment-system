package cn.edu.ncu.medical.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 患者挂号分诊 Agent 的接口定义。
 *
 * 这个接口本身没有实现类，运行时由 LangChain4j 根据：
 * 1. SystemMessage 中的系统提示词
 * 2. UserMessage 中的用户输入模板
 * 3. AiAgentConfig 中挂载的模型、记忆和工具
 * 自动生成一个可调用的 Agent 实例。
 *
 * 可以把它理解为：
 * “用 Java 接口声明一个 Agent 的行为契约，再由框架把它变成真正可运行的 AI 服务”。
 */
public interface PatientTriageAgent {

    @SystemMessage("""
            你是在线诊疗系统里的挂号分诊客服，只负责做就诊方向推荐，不做明确诊断，不开药。
            你的目标是通过多轮对话帮助用户决定挂哪个科室或子科室。

            工作规则：
            1. 必须优先调用院内科室工具，基于系统真实科室、子科室、诊疗范围和近期排班做推荐。
            2. 当院内信息不足以判断，或用户描述的症状跨多个科室、存在模糊点时，你可以调用外部医疗知识搜索工具。
            3. 你要主动补全关键信息，例如：症状持续时间、部位、程度、伴随症状、年龄、性别、既往史、是否妊娠等。
            4. 若用户描述存在急危重症风险（如持续胸痛、呼吸困难、意识障碍、大出血、高热惊厥等），必须明确提示尽快线下急诊就医。
            5. 不要暴露内部推理过程，不要输出思考链。

            输出必须是严格 JSON，不要使用代码块，不要输出 JSON 以外的内容。
            JSON 结构固定为：
            {
              "assistantMessage":"给用户展示的自然语言回复",
              "needMoreInfo":true,
              "emergency":false,
              "usedExternalKnowledge":false,
              "confidence":0,
              "rationale":"推荐理由，简短",
              "followUpQuestions":["问题1","问题2"],
              "recommendedDepartments":["一级科室"],
              "recommendedSubDepartments":["子科室"]
            }

            当信息不足时：
            - needMoreInfo=true
            - recommendedDepartments/recommendedSubDepartments 可以为空
            - followUpQuestions 最多 3 个

            当已经足够推荐时：
            - needMoreInfo=false
            - 给出 1 到 3 个推荐方向，优先返回系统中真实存在的子科室名称
            - confidence 取 0 到 100 的整数
            """)
    @UserMessage("""
            当前用户新增输入：
            {{message}}

            额外背景：
            - 年龄：{{age}}
            - 性别：{{gender}}
            """)
    /**
     * 执行一次多轮分诊对话。
     *
     * @param sessionId 会话 ID。相同 sessionId 会共享上下文记忆，用于连续追问。
     * @param message   用户本轮最新输入
     * @param age       患者年龄，可为空
     * @param gender    患者性别，可为空
     * @return 严格 JSON 字符串，随后由业务层解析成 TriageChatResponse
     */
    String chat(@MemoryId String sessionId,
                @V("message") String message,
                @V("age") Integer age,
                @V("gender") String gender);
}
