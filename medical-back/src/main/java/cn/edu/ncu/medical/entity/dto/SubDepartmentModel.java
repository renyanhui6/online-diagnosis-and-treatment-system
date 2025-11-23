package cn.edu.ncu.medical.entity.dto;

import com.baomidou.mybatisplus.annotation.*;
import jakarta.mail.Multipart;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

/**
 * 添加文件
 */
@Data
public class SubDepartmentModel {
	@NotNull
	private Long parentDepartmentId;

	private String departmentName;

	private String description;

	private String treatmentScope;

	private String departmentFeatures;

	private MultipartFile imageFile;

}
