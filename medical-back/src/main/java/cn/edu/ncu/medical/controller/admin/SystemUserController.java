package cn.edu.ncu.medical.controller.admin;

import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.SystemUserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("back/admin/systemUser")
public class SystemUserController {
	@Autowired
	private SystemUserService systemUserService;

	/**
	 * 修改用户状态
	 * @param systemUser
	 * @return
	 */
	@GetMapping("/modifyStatus")
	public Result modifyStatus(@RequestBody SystemUser systemUser) {
		return systemUserService.modifyStatus(systemUser);
	}


}
