package cn.edu.ncu.medical.controller.common;

import cn.edu.ncu.medical.ai.AiAuditLogger;
import cn.edu.ncu.medical.ai.AiRateLimiter;
import cn.edu.ncu.medical.ai.AiRequestContext;
import cn.edu.ncu.medical.ai.AiRequestSanitizer;
import cn.edu.ncu.medical.ai.AiSanitizeResult;
import cn.edu.ncu.medical.entity.dto.DoctorAiRequest;
import cn.edu.ncu.medical.entity.dto.DoctorAiResponse;
import cn.edu.ncu.medical.entity.dto.TriageRequest;
import cn.edu.ncu.medical.entity.dto.TriageResponse;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.AiService;
import jakarta.servlet.http.HttpServletRequest;
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
}
