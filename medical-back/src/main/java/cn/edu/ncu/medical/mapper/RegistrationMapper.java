package cn.edu.ncu.medical.mapper;

import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.dto.RegistrationCondition;
import cn.edu.ncu.medical.entity.vo.RegistrationInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author star
* @description 针对表【registration】的数据库操作Mapper
* @createDate 2025-07-24 17:44:39
* @Entity cn.edu.ncu.medical.entity.Registration
*/
@Mapper
public interface RegistrationMapper extends BaseMapper<Registration> {

    /**
     * 根据用户ID查询挂号信息
     * @param userId
     * @param page
     * @param registrationCondition
     * @return
     */
    IPage<RegistrationInfo> selectRegistrationInfoList(Long userId, Page<RegistrationInfo> page, RegistrationCondition registrationCondition);


    /**
     * 根据医生ID和排班ID查询挂号信息
     * @param doctorId
     * @param page
     * @param scheduleId
     * @param registrationCondition
     * @return
     */
    IPage<RegistrationInfo> selectRegistrationInfoBySchedule(Long doctorId, Page<RegistrationInfo> page,Long scheduleId, RegistrationCondition registrationCondition);

    /**
     * 根据医生ID查询所有挂号信息
     * @param doctorId
     * @param page
     * @param registrationCondition
     * @return
     */
    IPage<RegistrationInfo> selectAllRegistrationInfo(Long doctorId, Page<RegistrationInfo> page, RegistrationCondition registrationCondition);

    /**
     * 根据挂号ID查询详细信息
     * @param registrationId
     * @return
     */
    RegistrationInfo selectRegistrationInfoById(Long registrationId);
}



