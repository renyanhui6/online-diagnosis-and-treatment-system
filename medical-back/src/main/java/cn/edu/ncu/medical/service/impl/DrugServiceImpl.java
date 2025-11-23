package cn.edu.ncu.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.Drug;
import cn.edu.ncu.medical.service.DrugService;
import cn.edu.ncu.medical.mapper.DrugMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
* @author star
* @description 针对表【drug】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class DrugServiceImpl extends ServiceImpl<DrugMapper, Drug>
    implements DrugService{

    @Autowired
    private DrugMapper drugMapper;

    public IPage<Drug> getDrugListBySearch(Page<Drug> page, String search) {
        LambdaQueryWrapper<Drug> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Drug::getGenericName,search);
        queryWrapper.eq(Drug::getIsDeleted,0);
        queryWrapper.gt(Drug::getQuantity,0);
        queryWrapper.orderByAsc(Drug::getId);

        return drugMapper.selectPage(page,queryWrapper);
    }



    @Override
    public IPage<Drug> getDrugList(Page<Drug> page) {
        LambdaQueryWrapper<Drug> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Drug::getIsDeleted,0);
        queryWrapper.gt(Drug::getQuantity,0);
        queryWrapper.orderByAsc(Drug::getId);

        return drugMapper.selectPage(page,queryWrapper);
    }
}




