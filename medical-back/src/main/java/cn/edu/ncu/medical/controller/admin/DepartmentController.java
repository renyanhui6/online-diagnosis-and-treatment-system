package cn.edu.ncu.medical.controller.admin;

import cn.edu.ncu.medical.entity.Department;
import cn.edu.ncu.medical.entity.SubDepartment;
import cn.edu.ncu.medical.entity.dto.SubDepartmentModel;
import cn.edu.ncu.medical.exception.MyRuntimeException;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.DepartmentService;
import cn.edu.ncu.medical.service.SubDepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("back/admin/department")
public class DepartmentController {
	@Autowired
	private DepartmentService departmentService;
	@Autowired
	private SubDepartmentService subDepartmentService;
	/**
	 * 查询所有科室
	 * @return
	 */
	@GetMapping("/findList")
	public Result findList(){
		return Result.ok(departmentService.list());
	}
	/**
	 * 查询所有子科室
	 * @param departmentId
	 * @return
	 */
	@GetMapping("/findSubList")
	public Result findSubList(@RequestParam("departmentId") Long departmentId){
		LambdaQueryWrapper<SubDepartment> queryWrapper = new LambdaQueryWrapper<SubDepartment>().eq(SubDepartment::getParentDepartmentId, departmentId);
		return Result.ok(subDepartmentService.list(queryWrapper));
	}
	@PostMapping("/add")
	public Result add(@RequestBody Department department){
		if (department==null){
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		//保证科室名称唯一
		if (departmentService.getOne(new LambdaQueryWrapper<Department>().eq(Department::getDepartmentName, department.getDepartmentName()))!=null) {
			throw new MyRuntimeException(ResultCodeEnum.PARAM_ERROR);
		}
		return Result.ok(departmentService.save(department));
	}
	@PostMapping("/addSub")
	public Result addSub(@ModelAttribute SubDepartmentModel subDepartmentModel) throws IOException {
		subDepartmentService.addSub(subDepartmentModel);
		return Result.ok();
	}
	@GetMapping("/remove")
	public Result remove(@RequestParam("departmentId") Long departmentId){
		//判断是否有子科室
		LambdaQueryWrapper<SubDepartment> queryWrapper = new LambdaQueryWrapper<SubDepartment>();
		if (!subDepartmentService.list(queryWrapper.eq(SubDepartment::getParentDepartmentId, departmentId)).isEmpty()) {
			throw new MyRuntimeException(ResultCodeEnum.DELETE_ERROR);
		}
		//直接删
		departmentService.removeById(departmentId);
		return Result.ok();
	}
	@GetMapping("/removeSub")
	public Result removeSub(@RequestParam("subDepartmentId") Long subDepartmentId){
		//直接删
		subDepartmentService.removeById(subDepartmentId);
		return Result.ok();
	}
}
