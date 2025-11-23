package cn.edu.ncu.medical.config;

import cn.edu.ncu.medical.inteceptor.AuthenticInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired
    AuthenticInterceptor authenticInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticInterceptor)
                .addPathPatterns("/back/**", "/front/**") // 拦截所有需要认证的路径
                .excludePathPatterns(
                        "/front/patient/loginAndOut/register", // 注册接口
                        "/front/loginAndOut/captchaCode",
                        "/front/loginAndOut/login",
                        "/front/loginAndOut/logout",
                        "/front/loginAndOut/findPassword",
                        "/front/loginAndOut/getEmailCode"
                );
    }

}
