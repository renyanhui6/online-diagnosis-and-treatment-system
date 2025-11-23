package cn.edu.ncu.medical.mapper;

import cn.edu.ncu.medical.entity.Drug;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author star
* @description 针对表【drug】的数据库操作Mapper
* @createDate 2025-07-24 17:44:39
* @Entity cn.edu.ncu.medical.entity.Drug
*/
@Mapper
public interface DrugMapper extends BaseMapper<Drug> {

}




