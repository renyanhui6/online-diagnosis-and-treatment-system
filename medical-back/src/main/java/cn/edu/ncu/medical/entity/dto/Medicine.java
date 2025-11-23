package cn.edu.ncu.medical.entity.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class Medicine implements Serializable {
    private Long drugId;

    private Integer drugQuantity;
}
