package cn.edu.ncu.medical.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class MedicalRecordCondition {
    //处方状态
    private Integer prescriptionStatus;

    //日期，医生端,根据具体时间查
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date createTime;

    //患者姓名
    private String patientName;



    //患者端条件，传两个时间
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

}
