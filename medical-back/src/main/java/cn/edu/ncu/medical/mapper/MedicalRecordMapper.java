package cn.edu.ncu.medical.mapper;

import cn.edu.ncu.medical.entity.MedicalRecord;
import cn.edu.ncu.medical.entity.dto.MedicalRecordCondition;
import cn.edu.ncu.medical.entity.vo.MedicalRecordInfo;
import cn.edu.ncu.medical.entity.vo.PrescriptionInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

/**
* @author star
* @description 针对表【medical_record】的数据库操作Mapper
* @createDate 2025-07-24 17:44:39
* @Entity cn.edu.ncu.medical.entity.MedicalRecord
*/
@Mapper
public interface MedicalRecordMapper extends BaseMapper<MedicalRecord> {

    /**
     * 新增病历
     * @param medicalRecord
     */
    void insertMedicalRecord(MedicalRecord medicalRecord);

    /**
     * 根据用户id查询病历
     * @param userId
     * @param page
     * @return
     */
    IPage<MedicalRecordInfo> selectMedicalRecordByUserId(Long userId, Page<MedicalRecordInfo> page, MedicalRecordCondition medicalRecordCondition);

    /**
     * 根据病历id查询处方信息
     * @param medicalRecordId
     * @return
     */
    List<PrescriptionInfo> selectPrescriptionInfoByMedicalRecordId(Long medicalRecordId);


    /**
     * 根据医生id查询病历
     * @param doctorId
     * @param page
     * @return
     */
    IPage<MedicalRecordInfo> selectMedicalRecordByDoctorId(Long doctorId, Page<MedicalRecordInfo> page, MedicalRecordCondition medicalRecordCondition);


}




