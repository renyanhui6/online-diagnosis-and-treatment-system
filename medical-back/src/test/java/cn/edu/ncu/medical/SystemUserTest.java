package cn.edu.ncu.medical;

import cn.edu.ncu.medical.service.SystemUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SystemUserTest {
	@Autowired
	SystemUserService systemUserService;
	@Test
	public void testDelete() {
		systemUserService.removeById(3);
	}
}
