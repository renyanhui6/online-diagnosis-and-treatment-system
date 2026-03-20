package cn.edu.ncu.medical.entity.vo;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class RegistrationInfo {

    /**
     * 挂号详细信息，用于前端
     */

    private Long id;
    private Integer registrationStatus;
    private String patientName;
    private Long patientId;
    private Long patientUserId;
    private String patientPhone;
    private String patientGender;
    private Integer patientAge;
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    private String departmentName;

    private String doctorName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date scheduleDate;

    private String timePeriod;//上午/下午

}
