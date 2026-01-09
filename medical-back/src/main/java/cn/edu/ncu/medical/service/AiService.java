package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.dto.DoctorAiRequest;
import cn.edu.ncu.medical.entity.dto.DoctorAiResponse;
import cn.edu.ncu.medical.entity.dto.TriageRequest;
import cn.edu.ncu.medical.entity.dto.TriageResponse;

public interface AiService {
    DoctorAiResponse assistDoctor(DoctorAiRequest request);

    TriageResponse triage(TriageRequest request);
}
