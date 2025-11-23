package cn.edu.ncu.medical.controller.doctor;

import cn.edu.ncu.medical.entity.MedicalRecord;
import cn.edu.ncu.medical.entity.Prescription;
import cn.edu.ncu.medical.entity.dto.Medicine;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.MedicalRecordService;
import cn.edu.ncu.medical.service.PrescriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/front/doctor/prescription")
public class PrescriptionController {

    @Autowired
    private PrescriptionService prescriptionService;
    @Autowired
    private MedicalRecordService medicalRecordService;

    @PostMapping("/addPrescription")
    public Result addPrescription(@RequestBody List<Medicine> medicines,
                                  @RequestParam Long medicalRecordId) {
        // 保存所有处方明细
        medicines.forEach(medicine -> {
            Prescription prescription = new Prescription();
            prescription.setDrugId(medicine.getDrugId());
            prescription.setDrugQuantity(medicine.getDrugQuantity());
            prescription.setMedicalRecordId(medicalRecordId);
            prescriptionService.save(prescription);
        });

        // 更新就诊记录状态为0（未使用）
        MedicalRecord record = medicalRecordService.getById(medicalRecordId);
        record.setIsPurchasable(0);
        medicalRecordService.updateById(record);

        return Result.ok();
    }


}
