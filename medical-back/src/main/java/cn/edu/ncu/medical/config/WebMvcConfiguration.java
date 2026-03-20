package cn.edu.ncu.medical.config;

import cn.edu.ncu.medical.inteceptor.AuthenticInterceptor;
import cn.edu.ncu.medical.inteceptor.DevLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Autowired(required = false)
    AuthenticInterceptor authenticInterceptor;

    @Autowired(required = false)
    DevLoginInterceptor devLoginInterceptor;

    @Value("${app.auth.enabled:true}")
    private boolean authEnabled;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (authEnabled) {
            if (authenticInterceptor == null) {
                return;
            }
            registry.addInterceptor(authenticInterceptor)
                    .addPathPatterns("/back/**", "/front/**", "/ai/**") // 拦截所有需要认证的路径
                    .excludePathPatterns(
                            "/front/patient/loginAndOut/register", // 注册接口
                            "/front/loginAndOut/captchaCode",
                            "/front/loginAndOut/login",
                            "/front/loginAndOut/logout",
                            "/front/loginAndOut/findPassword",
                            "/front/loginAndOut/getEmailCode",
                            "/front/loginAndOut/devToken"
                    );
            return;
        }

        if (devLoginInterceptor != null) {
            registry.addInterceptor(devLoginInterceptor).addPathPatterns("/**");
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 个人项目/本地开发：放开跨域，便于前后端分离调试（不做安全强化）
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

}
