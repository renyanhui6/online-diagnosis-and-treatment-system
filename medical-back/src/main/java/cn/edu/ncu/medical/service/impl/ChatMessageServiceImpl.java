package cn.edu.ncu.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.ChatMessage;
import cn.edu.ncu.medical.service.ChatMessageService;
import cn.edu.ncu.medical.mapper.ChatMessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
* @author star
* @description 针对表【chat_message】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class ChatMessageServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage>
    implements ChatMessageService{
    @Override
    public List<ChatMessage> getMessagesByRoomId(String roomId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getRoomId, Long.valueOf(roomId))
                .orderByAsc(ChatMessage::getCreateTime);
        return this.list(wrapper);
    }
}




