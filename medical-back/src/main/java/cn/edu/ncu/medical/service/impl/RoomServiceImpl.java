package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.utils.RedisCache;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.Room;
import cn.edu.ncu.medical.service.RoomService;
import cn.edu.ncu.medical.mapper.RoomMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
* @author star
* @description 针对表【room】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class RoomServiceImpl extends ServiceImpl<RoomMapper, Room>
    implements RoomService{
	@Autowired
	private RedisCache redisCache;

	@Override
	public Room createOrUpdateRoom(Long registrationId, Integer status) {
		// 查询是否已存在房间
		LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Room::getRegistrationId, registrationId);
		Room room = this.getOne(wrapper);

		if (room == null) {
			// 创建新房间
			room = new Room();
			room.setRegistrationId(registrationId);
			room.setCreateTime(new Date());
		}

		room.setRoomStatus(status);
		room.setUpdateTime(new Date());

		this.saveOrUpdate(room);
		return room;
	}

	@Override
	public void updateRoomStatus(Long registrationId, Integer status) {
		LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Room::getRegistrationId, registrationId);

		Room room = new Room();
		room.setRoomStatus(status);
		room.setUpdateTime(new Date());

		this.update(room, wrapper);
	}

	@Override
	public Room getRoomByRegistrationId(Long registrationId) {
		LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<>();
		wrapper.eq(Room::getRegistrationId, registrationId);
		return this.getOne(wrapper);
	}
}




