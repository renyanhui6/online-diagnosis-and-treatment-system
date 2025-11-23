package cn.edu.ncu.medical.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.Department;
import cn.edu.ncu.medical.service.DepartmentService;
import cn.edu.ncu.medical.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;

/**
* @author star
* @description 针对表【department】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department>
    implements DepartmentService{

}




