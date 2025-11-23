package cn.edu.ncu.medical;

import cn.edu.ncu.medical.entity.Registration;
import cn.edu.ncu.medical.entity.dto.RegistrationCondition;
import cn.edu.ncu.medical.entity.vo.RegistrationInfo;
import cn.edu.ncu.medical.mapper.RegistrationMapper;
import cn.edu.ncu.medical.mapper.ScheduleMapper;
import cn.edu.ncu.medical.service.RegistrationService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.xml.ignore=true")
public class RegistrationTest {
    @Autowired
    private RegistrationService registrationService;
    @Autowired
    private RegistrationMapper registrationMapper;
    @Autowired
    private ScheduleMapper scheduleMapper;


    @Test
    public void getRegistrationList() {
        Long doctorId = 131L;
        Integer pageNum = 1;
        Integer pageSize = 10;
        Page<RegistrationInfo> page = new Page<>(pageNum,pageSize);
//        IPage<Registration> page1 = registrationService.getSuspendedRegistrationList(doctorId,page);
        RegistrationCondition registrationCondition = new RegistrationCondition();
//        registrationCondition.setRegistrationStatus(5);.


        IPage<RegistrationInfo> page1 = registrationService.getRegistrationList(doctorId,page,registrationCondition);
        System.out.println(page1);
//        IPage<RegistrationInfo> page2 = registrationService.getRegistrationList(doctorId,page,scheduleId,registrationCondition);
//        System.out.println(page2);
//        System.out.println(page1);
    }


    @Test
    public void getRegistrationInfoList(){
        Long userId = 3L;
        Integer pageNum = 1;
        Integer pageSize = 10;
        Page<RegistrationInfo> page = new Page<>(pageNum,pageSize);
        RegistrationCondition registrationCondition = new RegistrationCondition();
//        registrationCondition.setRegistrationStatus(5);
        IPage<RegistrationInfo> pageInfo = registrationService.getRegistrationInfoList(userId,page,registrationCondition);
        System.out.println(pageInfo);
    }

    @Test
    public void getAllRegistrationInfo(){
        Long doctorId = 132L;
        Integer pageNum = 1;
        Integer pageSize = 10;
        Page<RegistrationInfo> page = new Page<>(pageNum,pageSize);
        RegistrationCondition registrationCondition = new RegistrationCondition();
//        registrationCondition.setRegistrationStatus(5);
        IPage<RegistrationInfo> pageInfo = registrationService.getAllRegistrationList(doctorId,page,registrationCondition);
        System.out.println(pageInfo);
    }
}
