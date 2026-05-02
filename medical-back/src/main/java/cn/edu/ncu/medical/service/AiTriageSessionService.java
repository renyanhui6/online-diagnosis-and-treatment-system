package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.dto.TriageChatRequest;
import cn.edu.ncu.medical.entity.dto.TriageChatResponse;
import cn.edu.ncu.medical.entity.vo.AiTriageMessageVo;
import cn.edu.ncu.medical.entity.vo.AiTriageSessionVo;
import java.util.List;

public interface AiTriageSessionService {
    AiTriageSessionVo startSession(Long userId, Long patientAttendantId);

    void closeSession(Long userId, String sessionId);

    TriageChatResponse sendMessage(Long userId, TriageChatRequest request);

    List<AiTriageSessionVo> listSessions(Long userId);

    List<AiTriageMessageVo> listMessages(Long userId, String sessionId);
}
