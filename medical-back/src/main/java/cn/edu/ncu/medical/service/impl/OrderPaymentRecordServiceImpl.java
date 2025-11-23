package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.config.event.DelayQueueProducer;
import cn.edu.ncu.medical.entity.*;
import cn.edu.ncu.medical.entity.dto.Medicine;
import cn.edu.ncu.medical.entity.vo.MedicineInfo;
import cn.edu.ncu.medical.exception.MedicineOrderException;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.mapper.*;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.service.OrderPaymentRecordService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author star
 * @description 针对表【order_payment_record】的数据库操作Service实现
 * @createDate 2025-07-24 17:44:39
 */
@Service
public class OrderPaymentRecordServiceImpl extends ServiceImpl<OrderPaymentRecordMapper, OrderPaymentRecord>
        implements OrderPaymentRecordService {
    @Autowired
    private MedicineOrderMapper medicineOrderMapper;

    @Autowired
    private OrderPaymentRecordMapper orderPaymentRecordMapper;

    @Autowired
    private PrescriptionMapper prescriptionMapper;

    @Autowired
    private DrugMapper drugMapper;

    @Autowired
    private MedicalRecordMapper medicalRecordMapper;

    @Autowired
    private IService<MedicineOrder> medicineOrderService;

    /**
     * 订单创建事件（创建订单后发布）
     */
    public class OrderCreatedEvent {
        private final Long orderId;

        public OrderCreatedEvent(Long orderId) {
            this.orderId = orderId;
        }

        public Long getOrderId() {
            return orderId;
        }
    }

    /**
     * 订单删除事件（删除订单后发布）
     */
    public class OrderDeletedEvent {
        private final Long orderId;

        public OrderDeletedEvent(Long orderId) {
            this.orderId = orderId;
        }

        public Long getOrderId() {
            return orderId;
        }
    }



    @Autowired
    private DelayQueueProducer delayQueueProducer;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void addToDelayQueue(OrderCreatedEvent orderCreatedEvent) {
        delayQueueProducer.addMedicineOrder(orderCreatedEvent.getOrderId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void removefromDelayQueue(OrderDeletedEvent orderDeletedEvent) {
        delayQueueProducer.removeMedicineOrder(orderDeletedEvent.getOrderId());
    }


    //创建订单
    //传入参数为prescriptionId
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createOrderByPrescription(Long mId, Long userId) {

        //校验合法性
        //查询medicine_order
        MedicalRecord medicalRecord = medicalRecordMapper.selectById(mId);

        if (medicalRecord == null) {
            throw new MedicineOrderException(ResultCodeEnum.REGISTRATION_RECORD_ERROR);
        }
        if(medicalRecord.getIsPurchasable() != 0){
            throw new MedicineOrderException(ResultCodeEnum.PRISCRIPTION_ERROR);
        }

        //判断药品库存是否合法
        LambdaQueryWrapper<Prescription> prescriptionWrapper = new LambdaQueryWrapper<>();
        prescriptionWrapper.eq(Prescription::getMedicalRecordId, mId);
        List<Prescription> prescriptions = prescriptionMapper.selectList(prescriptionWrapper);

        for (Prescription prescription : prescriptions) {
            Drug drug = drugMapper.selectById(prescription.getDrugId());
            if (drug==null) throw new MedicineOrderException(ResultCodeEnum.MEDICINEORDER_INVALID);
            if((drug.getQuantity()-prescription.getDrugQuantity())<0) throw new MedicineOrderException(ResultCodeEnum.DRUG_QUANTITIES_SHORTAGE);
            //药品数量减一
            drug.setQuantity(drug.getQuantity()-prescription.getDrugQuantity());
            drugMapper.updateById(drug);
        }


        //先往order_payment_record插入数据回显得到主键
        OrderPaymentRecord orderPaymentRecord = new OrderPaymentRecord();
        //查询drug金额信息
        BigDecimal amount = orderPaymentRecordMapper.selectAmountByPrescription(mId);
        if (BigDecimal.ZERO.compareTo(amount) == 0 || amount == null) {
            throw new MedicineOrderException(ResultCodeEnum.DATA_ERROR);
        }
        orderPaymentRecord.setPaymentAmount(amount);
        //标记订单拥有者

        orderPaymentRecord.setPayerId(userId);
        //标记为根据处方生成
        orderPaymentRecord.setOrderSource(1);
        //标记相关联的处方
        orderPaymentRecord.setMedicalRecordId(mId);
        //标记订单状态
        orderPaymentRecord.setPaymentStatus(0);
        //金额状态


        orderPaymentRecordMapper.insert(orderPaymentRecord);
        Long orderId = orderPaymentRecord.getId();
        //用回显得到的主键，往medicine_order插入数据drug_id,order_payment_recored_id

        //medicine_order插入数据
        medicineOrderMapper.insertMedicineOrder(orderId, mId);
        //返回值为order_payment_record的id

        //处方表变为已购买
        LambdaUpdateWrapper<MedicalRecord> medicalRecordWrapper = new LambdaUpdateWrapper<MedicalRecord>();
        medicalRecordWrapper.eq(MedicalRecord::getId, mId).set(MedicalRecord::getIsPurchasable, 1);

        medicalRecordMapper.update(medicalRecordWrapper);

        applicationEventPublisher.publishEvent(new OrderCreatedEvent(orderId));
        return orderId;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelOrder(Long oId) {


        OrderPaymentRecord orderPaymentRecord = orderPaymentRecordMapper.selectById(oId);
        if (orderPaymentRecord == null) {
            throw new MedicineOrderException(ResultCodeEnum.ORDER_NOT_EXIST);
        }

        //判断操作者是否合法
        if (orderPaymentRecord.getPayerId() != LoginUserHolder.getLoginUser().getUserId()) {
            throw new MedicineOrderException(ResultCodeEnum.OPERATION_ERROR);
        }
        //判断订单状态
        if (orderPaymentRecord.getPaymentStatus() != 0) {
            throw new MedicineOrderException(ResultCodeEnum.ORDER_STATUS_ILLEGAL);
        }


        //判断是否为处方购买
        Integer orderSource = orderPaymentRecord.getOrderSource();
        if (orderSource == 1) {
            LambdaUpdateWrapper<MedicalRecord> medicalRecordWrapper = new LambdaUpdateWrapper<>();
            medicalRecordWrapper.eq(MedicalRecord::getId, orderPaymentRecord.getMedicalRecordId())
                    .set(MedicalRecord::getIsPurchasable, 0);
            medicalRecordMapper.update(medicalRecordWrapper);
        }

        //药品库存处理
        LambdaQueryWrapper<MedicineOrder> medicineOrderLambdaQueryWrapper = new LambdaQueryWrapper<>();
        medicineOrderLambdaQueryWrapper.eq(MedicineOrder::getOrderPaymentRecordId, oId);


        List<MedicineOrder> medicineOrders = medicineOrderMapper.selectList(medicineOrderLambdaQueryWrapper);
        for (MedicineOrder medicineOrder : medicineOrders) {
            Drug drug = drugMapper.selectById(medicineOrder.getDrugId());
            drug.setQuantity(drug.getQuantity()+medicineOrder.getDrugQuantity());
            drugMapper.updateById(drug);
        }

        medicineOrderMapper.delete(medicineOrderLambdaQueryWrapper);

        orderPaymentRecordMapper.deleteById(oId);

        applicationEventPublisher.publishEvent(new OrderDeletedEvent(oId));
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancelOrderByAdmin(Long oId) {

        OrderPaymentRecord orderPaymentRecord = orderPaymentRecordMapper.selectById(oId);
        if (orderPaymentRecord == null) {
            throw new MedicineOrderException(ResultCodeEnum.ORDER_NOT_EXIST);
        }

        //判断订单状态
        if (orderPaymentRecord.getPaymentStatus() != 0) {
            throw new MedicineOrderException(ResultCodeEnum.ORDER_STATUS_ILLEGAL);
        }


        //判断是否为处方购买
        Integer orderSource = orderPaymentRecord.getOrderSource();
        if (orderSource == 1) {
            LambdaUpdateWrapper<MedicalRecord> medicalRecordWrapper = new LambdaUpdateWrapper<>();
            medicalRecordWrapper.eq(MedicalRecord::getId, orderPaymentRecord.getMedicalRecordId())
                    .set(MedicalRecord::getIsPurchasable, 0);
            medicalRecordMapper.update(medicalRecordWrapper);
        }



        LambdaQueryWrapper<MedicineOrder> medicineOrderLambdaQueryWrapper = new LambdaQueryWrapper<>();
        medicineOrderLambdaQueryWrapper.eq(MedicineOrder::getOrderPaymentRecordId, oId);

        //药品库存处理

        List<MedicineOrder> medicineOrders = medicineOrderMapper.selectList(medicineOrderLambdaQueryWrapper);
        for (MedicineOrder medicineOrder : medicineOrders) {
            Drug drug = drugMapper.selectById(medicineOrder.getDrugId());
            drug.setQuantity(drug.getQuantity()+medicineOrder.getDrugQuantity());
            drugMapper.updateById(drug);
        }



        medicineOrderMapper.delete(medicineOrderLambdaQueryWrapper);
        orderPaymentRecordMapper.deleteById(oId);

    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long createOrder(List<Medicine> medicineList,Long userId) {

        //校验合法性
        //查询medicine_order
        if (medicineList == null) {
            throw new MedicineOrderException(ResultCodeEnum.DATA_ERROR);
        }
        //检验药品并得到总金额
        Long drugId;
        BigDecimal price;
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal itemAmount = BigDecimal.ZERO;
        for (Medicine medicine : medicineList) {
            drugId = medicine.getDrugId();
            if (drugId == null) throw new MedicineOrderException(ResultCodeEnum.DATA_ERROR);

            //药品库存减少
            Drug drug = drugMapper.selectById(drugId);
            if((drug.getQuantity()-medicine.getDrugQuantity())<0) throw new MedicineOrderException(ResultCodeEnum.DRUG_QUANTITIES_SHORTAGE);
            drug.setQuantity(drug.getQuantity()-medicine.getDrugQuantity());
            drugMapper.updateById(drug);

            price = orderPaymentRecordMapper.selectPrice(drugId);
            if (price == null) throw new MedicineOrderException(ResultCodeEnum.DATA_ERROR);
            itemAmount = BigDecimal.valueOf(medicine.getDrugQuantity()).multiply(price);
            amount = amount.add(itemAmount);
        }


        //先往order_payment_record插入数据回显得到主键
        OrderPaymentRecord orderPaymentRecord = new OrderPaymentRecord();
        //查询金额信息
        orderPaymentRecord.setPaymentAmount(amount);
        //标记订单拥有者

        orderPaymentRecord.setPayerId(userId);
        //标记为非处方生成
        orderPaymentRecord.setOrderSource(0);
        //标记订单状态
        orderPaymentRecord.setPaymentStatus(0);

        orderPaymentRecordMapper.insert(orderPaymentRecord);
        Long orderPaymentRecordId = orderPaymentRecord.getId();
        //用回显得到的主键，往medicine_order插入数据drug_id,quantity,order_payment_recored_id

        //medicine_order插入数据
        List<MedicineOrder> orderList = medicineList.stream().map(medicine -> {
            MedicineOrder order = new MedicineOrder();
            order.setDrugId(medicine.getDrugId());
            order.setDrugQuantity(medicine.getDrugQuantity());
            order.setOrderPaymentRecordId(orderPaymentRecordId);
            return order;
        }).collect(Collectors.toList());
        medicineOrderService.saveBatch(orderList);

        applicationEventPublisher.publishEvent(new OrderCreatedEvent(orderPaymentRecordId));
        return orderPaymentRecordId;
    }


    @Transactional(rollbackFor = Exception.class)
    @Override
    public void rufund(Long oId) {


        OrderPaymentRecord orderPaymentRecord = orderPaymentRecordMapper.selectById(oId);
        if (orderPaymentRecord == null) {
            throw new MedicineOrderException(ResultCodeEnum.ORDER_NOT_EXIST);
        }

        //判断操作者是否合法
        if (orderPaymentRecord.getPayerId() != LoginUserHolder.getLoginUser().getUserId()) {
            throw new MedicineOrderException(ResultCodeEnum.OPERATION_ERROR);
        }
        //判断订单状态
        if (orderPaymentRecord.getPaymentStatus() != 1) {
            throw new MedicineOrderException(ResultCodeEnum.ORDER_STATUS_ILLEGAL);
        }


        //判断是否为处方购买
        Integer orderSource = orderPaymentRecord.getOrderSource();
        if (orderSource == 1) {
            LambdaUpdateWrapper<MedicalRecord> medicalRecordWrapper = new LambdaUpdateWrapper<>();
            medicalRecordWrapper.eq(MedicalRecord::getId, orderPaymentRecord.getMedicalRecordId())
                    .set(MedicalRecord::getIsPurchasable, 0);
            medicalRecordMapper.update(medicalRecordWrapper);
        }

        //药品库存处理
        LambdaQueryWrapper<MedicineOrder> medicineOrderLambdaQueryWrapper = new LambdaQueryWrapper<>();
        medicineOrderLambdaQueryWrapper.eq(MedicineOrder::getOrderPaymentRecordId, oId);


        List<MedicineOrder> medicineOrders = medicineOrderMapper.selectList(medicineOrderLambdaQueryWrapper);
        for (MedicineOrder medicineOrder : medicineOrders) {
            Drug drug = drugMapper.selectById(medicineOrder.getDrugId());
            drug.setQuantity(drug.getQuantity()+medicineOrder.getDrugQuantity());
            drugMapper.updateById(drug);
        }


        //不删方便查询
        /*LambdaQueryWrapper<MedicineOrder> medicineOrderLambdaQueryWrapper = new LambdaQueryWrapper<>();
        medicineOrderLambdaQueryWrapper.eq(MedicineOrder::getOrderPaymentRecordId, oId);
        medicineOrderMapper.delete(medicineOrderLambdaQueryWrapper);*/
        orderPaymentRecord.setPaymentStatus(2);
        orderPaymentRecordMapper.updateById(orderPaymentRecord);
        //后续执行退款处理
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void completeOrder(OrderPaymentRecord orderPaymentRecord,Long userId) {
        if (orderPaymentRecord == null) {
            throw new MedicineOrderException(ResultCodeEnum.DATA_ERROR);
        }

        Long id = orderPaymentRecord.getId();
        if (id == null) {
            throw new MedicineOrderException(ResultCodeEnum.DATA_ERROR);
        }
        OrderPaymentRecord order = orderPaymentRecordMapper.selectById(id);
        if (order == null) {
            throw new MedicineOrderException(ResultCodeEnum.DATA_ERROR);
        }

        if (order.getPayerId() != userId) {
            throw new MedicineOrderException(ResultCodeEnum.OPERATION_ERROR);
        }

        if (order.getPaymentStatus() != 0) {
            throw new MedicineOrderException(ResultCodeEnum.DATA_ERROR);
        }


        orderPaymentRecord.setPaymentStatus(1);
        orderPaymentRecord.setPaymentTime(new Date());
        orderPaymentRecordMapper.updateById(orderPaymentRecord);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void vertifyPayment(Long oId){

        OrderPaymentRecord orderPaymentRecord = orderPaymentRecordMapper.selectById(oId);
        if (orderPaymentRecord == null) {
            throw new MedicineOrderException(ResultCodeEnum.ORDER_NOT_EXIST);
        }
        //判断是否是当前用户操作
        if (orderPaymentRecord.getPayerId() != LoginUserHolder.getLoginUser().getUserId()) {
            throw new MedicineOrderException(ResultCodeEnum.OPERATION_ERROR);
        }
        //判断是否处于已付款状态
        if (orderPaymentRecord.getPaymentStatus() != 1) {
            throw new MedicineOrderException(ResultCodeEnum.ORDER_STATUS_ILLEGAL);
        }
        OrderPaymentRecord order = new OrderPaymentRecord();
        order.setId(orderPaymentRecord.getId());
        order.setPaymentStatus(3);//表示已核销
        orderPaymentRecordMapper.updateById(order);
    }


    @Override
    public IPage<OrderPaymentRecord> getOrders(Integer pageNum, Integer pageSize, Date createDate) {
        // 1. 创建分页对象
        IPage<OrderPaymentRecord> page = new Page<>(pageNum, pageSize);

        // 2. 构建基础查询条件（无论是否传时间都生效的条件）
        LambdaQueryWrapper<OrderPaymentRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderPaymentRecord::getPayerId, LoginUserHolder.getLoginUser().getUserId())  // 固定条件
                .orderByDesc(OrderPaymentRecord::getCreateTime);  // 固定排序

        // 3. 动态添加时间范围条件（仅当createDate不为null时才添加）
        if (createDate != null) {
            // 计算当天的起始和结束时间
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(createDate);

            // 当天00:00:00
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date startTime = calendar.getTime();

            // 当天23:59:59
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            Date endTime = calendar.getTime();

            // 添加时间范围条件
            queryWrapper.ge(OrderPaymentRecord::getCreateTime, startTime)
                    .le(OrderPaymentRecord::getCreateTime, endTime);
        }

        // 4. 执行分页查询
        return orderPaymentRecordMapper.selectPage(page, queryWrapper);
    }

    @Override
    public IPage<MedicineInfo> getDrugsByOrderId(Integer pageNum, Integer pageSize, Long orderId) {
        // 1. 初始化分页对象


        // 2. 根据订单ID查询关联的MedicineOrder列表
        LambdaQueryWrapper<MedicineOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MedicineOrder::getOrderPaymentRecordId, orderId);
        List<MedicineOrder> medicineOrders = medicineOrderMapper.selectList(queryWrapper);

        List<MedicineInfo> medicineInfoList = new ArrayList<>();
        for (MedicineOrder medicineOrder : medicineOrders) {
            MedicineInfo medicineInfo = new MedicineInfo();
            medicineInfo.setDrugId(medicineOrder.getDrugId());
            medicineInfo.setDrugQuantity(medicineOrder.getDrugQuantity());
            Drug drug = drugMapper.selectById(medicineOrder.getDrugId());
            medicineInfo.setGenericName(drug.getGenericName());
            medicineInfo.setAmount(drug.getDrugPrice());
            medicineInfoList.add(medicineInfo);
        }

        IPage<MedicineInfo> page = new Page<>(pageNum, pageSize);
        page.setRecords(medicineInfoList);
        return  page;
    }
}