package cn.edu.ncu.medical.controller.admin;

import cn.edu.ncu.medical.constant.RedisConstant;
import cn.edu.ncu.medical.entity.DoctorDetail;
import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.entity.vo.SystemUserPage;
import cn.edu.ncu.medical.exception.TypeException;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.DoctorDetailService;
import cn.edu.ncu.medical.service.SystemUserService;
import cn.edu.ncu.medical.utils.RedisCache;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/back/admin/doctor")
public class DoctorController {
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private DoctorDetailService doctorDetailService;

    @GetMapping("/findById")
    public Result findById(@RequestParam("id") Long id) {
        if(systemUserService.getById(id).getType()!=2) {
            throw new TypeException(ResultCodeEnum.BACK_ACCESS_TYPE_ERROR);
        }
        LambdaQueryWrapper<DoctorDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DoctorDetail::getSystemUserId, id);
        return Result.ok(doctorDetailService.getOne(queryWrapper));
    }

    @PostMapping("/findAll")
    public Result findAllDoctor(@RequestParam(value = "pageNum",required = false) Integer pageNum,
                          @RequestParam(value = "pageSize",required = false) Integer pageSize,
                          @RequestBody(required = false) SystemUserPage condition) {
        if (pageNum == null) {
            pageNum = 1;
        }
        if (pageSize == null) {
            pageSize = 10;
        }
        if (condition == null) {
            condition = new SystemUserPage();
        }
            condition.setType(2);
            IPage<SystemUserPage> page = new Page<>(pageNum, pageSize);
            return Result.ok(systemUserService.findAll(page, condition));
        }
    @DeleteMapping("/removeById")
    public Result removeById(@RequestParam("id") Long id) {
        if(systemUserService.getById(id).getType()!=2) {
           throw new TypeException(ResultCodeEnum.BACK_ACCESS_TYPE_ERROR);
        }
        LambdaQueryWrapper<DoctorDetail> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DoctorDetail::getSystemUserId,id);
        doctorDetailService.remove(queryWrapper);
        systemUserService.removeById(id);
        redisCache.delete(RedisConstant.LOGIN_TOKEN_PREFIX+ id);
        return Result.ok();
    }

    @PostMapping("/addAccount")
    public Result addAccount(@RequestBody SystemUser systemUser) {
        if(systemUser.getType()!=2) {
            throw new TypeException(ResultCodeEnum.BACK_ACCESS_TYPE_ERROR);
        }
        systemUserService.insert(systemUser);
        return Result.ok();
    }

    @PostMapping("/addDetail")
    public Result addDetail(@RequestBody DoctorDetail doctorDetail) {
        return Result.ok(doctorDetailService.save(doctorDetail));
    }
    @PutMapping("/modifyStatusById")
    public Result modifyStatus(@RequestParam("status") Integer status,@RequestParam("id") Long id) {
        if(systemUserService.getById(id).getType()!=2) {
            throw new TypeException(ResultCodeEnum.BACK_ACCESS_TYPE_ERROR);
        }
        SystemUser systemUser = new SystemUser();
        systemUser.setStatus(status);
        systemUser.setId(id);
        systemUserService.updateById(systemUser);
        return Result.ok();
    }
}
