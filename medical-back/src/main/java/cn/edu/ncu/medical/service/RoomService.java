package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.Room;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Date;

/**
* @author star
* @description 针对表【room】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface RoomService extends IService<Room> {
	/**
	 * 创建或更新房间
	 */
	Room createOrUpdateRoom(Long registrationId, Integer status);

	/**
	 * 更新房间状态
	 */
	void updateRoomStatus(Long registrationId, Integer status);

	/**
	 * 根据预约ID获取房间
	 */
	Room getRoomByRegistrationId(Long registrationId);
}

