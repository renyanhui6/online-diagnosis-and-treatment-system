package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("registration_person_lock")
public class RegistrationPersonLock {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("schedule_id")
    private Long scheduleId;

    @TableField("person_key")
    private String personKey;

    @TableField("request_token")
    private String requestToken;

    @TableField("registration_id")
    private Long registrationId;

    @TableField("create_time")
    private Date createTime;
}
