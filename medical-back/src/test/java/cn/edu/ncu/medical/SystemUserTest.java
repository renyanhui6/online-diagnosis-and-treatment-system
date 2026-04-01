package cn.edu.ncu.medical;

import cn.edu.ncu.medical.service.SystemUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SystemUserTest {
	@Autowired
	SystemUserService systemUserService;
	@Test
	public void getSystemUserById() {
		assertNotNull(systemUserService.getById(3L), "测试账号 userId=3 应存在");
	}
}
