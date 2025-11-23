package cn.edu.ncu.medical;

import cn.edu.ncu.medical.entity.Drug;
import cn.edu.ncu.medical.service.DrugService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class DrugTest {

    @Autowired
    private DrugService drugService;

    @Test
    public void getDrugList() {
        Page<Drug> page = new Page<>(1, 10);
        IPage<Drug> drugList = drugService.getDrugList(page);
        String search = "阿";
        IPage<Drug> drugListBySearch = drugService.getDrugListBySearch(page, search);
        System.out.println(drugList);
        System.out.println(drugListBySearch);
    }

    @Test
    public void getAllDrug() {
        List<Drug> allDrug = drugService.list();
        System.out.println(allDrug);
    }
}
