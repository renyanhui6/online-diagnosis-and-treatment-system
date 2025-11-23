package cn.edu.ncu.medical.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdCard {
	private String realName;
	private String idCard;
}
