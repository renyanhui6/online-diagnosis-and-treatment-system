package cn.edu.ncu.medical.controller.common;

import cn.edu.ncu.medical.ai.AiAuditLogger;
import cn.edu.ncu.medical.ai.AiRateLimiter;
import cn.edu.ncu.medical.ai.AiRequestContext;
import cn.edu.ncu.medical.ai.AiRequestSanitizer;
import cn.edu.ncu.medical.ai.AiSanitizeResult;
import cn.edu.ncu.medical.entity.dto.DoctorAiRequest;
import cn.edu.ncu.medical.entity.dto.DoctorAiResponse;
import cn.edu.ncu.medical.entity.dto.AiTriageSessionStartRequest;
import cn.edu.ncu.medical.entity.dto.TriageChatRequest;
import cn.edu.ncu.medical.entity.dto.TriageChatResponse;
import cn.edu.ncu.medical.entity.dto.TriageRequest;
import cn.edu.ncu.medical.entity.dto.TriageResponse;
import cn.edu.ncu.medical.entity.vo.AiTriageMessageVo;
import cn.edu.ncu.medical.entity.vo.AiTriageSessionVo;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.inteceptor.login.LoginUser;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.AiService;
import cn.edu.ncu.medical.service.AiTriageSessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;
    @Autowired
    private AiRequestSanitizer aiRequestSanitizer;
    @Autowired
    private AiRateLimiter aiRateLimiter;
    @Autowired
    private AiAuditLogger aiAuditLogger;
    @Autowired
    private AiTriageSessionService aiTriageSessionService;

    /**
     * 医生端 AI 辅助建议
     */
    @PostMapping("/doctor/assist")
    public Result<DoctorAiResponse> doctorAssist(@RequestBody DoctorAiRequest request, HttpServletRequest httpServletRequest) {
        if (request == null) {
            throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
        }
        AiRequestContext context = AiRequestContext.from(httpServletRequest, "doctor_assist");
        AiSanitizeResult<DoctorAiRequest> sanitized = aiRequestSanitizer.sanitizeDoctorRequest(request);
        aiRateLimiter.check(context);
        aiAuditLogger.logDoctorAssist(context, sanitized.getRequest(), sanitized.getMeta());
        return Result.ok(aiService.assistDoctor(sanitized.getRequest()));
    }

    /**
     * 患者挂号前科室推荐
     */
    @PostMapping("/patient/triage")
    public Result<TriageResponse> patientTriage(@RequestBody TriageRequest request, HttpServletRequest httpServletRequest) {
        if (request == null) {
            throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
        }
        AiRequestContext context = AiRequestContext.from(httpServletRequest, "patient_triage");
        AiSanitizeResult<TriageRequest> sanitized = aiRequestSanitizer.sanitizeTriageRequest(request);
        aiRateLimiter.check(context);
        aiAuditLogger.logTriage(context, sanitized.getRequest(), sanitized.getMeta());
        return Result.ok(aiService.triage(sanitized.getRequest()));
    }

    /**
     * 患者端 AI 多轮分诊客服
     */
    @PostMapping("/patient/triage/chat")
    public Result<TriageChatResponse> patientTriageChat(@RequestBody TriageChatRequest request, HttpServletRequest httpServletRequest) {
        if (request == null) {
            throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
        }
        AiRequestContext context = AiRequestContext.from(httpServletRequest, "patient_triage_chat");
        AiSanitizeResult<TriageChatRequest> sanitized = aiRequestSanitizer.sanitizeTriageChatRequest(request);
        aiRateLimiter.check(context);
        aiAuditLogger.logTriageChat(context, sanitized.getRequest(), sanitized.getMeta());
        return Result.ok(aiTriageSessionService.sendMessage(currentUserId(), sanitized.getRequest()));
    }

    @PostMapping("/patient/triage/session/start")
    public Result<AiTriageSessionVo> startTriageSession(
            @RequestBody(required = false) AiTriageSessionStartRequest request) {
        Long patientAttendantId = request == null ? null : request.getPatientAttendantId();
        return Result.ok(aiTriageSessionService.startSession(currentUserId(), patientAttendantId));
    }

    @PostMapping("/patient/triage/session/close")
    public Result<Void> closeTriageSession(@RequestParam("sessionId") String sessionId) {
        aiTriageSessionService.closeSession(currentUserId(), sessionId);
        return Result.ok();
    }

    @GetMapping("/patient/triage/session/list")
    public Result<List<AiTriageSessionVo>> listTriageSessions() {
        return Result.ok(aiTriageSessionService.listSessions(currentUserId()));
    }

    @GetMapping("/patient/triage/session/messages")
    public Result<List<AiTriageMessageVo>> listTriageMessages(@RequestParam("sessionId") String sessionId) {
        return Result.ok(aiTriageSessionService.listMessages(currentUserId(), sessionId));
    }

    private Long currentUserId() {
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new MyRuntimeException(ResultCodeEnum.USER_NOT_LOGIN);
        }
        return loginUser.getUserId();
    }
}
