package cn.edu.ncu.medical.controller.doctor;

import cn.edu.ncu.medical.constant.RegistrationStatus;
import cn.edu.ncu.medical.entity.Drug;
import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.dto.RegistrationCondition;
import cn.edu.ncu.medical.entity.vo.RegistrationInfo;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.DoctorDetailService;
import cn.edu.ncu.medical.service.DrugService;
import cn.edu.ncu.medical.service.RegistrationService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/front/doctor/drug")
public class DoctorDrugController {

    @Autowired
    private DrugService drugService;


    /**
     * 查询药品列表,分页查询
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    @PostMapping("/getDrugList")
    public Result getDrugList(@RequestParam(defaultValue = "1") Integer pageNum,
                              @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Drug> page = new Page<>(pageNum, pageSize);
        return Result.ok(drugService.getDrugList(page));
    }

    /**
     * 获取所有药品，不需要分页
     * @return
     */
    @GetMapping("/getAllDrug")
    public Result getAllDrug() {
        return Result.ok(drugService.list());
    }


    /**
     * 根据搜索条件查询药品列表
     *
     * @param pageNum
     * @param pageSize
     * @param search
     * @return
     */
    @PostMapping("/searchDrugList")
    public Result searchDrugList(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                 @RequestParam(defaultValue = "") String search) {
        Page<Drug> page = new Page<>(pageNum, pageSize);

        return Result.ok(drugService.getDrugListBySearch(page, search));
    }
}
