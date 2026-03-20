package cn.edu.ncu.medical.controller.admin;

import cn.edu.ncu.medical.constant.RedisConstant;
import cn.edu.ncu.medical.entity.PatientAttendant;
import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.entity.dto.SystemUserPatientDetail;
import cn.edu.ncu.medical.entity.vo.SystemUserPage;
import cn.edu.ncu.medical.exception.TypeException;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.service.PatientAttendantService;
import cn.edu.ncu.medical.service.SystemUserService;
import cn.edu.ncu.medical.utils.FormatValidator;
import cn.edu.ncu.medical.utils.RedisCache;
import cn.edu.ncu.medical.utils.SHA256Util;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/back/admin/patient")
public class PatientController {
    @Autowired
    private SystemUserService systemUserService;
    @Autowired
    private RedisCache redisCache;
    @Autowired
    private PatientAttendantService patientAttendantService;

    @GetMapping("/findById")
    public Result findById(@RequestParam("id") Long id) {
        if(systemUserService.getById(id).getType()!=1) {
            throw new TypeException(ResultCodeEnum.BACK_ACCESS_TYPE_ERROR);
        }
        LambdaQueryWrapper<PatientAttendant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PatientAttendant::getSystemUserId, id);
        queryWrapper.eq(PatientAttendant::getIsMaster, 1);
        return Result.ok(patientAttendantService.getOne(queryWrapper));
    }
    //条件分页查询
    @PostMapping("/findAll")
    public Result findAllPatient(@RequestParam(value = "pageNum",required = false) Integer pageNum,
                          @RequestParam(value = "pageSize",required = false) Integer pageSize,
                          @RequestBody(required = false) SystemUserPage condition) {
        if (pageNum ==null) {
            pageNum = 1;
        }
        if (pageSize==null) {
            pageSize = 10;
        }
        if(condition==null){
            condition=new SystemUserPage();
        }
        condition.setType(1);
        IPage<SystemUserPage>  page = new Page<>(pageNum, pageSize);
        return Result.ok(systemUserService.findAll(page,condition));
    }

    @DeleteMapping("/removeById")
    public Result removeById(@RequestParam("id") Long id, HttpServletRequest request) {
        if(systemUserService.getById(id).getType()!=1) {
            throw new TypeException(ResultCodeEnum.BACK_ACCESS_TYPE_ERROR);
        }
        LambdaQueryWrapper<PatientAttendant> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PatientAttendant::getSystemUserId,id);
        patientAttendantService.remove(queryWrapper);
        systemUserService.removeById(id);
        redisCache.delete(RedisConstant.LOGIN_TOKEN_PREFIX+ id);
        return Result.ok();
    }

    @PutMapping("/modifyStatusById")
    public Result modifyStatus(@RequestParam("status") Integer status,@RequestParam("id") Long id) {
        if(systemUserService.getById(id).getType()!=1) {
            throw new TypeException(ResultCodeEnum.BACK_ACCESS_TYPE_ERROR);
        }
        SystemUser systemUser = new SystemUser();
        systemUser.setStatus(status);
        systemUser.setId(id);
        systemUserService.updateById(systemUser);
        return Result.ok();
    }

    @PostMapping("/create")
    @Transactional(rollbackFor = Exception.class)
    public Result<Long> create(@RequestBody SystemUserPatientDetail payload) {
        if (payload == null) {
            throw new TypeException(ResultCodeEnum.PARAM_ERROR);
        }
        if (payload.getUsername() == null || payload.getPassword() == null || payload.getEmail() == null) {
            throw new TypeException(ResultCodeEnum.PARAM_ERROR);
        }
        if (!FormatValidator.isValidEmail(payload.getEmail()) || payload.getPassword().length() < 6) {
            throw new TypeException(ResultCodeEnum.PARAM_ERROR);
        }
        LambdaQueryWrapper<SystemUser> userQuery = new LambdaQueryWrapper<>();
        userQuery.eq(SystemUser::getUsername, payload.getUsername()).or().eq(SystemUser::getEmail, payload.getEmail());
        if (systemUserService.getOne(userQuery) != null) {
            throw new TypeException(ResultCodeEnum.FRONT_ACCOUNT_EXIST_ERROR);
        }

        SystemUser systemUser = new SystemUser();
        systemUser.setUsername(payload.getUsername());
        systemUser.setPassword(SHA256Util.encrypt(payload.getPassword()));
        systemUser.setType(1);
        systemUser.setEmail(payload.getEmail());
        systemUser.setRegisterType(payload.getRegisterType() == null ? 1 : payload.getRegisterType());
        systemUser.setStatus(payload.getStatus() == null ? 1 : payload.getStatus());
        systemUserService.save(systemUser);

        PatientAttendant attendant = new PatientAttendant();
        attendant.setSystemUserId(systemUser.getId());
        attendant.setNickname(payload.getNickname() == null ? payload.getUsername() : payload.getNickname());
        attendant.setRealName(payload.getRealName());
        attendant.setIdCard(payload.getIdCard());
        attendant.setGender(payload.getGender());
        attendant.setPhone(payload.getPhone());
        attendant.setHomeAddress(payload.getHomeAddress());
        attendant.setIsMaster(1);
        patientAttendantService.save(attendant);

        return Result.ok(systemUser.getId());
    }

    @PostMapping("/updateDetail")
    public Result updateDetail(@RequestBody PatientAttendant patientAttendant) {
        if (patientAttendant == null || patientAttendant.getId() == null) {
            throw new TypeException(ResultCodeEnum.PARAM_ERROR);
        }
        patientAttendantService.updateById(patientAttendant);
        return Result.ok();
    }
}
