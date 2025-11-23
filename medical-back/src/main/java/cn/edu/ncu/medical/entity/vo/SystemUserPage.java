package cn.edu.ncu.medical.entity.vo;

import lombok.Data;

import java.util.Date;
@Data
public class SystemUserPage {
    private Long tempId;
    private Long id;
    private String username;
    private String password;
    private Integer type;
    private Date createTime;
    private String email;
    private Date updateTime;
    private Integer status;

}
