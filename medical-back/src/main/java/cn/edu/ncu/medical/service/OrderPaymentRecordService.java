package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.AppointmentPaymentRecord;
import cn.edu.ncu.medical.entity.Drug;
import cn.edu.ncu.medical.entity.MedicineOrder;
import cn.edu.ncu.medical.entity.OrderPaymentRecord;
import cn.edu.ncu.medical.entity.dto.Medicine;
import cn.edu.ncu.medical.entity.vo.MedicineInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
* @author star
* @description 针对表【order_payment_record】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface OrderPaymentRecordService extends IService<OrderPaymentRecord> {
    Long createOrderByPrescription(Long orderId,Long userId);
    Long createOrder(List<Medicine> medicine,Long userId);
    void cancelOrder(Long mId);
    void rufund(Long oId);
    void completeOrder(OrderPaymentRecord orderPaymentRecord,Long userId);
    void cancelOrderByAdmin(Long oId);
    //查询订单查询条件为当前天
    IPage<OrderPaymentRecord> getOrders(Integer pageNum,  // 页码（从1开始）
                                        Integer pageSize, // 每页条数
                                        Date createDate); //查询日期
    void vertifyPayment(Long oId);

    IPage<MedicineInfo> getDrugsByOrderId(Integer pageNum,  // 页码（从1开始）
                                          Integer pageSize, Long orderId);

}
