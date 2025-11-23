package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.PatientAttendant;
import cn.edu.ncu.medical.entity.dto.IdCard;
import cn.edu.ncu.medical.result.Result;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author star
* @description 针对表【patient_attendant】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface PatientAttendantService extends IService<PatientAttendant> {

	void updateInfo(PatientAttendant patientAttendant);


	PatientAttendant getInfo();

	void addIdCard(IdCard idCard);

	Result addPatientAttendant(IdCard idCard);

	Result getPatientList();


	void removePatientAttendant(Long patientAttendantId);
}
