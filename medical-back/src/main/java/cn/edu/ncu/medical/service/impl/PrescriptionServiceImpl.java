package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.entity.Registration;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.Prescription;
import cn.edu.ncu.medical.service.PrescriptionService;
import cn.edu.ncu.medical.mapper.PrescriptionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author star
* @description 针对表【prescription】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class PrescriptionServiceImpl extends ServiceImpl<PrescriptionMapper, Prescription>
    implements PrescriptionService{



}




