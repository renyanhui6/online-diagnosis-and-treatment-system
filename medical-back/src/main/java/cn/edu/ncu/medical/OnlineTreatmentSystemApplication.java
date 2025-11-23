package cn.edu.ncu.medical;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("cn.edu.ncu.medical.mapper")
@EnableScheduling
public class OnlineTreatmentSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnlineTreatmentSystemApplication.class, args);
	}

}
