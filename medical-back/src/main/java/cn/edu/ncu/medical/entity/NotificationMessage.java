package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/**
 * @TableName notification_message
 */
@TableName(value ="notification_message")
@Data
public class NotificationMessage {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer patientId;

    private String messageType;

    private Integer isRead;

    private String title;

    private String content;
    @TableLogic
    private Integer isDeleted;
}