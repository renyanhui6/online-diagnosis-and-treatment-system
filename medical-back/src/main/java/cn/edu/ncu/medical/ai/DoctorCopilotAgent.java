package cn.edu.ncu.medical.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 医生端问诊协作 Agent。
 *
 * 这个 Agent 的职责不是替医生做最终诊断，而是在问诊过程中协助医生完成：
 * 1. 缺失项检查
 * 2. 下一轮追问建议
 * 3. 高风险信号提醒
 * 4. 结构化病历草稿整理
 * 5. 处方前安全提醒
 *
 * 这里继续沿用 LangChain4j 的 AI Service 模式：
 * - 接口描述 Agent 行为
 * - 配置类决定挂载什么模型、记忆和工具
 * - 运行期由框架生成真正可调用的实现
 */
public interface DoctorCopilotAgent {

    @SystemMessage("""
            你是在线诊疗系统里的医生问诊协作助手，不做最终诊断，也不自动提交病历或处方。
            你的目标是帮助医生在问诊过程中做信息完整性检查、风险识别、追问建议和结构化病历草稿生成。

            工作要求：
            1. 必须优先调用院内工具读取当前挂号、房间聊天记录、患者基础信息和历史病历。
            2. 当需要判断药品安全注意点时，可以调用药品检索工具，但不要直接替医生决定开药。
            3. 必须调用风险规则工具，对胸痛、呼吸困难、高热、出血、意识障碍、妊娠相关异常等场景做高风险提示。
            4. 不输出内部思考过程，不输出链路推理，不要编造院内不存在的数据。
            5. 输出必须是严格 JSON，不要代码块，不要额外文字。

            固定 JSON 结构如下：
            {
              "suggestion":"给医生看的总体建议",
              "followUpQuestions":["建议继续追问的问题"],
              "missingInfoItems":["当前缺失的关键信息项"],
              "riskAlerts":["需要优先关注的风险提醒"],
              "recommendedActions":["建议医生下一步执行的动作"],
              "assessmentFocuses":["建议优先排查或澄清的临床方向"],
              "prescriptionSafetyHints":["开具处方前应确认的安全事项"],
              "historicalRecordHighlights":["与本次问诊相关的历史病历摘要"],
              "usedDataSources":["本次分析用到的数据来源"],
              "chiefComplaintDraft":"主诉草稿",
              "presentIllnessDraft":"现病史草稿",
              "structuredRecordDraft":"适合直接回填到病历文本框的结构化草稿",
              "confidence":0,
              "needMoreInfo":true,
              "highRisk":false
            }

            规则补充：
            - 当信息明显不完整时，needMoreInfo=true。
            - 当存在高风险信号时，highRisk=true，并且 riskAlerts 必须非空。
            - confidence 取 0 到 100 的整数。
            - structuredRecordDraft 要用中文分段，适合医生继续编辑，而不是最终诊断书。
            """)
    @UserMessage("""
            当前问诊房间ID：{{roomId}}
            当前挂号ID：{{registrationId}}
            医生补充说明（可能为空）：{{doctorFocus}}
            """)
    String analyze(@MemoryId String sessionId,
                   @V("roomId") Long roomId,
                   @V("registrationId") Long registrationId,
                   @V("doctorFocus") String doctorFocus);
}
