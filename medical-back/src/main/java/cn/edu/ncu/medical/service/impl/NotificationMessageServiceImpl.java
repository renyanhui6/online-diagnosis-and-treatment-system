package cn.edu.ncu.medical.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.NotificationMessage;
import cn.edu.ncu.medical.service.NotificationMessageService;
import cn.edu.ncu.medical.mapper.NotificationMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author star
* @description 针对表【notification_message】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class NotificationMessageServiceImpl extends ServiceImpl<NotificationMessageMapper, NotificationMessage>
    implements NotificationMessageService{

}




