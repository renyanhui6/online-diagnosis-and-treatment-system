package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.Drug;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author star
* @description 针对表【drug】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface DrugService extends IService<Drug> {


    /**
     * 根据搜索条件查询药品列表
     *
     * @param page
     * @param search
     * @return
     */
    IPage<Drug> getDrugListBySearch(Page<Drug> page, String search);

    /**
     * 获取所有药品列表(管理员)
     * @param page
     * @return
     */
    IPage<Drug> getDrugList(Page<Drug> page);
}
