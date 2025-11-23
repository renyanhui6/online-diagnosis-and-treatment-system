package cn.edu.ncu.medical.mapper;

import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.entity.vo.SystemUserPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
* @author star
* @description 针对表【system_user】的数据库操作Mapper
* @createDate 2025-07-24 17:44:39
* @Entity cn.edu.ncu.medical.entity.SystemUser
*/
@Mapper
public interface SystemUserMapper extends BaseMapper<SystemUser> {


	IPage<SystemUser> selectDoctorList(IPage<SystemUser> page);
    IPage<SystemUserPage> selectByCondition(IPage<SystemUserPage> page, @Param("condition") SystemUserPage condition);
}




