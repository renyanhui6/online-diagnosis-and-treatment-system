package cn.edu.ncu.medical.inteceptor;

import cn.edu.ncu.medical.inteceptor.login.LoginUser;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 本地开发用：当关闭鉴权时，注入一个固定登录态，避免大量接口因 LoginUserHolder 为空而 NPE。
 */
@Component
@ConditionalOnProperty(prefix = "app.auth", name = "enabled", havingValue = "false")
public class DevLoginInterceptor implements HandlerInterceptor {

    @Value("${app.auth.dev-user-id:1}")
    private Long devUserId;

    @Value("${app.auth.dev-username:dev}")
    private String devUsername;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        LoginUserHolder.setLoginUser(new LoginUser(devUserId, devUsername));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LoginUserHolder.clear();
    }
}

