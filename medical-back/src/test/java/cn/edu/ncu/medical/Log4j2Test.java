package cn.edu.ncu.medical;//package cn.edu.ncu.demo01;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class Log4j2Test {
    // 修改 Logger 导入和获取方式
    private static final Logger logger = LogManager.getLogger(Log4j2Test.class);

    @Test
    public void contextLoads() {
        logger.fatal("致命错误1");
        logger.error("严重警告2");
        logger.warn("警告3");
        logger.info("普通信息4");
        logger.debug("调试信息5");
    }
}

