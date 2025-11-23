package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.MedicalRecord;
import cn.edu.ncu.medical.entity.dto.MedicalRecordCondition;
import cn.edu.ncu.medical.entity.vo.MedicalRecordInfo;
import cn.edu.ncu.medical.entity.vo.PrescriptionInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Date;
import java.util.List;

/**
* @author star
* @description 针对表【medical_record】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface MedicalRecordService extends IService<MedicalRecord> {


    // 用户根据用户id查询病历
    IPage<MedicalRecordInfo> getMedicalRecordByUserId(Long userId, Page<MedicalRecordInfo> page,MedicalRecordCondition medicalRecordCondition);


    // 根据病历id查询处方信息
    List<PrescriptionInfo> getPrescriptionInfoByMedicalRecordId(Long medicalRecordId);

    //医生查所有病例
    IPage<MedicalRecordInfo> getMedicalRecordByDoctorId(Long doctorId, Page<MedicalRecordInfo> page, MedicalRecordCondition medicalRecordCondition);
}
