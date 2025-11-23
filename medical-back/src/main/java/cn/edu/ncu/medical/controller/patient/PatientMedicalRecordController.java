package cn.edu.ncu.medical.controller.patient;

import cn.edu.ncu.medical.entity.dto.MedicalRecordCondition;
import cn.edu.ncu.medical.entity.vo.MedicalRecordInfo;
import cn.edu.ncu.medical.entity.vo.PrescriptionInfo;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.MedicalRecordService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/front/patient/medicalRecord")
public class PatientMedicalRecordController {
    @Autowired
    private MedicalRecordService medicalRecordService;


    /**
     * 根据用户id查询病历
     *
     * @param pageNum
     * @param pageSize
     * @param medicalRecordCondition
     * @return
     */
    @PostMapping("/getMedicalRecordByUserId")
    public Result getMedicalRecordByUserId(
                                           @RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestBody(required = false) MedicalRecordCondition medicalRecordCondition) {
        Page<MedicalRecordInfo> page = new Page<>(pageNum, pageSize);
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        IPage<MedicalRecordInfo> medicalRecordInfoIPage = medicalRecordService.getMedicalRecordByUserId(userId, page, medicalRecordCondition);
        return Result.ok(medicalRecordInfoIPage);
    }


    @GetMapping("/getPrescriptionInfoByMedicalRecordId")
    public Result getPrescriptionInfoByMedicalRecordId(@RequestParam("medicalRecordId") Long medicalRecordId) {
        List<PrescriptionInfo> prescriptionInfoList = medicalRecordService.getPrescriptionInfoByMedicalRecordId(medicalRecordId);
        return Result.ok(prescriptionInfoList);
    }

}
