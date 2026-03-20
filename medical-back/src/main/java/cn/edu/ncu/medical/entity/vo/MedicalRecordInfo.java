package cn.edu.ncu.medical.entity.vo;

import cn.edu.ncu.medical.entity.Drug;
import cn.edu.ncu.medical.entity.Prescription;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class MedicalRecordInfo {

    // 患者姓名
    private String patientName;
    // 患者联系电话
    private String patientPhone;
    // 患者性别（展示用）
    private String patientGender;
    // 患者年龄（展示用）
    private Integer patientAge;
    // 医生姓名
    private String doctorName;
    // 挂号id
    private Long registrationId;
    // 房间id
    private Long roomId;
    // 医生描述
    private String doctorDescription;

    // 病历id
    private Long medicalRecordId;

    // 处方状态 0-未使用 1-已使用 2-未开具
    private Integer isPurchasable;
    // 创建时间
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


}
