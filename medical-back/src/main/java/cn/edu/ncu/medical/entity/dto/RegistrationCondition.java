package cn.edu.ncu.medical.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class RegistrationCondition {

    //2 - 'queuing'（排队中）
    //     * 3 - in_progress - 问诊中
    //4- 'completed'（已完成）
    //5- suspended '（患者未及时响应，暂时挂起，等待后续处理）
    //  * 6-“已回归”
    private Integer registrationStatus;
    //开始时间和结束时间
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;
}
