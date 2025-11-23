package cn.edu.ncu.medical.mapper;

import cn.edu.ncu.medical.entity.MedicineOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
* @author star
* @description 针对表【medicine_order】的数据库操作Mapper
* @createDate 2025-07-24 17:44:39
* @Entity cn.edu.ncu.medical.entity.MedicineOrder
*/
@Mapper
public interface MedicineOrderMapper extends BaseMapper<MedicineOrder> {
    void insertMedicineOrder(Long orderPaymentRecordId,Long medicalRecordId);
}




