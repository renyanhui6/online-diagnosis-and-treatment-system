package cn.edu.ncu.medical.controller.admin;

import cn.edu.ncu.medical.entity.Drug;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.service.DrugService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/back/admin/drug")
public class DrugController {
    @Autowired
    private DrugService drugService;

    /**
     * 添加药品
     *
     * @param drug
     * @return
     */
    @PostMapping("/addDrug")
    public Result addDrug(@RequestBody Drug drug) {
        drugService.save(drug);
        return Result.ok();
    }

    /**
     * 删除药品
     *
     * @param id
     * @return
     */
    @PostMapping("/deleteDrug")
    public Result deleteDrug(@RequestParam("id") Long id) {
        drugService.removeById(id);
        return Result.ok();
    }

    /**
     * 修改药品
     *
     * @param drug
     * @return
     */
    @PostMapping("/modifyDrug")
    public Result modifyDrug(@RequestBody Drug drug) {
        drugService.updateById(drug);
        return Result.ok();
    }

    /**
     * 查询药品详情
     *
     * @param id
     * @return
     */
    @PostMapping("/getDrug")
    public Result getDrug(@RequestParam("id") Long id) {
        Drug drug = drugService.getById(id);
        return Result.ok(drug);
    }


    /**
     * 查询药品列表,分页查询
     * 药品数量0的不显示在列表中
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
