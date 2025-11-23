package cn.edu.ncu.medical.entity.vo;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class MedicineInfo {
    private Long drugId;


    private String genericName;

    private Integer drugQuantity;

    private BigDecimal amount;

}
