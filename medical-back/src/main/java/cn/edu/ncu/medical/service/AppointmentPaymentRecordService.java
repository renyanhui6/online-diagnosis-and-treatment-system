package cn.edu.ncu.medical.service;

import cn.edu.ncu.medical.entity.AppointmentPaymentRecord;
import cn.edu.ncu.medical.entity.Registration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;
import java.util.List;

/**
* @author star
* @description 针对表【appointment_payment_record】的数据库操作Service
* @createDate 2025-07-24 17:44:39
*/
public interface AppointmentPaymentRecordService extends IService<AppointmentPaymentRecord> {
    //用于创建订单
    Long createAppointmentPayment(Registration r,Long userId);



    //用于查询订单
    AppointmentPaymentRecord getAppointmentPaymentRecordById(Long id);


    //用于更新订单状态,变为已付款
    void modifyAppointmentPayment(AppointmentPaymentRecord appointmentPaymentRecord,Long userId);

    //用于退款处理
    void RefundAppointmentPayment(Long id);

    //用于未付款的订单移除处理
    void cancelAppointmentPaymentRecordById(Long id);

    //查询订单查询条件为当前天
    IPage<AppointmentPaymentRecord> getAppointmentPaymentRecords(  Integer pageNum,  // 页码（从1开始）
                                                                   Integer pageSize, // 每页条数
                                                                   Date createDate); //查询日期

    //管理员删除订单
    void cancelAppointmentPaymentRecordByIdAdmin(Long id);

}
