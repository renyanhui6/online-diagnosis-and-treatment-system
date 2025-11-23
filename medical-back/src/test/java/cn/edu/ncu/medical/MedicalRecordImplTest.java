package cn.edu.ncu.medical;

import cn.edu.ncu.medical.entity.MedicalRecord;
import cn.edu.ncu.medical.entity.dto.MedicalRecordCondition;
import cn.edu.ncu.medical.entity.vo.MedicalRecordInfo;
import cn.edu.ncu.medical.entity.vo.PrescriptionInfo;
import cn.edu.ncu.medical.service.MedicalRecordService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class MedicalRecordImplTest {
    @Autowired
    private MedicalRecordService medicalRecordService;

    @Test
    public void addMedicalRecord() {
        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setPatientId(1L);
        medicalRecord.setDoctorId(131L);
        medicalRecord.setDoctorDescription("医生描述111");
        medicalRecord.setIsPurchasable(0);

        medicalRecordService.save(medicalRecord);
    }

    @Test
    public void getMedicalRecordByUserId() {

        MedicalRecordCondition medicalRecordCondition = new MedicalRecordCondition();

        IPage<MedicalRecordInfo> medicalRecordInfoIPage = medicalRecordService.getMedicalRecordByUserId(3L, new Page<>(1,5),medicalRecordCondition);
        System.out.println(medicalRecordInfoIPage);
    }

    @Test
    public void getMedicalRecordByDoctorId() {
        MedicalRecordCondition medicalRecordCondition = new MedicalRecordCondition();
        Integer pageNum = 1;
        Integer pageSize = 10;
        Page<MedicalRecordInfo> page = new Page<>(pageNum,pageSize);
        Long doctorId = 132L;
        medicalRecordCondition.setPrescriptionStatus(1);
        IPage<MedicalRecordInfo> medicalRecordInfoIPage = medicalRecordService.getMedicalRecordByDoctorId(doctorId,page,medicalRecordCondition);
        System.out.println(medicalRecordInfoIPage);
    }

    @Test
    public void getPrescriptionByMedicalRecordId() {
        List<PrescriptionInfo> prescriptionInfoList = medicalRecordService.getPrescriptionInfoByMedicalRecordId(8L);
        System.out.println(prescriptionInfoList);
    }

}
