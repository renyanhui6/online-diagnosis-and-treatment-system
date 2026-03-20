package cn.edu.ncu.medical.controller.common;

import cn.edu.ncu.medical.constant.RedisConstant;
import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.mapper.SystemUserMapper;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.utils.JwtUtil;
import cn.edu.ncu.medical.utils.RedisCache;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Local-only dev token endpoint to bypass captcha during integration.
 */
@RestController
@RequestMapping("/front/loginAndOut")
@Profile("local")
@ConditionalOnProperty(prefix = "app.auth", name = "dev-token-enabled", havingValue = "true")
public class DevTokenController {

    @Value("${app.auth.dev-user-id:1}")
    private Long devUserId;

    @Value("${app.auth.dev-username:dev}")
    private String devUsername;

    @Autowired
    private SystemUserMapper systemUserMapper;

    @Autowired
    private RedisCache redisCache;

    @GetMapping("/devToken")
    public Result devToken(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "type", required = false) Integer type
    ) {
        SystemUser user = resolveUser(userId, username, type);
        if (user == null) {
            return Result.fail(404, "用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.fail(403, "用户已禁用");
        }
        String token = JwtUtil.createToken(user.getId(), user.getUsername());
        String redisKey = RedisConstant.LOGIN_TOKEN_PREFIX + user.getId();
        redisCache.setString(redisKey, token).setExpire(redisKey, RedisConstant.LOGIN_TOKEN_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    private SystemUser resolveUser(Long userId, String username, Integer type) {
        if (userId != null && userId > 0) {
            return systemUserMapper.selectById(userId);
        }
        if (StringUtils.hasText(username)) {
            LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SystemUser::getUsername, username);
            return systemUserMapper.selectOne(queryWrapper);
        }
        if (type != null && type > 0) {
            LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SystemUser::getType, type)
                    .eq(SystemUser::getStatus, 1)
                    .orderByAsc(SystemUser::getId)
                    .last("limit 1");
            SystemUser user = systemUserMapper.selectOne(queryWrapper);
            if (user != null) {
                return user;
            }
        }
        if (devUserId != null && devUserId > 0) {
            SystemUser user = systemUserMapper.selectById(devUserId);
            if (user != null) {
                return user;
            }
        }
        if (StringUtils.hasText(devUsername)) {
            LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SystemUser::getUsername, devUsername);
            return systemUserMapper.selectOne(queryWrapper);
        }
        return null;
    }
}
