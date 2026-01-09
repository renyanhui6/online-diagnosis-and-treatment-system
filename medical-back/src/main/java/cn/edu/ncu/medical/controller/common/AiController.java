package cn.edu.ncu.medical.controller.common;

import cn.edu.ncu.medical.entity.dto.DoctorAiRequest;
import cn.edu.ncu.medical.entity.dto.DoctorAiResponse;
import cn.edu.ncu.medical.entity.dto.TriageRequest;
import cn.edu.ncu.medical.entity.dto.TriageResponse;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * 医生端 AI 辅助建议
     */
    @PostMapping("/doctor/assist")
    public Result<DoctorAiResponse> doctorAssist(@RequestBody DoctorAiRequest request) {
        return Result.ok(aiService.assistDoctor(request));
    }

    /**
     * 患者挂号前科室推荐
     */
    @PostMapping("/patient/triage")
    public Result<TriageResponse> patientTriage(@RequestBody TriageRequest request) {
        return Result.ok(aiService.triage(request));
    }
}
