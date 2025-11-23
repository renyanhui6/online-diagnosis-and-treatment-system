package cn.edu.ncu.medical.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.util.Date;
import lombok.Data;


/**
 * 聊天消息实体类
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    /**
     * 消息ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 房间ID（关联room表的id字段）
     */
    private Long roomId;

    /**
     * 发送者类型（1-患者，2-医生）
     */
    private Integer senderType;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 消息类型（1-文本，2-图片）
     */
    private Integer messageType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 软删除标记（0-未删除，1-已删除）
     */
    @TableLogic
    private Integer isDeleted;
}





