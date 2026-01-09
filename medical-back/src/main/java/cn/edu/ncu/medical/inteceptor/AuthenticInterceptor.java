package cn.edu.ncu.medical.inteceptor;
import cn.edu.ncu.medical.constant.RedisConstant;
import cn.edu.ncu.medical.exception.LoginException;
import cn.edu.ncu.medical.inteceptor.login.LoginUser;
import cn.edu.ncu.medical.inteceptor.login.LoginUserHolder;
import cn.edu.ncu.medical.result.ResultCodeEnum;
import cn.edu.ncu.medical.utils.JwtUtil;
import cn.edu.ncu.medical.utils.RedisCache;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

@Component
public class AuthenticInterceptor implements HandlerInterceptor {
    @Autowired
    private RedisCache redisCache;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // CORS 预检请求直接放行，避免被鉴权拦截导致前端跨域失败
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        //获取token
        String token = request.getHeader("access-key");
        // 2. 验证并解析Token (已有逻辑)
        Claims claims = JwtUtil.parseToken(token);
        String s = claims.get("username", String.class);
        Long l = claims.get("userId", Long.class);
        // 3. 验证Redis中的Token一致性 (已有逻辑)
        String redisToken = redisCache.getString(RedisConstant.LOGIN_TOKEN_PREFIX + l);
        if (redisToken == null || !redisToken.equals(token)) {
            throw new LoginException(ResultCodeEnum.FRONT_LOGIN_AUTH);
        }
        LoginUserHolder.setLoginUser(new LoginUser(l, s));
        // 4. 刷新Redis有效期（每次请求都刷新）
        redisCache.setExpire(RedisConstant.LOGIN_TOKEN_PREFIX +l, RedisConstant.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);

        // 5. 检查是否需要刷新JWT Token
        if(JwtUtil.shouldRefreshToken(claims.getExpiration())) {
            // 生成新Token（有效期30分钟）
            String newToken = JwtUtil.createToken(l, s);

            // 更新Redis存储
            redisCache.setString(RedisConstant.LOGIN_TOKEN_PREFIX +l, newToken).setExpire(RedisConstant.LOGIN_TOKEN_PREFIX +l,RedisConstant.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);

            // 通过响应头返回新Token
            response.setHeader("new-access-key", newToken);
        }
        return true;
    }



    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        LoginUserHolder.clear();
    }
}
