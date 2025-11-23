package cn.edu.ncu.medical.controller.patient;

import cn.edu.ncu.medical.entity.AppointmentPaymentRecord;
import cn.edu.ncu.medical.entity.Drug;
import cn.edu.ncu.medical.entity.MedicineOrder;
import cn.edu.ncu.medical.entity.OrderPaymentRecord;
import cn.edu.ncu.medical.entity.dto.Medicine;
import cn.edu.ncu.medical.entity.vo.MedicineInfo;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.mapper.OrderPaymentRecordMapper;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.OrderPaymentRecordService;
import cn.edu.ncu.medical.topic.RabbitTopicSender;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/front/patient/medicine")
public class medicineOrderController {
    @Autowired
    private OrderPaymentRecordService orderPaymentRecordService;
    @Autowired
    private RabbitTopicSender  rabbitTopicSender;

    //传入参数为就诊记录id
    //创建订单
    //使用处方创建
    @PostMapping("/createByPrescription")
    public Result<Long> createMedicineOrderByPrescription(@RequestParam("id") Long id) {
       /* Long orderId = orderPaymentRecordService.createOrderByPrescription(id, LoginUserHolder.getLoginUser().getUserId());*/
        rabbitTopicSender.sendMedicineCreateByPrescriptionMsg(id);
        return Result.ok();
    }
    //传参数为订单记录id
    //未付款时取消
    @PostMapping("/cancel")
    public Result cancelMedicineOrderById(@RequestParam("id") Long id) {
        orderPaymentRecordService.cancelOrder(id);
        return Result.ok();
    }
    //自行购买创建
    @PostMapping("/createOrder")
    public Result<Long> createMedicineOrderByOrderIdLong(@RequestBody List<Medicine> medicineList) {
        /*Long id = orderPaymentRecordService.createOrder(medicineList);*/
        rabbitTopicSender.sendMedicineCreateSelfPurchaseMsg(medicineList);
        return Result.ok();
    }
    //传入参数字段OrderPayment的 id 及paymentGateway;
    //完成支付
    @PostMapping("/completeOrder")
    public Result completeMedicineOrderById(@RequestBody OrderPaymentRecord orderPaymentRecord) {
    //orderPaymentRecordService.completeOrder(orderPaymentRecord);
        rabbitTopicSender.sendMedicineOrderCompleteMsg(orderPaymentRecord);
        return Result.ok();
    }
    //传入订单记录id
    //支付后退款
    @PostMapping("/refund")
    public Result refundMedicineOrderById(@RequestParam Long id) {
        orderPaymentRecordService.rufund(id);
        return Result.ok();
    }

    //对订单进行核销操作 0 未支付 1 已支付 2 已退款 3 已核销
    //当为 1 已支付时旁边一个按钮核销，点击进入已核销状态，已核销后无法退款
    @PostMapping("/vertify")
    public Result vertifyMedicineOrderById(@RequestParam("id") Long id){
        orderPaymentRecordService.vertifyPayment(id);
        return Result.ok();
    }



    //得到订单列表，日期可以不要 (得到结果只包含付款及状态信息)
    @GetMapping("/getOrders")
    public Result<IPage<OrderPaymentRecord>> getOrders(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,  // 页码（从1开始）
                                                             @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize, // 每页条数
                                                             @RequestParam(value = "createDate",required = false ) @DateTimeFormat(pattern = "yyyy-MM-dd") Date createDate){
        IPage<OrderPaymentRecord> ipage = orderPaymentRecordService.getOrders(pageNum,pageSize,createDate);
        return Result.ok(ipage);
    }

    //传入参数id为订单号id
    @GetMapping("/getDrugs")
    public Result<IPage<MedicineInfo>> getDrugs(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum,  // 页码（从1开始）
                                                       @RequestParam(value = "pageSize",defaultValue = "10") Integer pageSize, // 每页条数
                                                       @RequestParam(value = "id",required = false ) Long id){
        IPage<MedicineInfo> ipage = orderPaymentRecordService.getDrugsByOrderId(pageNum,pageSize,id);
        return Result.ok(ipage);
    }

}
