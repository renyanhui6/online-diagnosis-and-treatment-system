package cn.edu.ncu.medical.controller.patient;

import cn.edu.ncu.medical.entity.AppointmentPaymentRecord;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.AppointmentPaymentRecordService;
import cn.edu.ncu.medical.topic.RabbitTopicSender;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/front/patient/appointment")
public class AppointRegistrationController {
    @Autowired
    private AppointmentPaymentRecordService appointmentPaymentRecordService;
    @Autowired
    private RabbitTopicSender rabbitTopicSender;

    //创建订单
    //创建预约并创建支付记录
    //返回值为订单号
    @PostMapping("/create")
    public Result Create(@RequestBody Registration registration) {
        /*Long id = null;
        id = appointmentPaymentRecordService.createAppointmentPayment(registration);*/
        rabbitTopicSender.sendAppointmentCreateMsg(registration);
        return Result.ok();
    }

    //查询订单
    //返回值为订单对象
    @GetMapping("/getOrder")
    public Result<AppointmentPaymentRecord> getOrder(@RequestParam Long id){

        return Result.ok(appointmentPaymentRecordService.getAppointmentPaymentRecordById(id));

    }


    //得到挂号订单列表，日期可以不要
    @GetMapping("/getOrders")
    public Result<IPage<AppointmentPaymentRecord>> getOrders(@RequestParam("pageNum") Integer pageNum,  // 页码（从1开始）
                                                            @RequestParam("pageSize") Integer pageSize, // 每页条数
                                                            @RequestParam(value = "createDate",required = false ) @DateTimeFormat(pattern = "yyyy-MM-dd") Date createDate){
        IPage<AppointmentPaymentRecord> ipage = appointmentPaymentRecordService.getAppointmentPaymentRecords(pageNum,pageSize,createDate);
        return Result.ok(ipage);
    }




    //完成订单
    //返回值为默认
    @PostMapping("/finish")
    public Result finish(@RequestBody AppointmentPaymentRecord appointmentPaymentRecord){

        /*appointmentPaymentRecordService.modifyAppointmentPayment(appointmentPaymentRecord);*/
        rabbitTopicSender.sendAppointmentFinishMsg(appointmentPaymentRecord);
        return Result.ok();
    }


    //订单退款处理
    @GetMapping("/refund")
    public Result refund(@RequestParam Long id){
        appointmentPaymentRecordService.RefundAppointmentPayment(id);
        return Result.ok();
    }

    //未付款时取消
    @GetMapping("/cancel")
    public Result cancel(@RequestParam Long id){
        appointmentPaymentRecordService.cancelAppointmentPaymentRecordById(id);
        return Result.ok();
    }

}
