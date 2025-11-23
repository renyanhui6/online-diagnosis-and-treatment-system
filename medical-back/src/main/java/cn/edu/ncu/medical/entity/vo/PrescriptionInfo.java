package cn.edu.ncu.medical.entity.vo;

import lombok.Data;

@Data
public class PrescriptionInfo {
    private Integer id;
    private Double price;
    private String drugName;
    private Integer drugQuantity;
    private String minimumSalesUnit;
    // 处方状态 0 未处方 1 已处方
    private String isPrescription;

}
