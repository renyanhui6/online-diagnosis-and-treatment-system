package cn.edu.ncu.medical.service.impl;

import cn.edu.ncu.medical.config.UploadConfig;
import cn.edu.ncu.medical.entity.Department;
import cn.edu.ncu.medical.entity.dto.SubDepartmentModel;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.mapper.DepartmentMapper;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.DepartmentService;
import cn.edu.ncu.medical.utils.UploadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.edu.ncu.medical.entity.SubDepartment;
import cn.edu.ncu.medical.service.SubDepartmentService;
import cn.edu.ncu.medical.mapper.SubDepartmentMapper;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
* @author star
* @description 针对表【sub_department】的数据库操作Service实现
* @createDate 2025-07-24 17:44:39
*/
@Service
public class SubDepartmentServiceImpl extends ServiceImpl<SubDepartmentMapper, SubDepartment>
    implements SubDepartmentService{
	@Autowired
	private SubDepartmentMapper subDepartmentMapper;
	@Autowired
	private DepartmentMapper departmentMapper;
	@Autowired
	private UploadConfig uploadConfig;
	@Override
	public void addSub(SubDepartmentModel subDepartmentModel) throws IOException {
		//首先保证父科室存在
		if (departmentMapper.selectById(subDepartmentModel.getParentDepartmentId())==null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		//其次保证子科室名称唯一
		if (subDepartmentMapper.selectOne(new LambdaQueryWrapper<SubDepartment>().eq(SubDepartment::getDepartmentName, subDepartmentModel.getDepartmentName()))!=null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}

		//先把图片上传获取文件路径判断文件是jpg,png或者gif
		List<String> suffix = Arrays.asList("jpg", "png", "gif");
		//然后判断文件是否为空
		MultipartFile imageFile = subDepartmentModel.getImageFile();
		if (imageFile==null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		if (!suffix.contains(imageFile.getOriginalFilename().substring(imageFile.getOriginalFilename().lastIndexOf(".")+1))) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		String path = UploadUtil.putPhoto(uploadConfig, imageFile.getInputStream(), UUID.randomUUID() + ":" + imageFile.getOriginalFilename());
		//最后把数据写入数据库
		SubDepartment subDepartment = new SubDepartment();
		subDepartment.setDepartmentName(subDepartmentModel.getDepartmentName());
		subDepartment.setParentDepartmentId(subDepartmentModel.getParentDepartmentId());
		subDepartment.setDescription(subDepartmentModel.getDescription());
		subDepartment.setTreatmentScope(subDepartmentModel.getTreatmentScope());
		subDepartment.setDepartmentFeatures(subDepartmentModel.getDepartmentFeatures());
		subDepartment.setImagePath(path);
		subDepartmentMapper.insert(subDepartment);
	}
}



