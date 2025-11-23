package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.entity.*;
import cn.edu.ncu.medical.exception.MedicineOrderException;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.service.MedicineOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
* @author star
* @description 针对表【medicine_order】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class MedicineOrderServiceImpl extends ServiceImpl<MedicineOrderMapper, MedicineOrder>
    implements MedicineOrderService{

}




