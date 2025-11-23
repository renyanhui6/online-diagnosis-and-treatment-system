package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.entity.dto.MedicalRecordCondition;
import cn.edu.ncu.medical.entity.vo.MedicalRecordInfo;
import cn.edu.ncu.medical.entity.vo.PrescriptionInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.MedicalRecord;
import cn.edu.ncu.medical.service.MedicalRecordService;
import cn.edu.ncu.medical.mapper.MedicalRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
* @author star
* @description 针对表【medical_record】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class MedicalRecordServiceImpl extends ServiceImpl<MedicalRecordMapper, MedicalRecord>
    implements MedicalRecordService{

    @Autowired
    private MedicalRecordMapper medicalRecordMapper;



    @Override
    public IPage<MedicalRecordInfo> getMedicalRecordByUserId(Long userId, Page<MedicalRecordInfo> page,MedicalRecordCondition medicalRecordCondition) {
        // 根据用户id查询病历

        IPage<MedicalRecordInfo> medicalRecordInfoIPage = medicalRecordMapper.selectMedicalRecordByUserId(userId, page, medicalRecordCondition);
        return medicalRecordInfoIPage;
    }

    @Override
    public List<PrescriptionInfo> getPrescriptionInfoByMedicalRecordId(Long medicalRecordId) {
        // 根据病历id查询处方信息
        List<PrescriptionInfo> prescriptionInfoList = medicalRecordMapper.selectPrescriptionInfoByMedicalRecordId(medicalRecordId);
        return prescriptionInfoList;
    }

    @Override
    public IPage<MedicalRecordInfo> getMedicalRecordByDoctorId(Long doctorId, Page<MedicalRecordInfo> page, MedicalRecordCondition medicalRecordCondition) {
        // 根据医生id查询病历
        IPage<MedicalRecordInfo> medicalRecordInfoIPage = medicalRecordMapper.selectMedicalRecordByDoctorId(doctorId, page, medicalRecordCondition);
        return medicalRecordInfoIPage;
    }
}




