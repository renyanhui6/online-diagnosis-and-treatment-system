package cn.edu.ncu.medical.mapper;

import cn.edu.ncu.medical.entity.OrderPaymentRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;

/**
* @author star
* @description 针对表【order_payment_record】的数据库操作Mapper
* @createDate 2025-07-24 17:44:39
* @Entity cn.edu.ncu.medical.entity.OrderPaymentRecord
*/
@Mapper
public interface OrderPaymentRecordMapper extends BaseMapper<OrderPaymentRecord> {
    BigDecimal selectAmountByPrescription(Long mId);

    BigDecimal selectPrice(Long id);

}




