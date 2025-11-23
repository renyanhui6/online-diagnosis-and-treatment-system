package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.ChatMessage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author star
* @description 针对表【chat_message】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface ChatMessageService extends IService<ChatMessage> {
    /**
     * 根据房间ID获取聊天记录
     */
    List<ChatMessage> getMessagesByRoomId(String roomId);

}
