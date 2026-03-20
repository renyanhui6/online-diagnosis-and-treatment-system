package cn.edu.ncu.medical.controller.doctor;

import cn.edu.ncu.medical.entity.MedicalRecord;
import cn.edu.ncu.medical.entity.dto.MedicalRecordCondition;
import cn.edu.ncu.medical.entity.vo.MedicalRecordInfo;
import cn.edu.ncu.medical.entity.vo.PrescriptionInfo;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/front/doctor/medicalRecord")
public class MedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;
    @Autowired
    private DoctorDetailService doctorDetailService;
    @Autowired
    private SystemUserService systemUserService;
    @PostMapping("/addMedicalRecord")
    public Result addMedicalRecord(@RequestBody MedicalRecord medicalRecord) {
        // 初始状态设为2（未开具）
        medicalRecord.setIsPurchasable(2);
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        Long doctorId = doctorDetailService.getDoctorIdByUserId(userId);
        medicalRecord.setDoctorId(doctorId);
        Date date = new Date();
        medicalRecord.setCreateTime(date);
        medicalRecordService.save(medicalRecord);
        System.out.println(medicalRecord.getId());
        return Result.ok(medicalRecord.getId());
    }

    @PostMapping("/getMedicalRecordList")
    public Result getMedicalRecord(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                   @RequestBody(required = false) MedicalRecordCondition medicalRecordCondition
                                   ) {
        Page<MedicalRecordInfo> page = new Page<>(pageNum, pageSize);
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        Long doctorId = doctorDetailService.getDoctorIdByUserId(userId);
        return Result.ok(medicalRecordService.getMedicalRecordByDoctorId(doctorId, page, medicalRecordCondition));
    }

    @GetMapping("/getPrescriptionInfoByMedicalRecordId")
    public Result getPrescriptionInfoByMedicalRecordId(@RequestParam("medicalRecordId") Long medicalRecordId) {
        List<PrescriptionInfo> prescriptionInfoList = medicalRecordService.getPrescriptionInfoByMedicalRecordId(medicalRecordId);
        return Result.ok(prescriptionInfoList);
    }
}
