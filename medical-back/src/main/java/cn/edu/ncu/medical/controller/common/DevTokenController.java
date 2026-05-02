package cn.edu.ncu.medical.controller.common;

import cn.edu.ncu.medical.constant.RedisConstant;
import cn.edu.ncu.medical.entity.SystemUser;
import cn.edu.ncu.medical.mapper.SystemUserMapper;
import cn.edu.ncu.medical.result.Result;
import cn.edu.ncu.medical.utils.JwtUtil;
import cn.edu.ncu.medical.utils.RedisCache;
import cn.edu.ncu.medical.utils.SHA256Util;
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

    private static final int ADMIN_USER_TYPE = 3;
    private static final String LOCAL_ADMIN_EMAIL = "admin@local.test";
    private static final String LOCAL_ADMIN_PASSWORD = "123456";

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
            SystemUser user = findFirstEnabledUserByType(type);
            if (user != null) {
                return user;
            }
            if (type == ADMIN_USER_TYPE) {
                return ensureLocalAdminUser();
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

    private SystemUser findFirstEnabledUserByType(Integer type) {
        LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemUser::getType, type)
                .eq(SystemUser::getStatus, 1)
                .orderByAsc(SystemUser::getId)
                .last("limit 1");
        return systemUserMapper.selectOne(queryWrapper);
    }

    private SystemUser ensureLocalAdminUser() {
        LambdaQueryWrapper<SystemUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SystemUser::getType, ADMIN_USER_TYPE)
                .orderByAsc(SystemUser::getId)
                .last("limit 1");
        SystemUser adminUser = systemUserMapper.selectOne(queryWrapper);
        if (adminUser != null) {
            boolean changed = false;
            if (adminUser.getStatus() == null || adminUser.getStatus() != 1) {
                adminUser.setStatus(1);
                changed = true;
            }
            if (adminUser.getIsDeleted() != null && adminUser.getIsDeleted() != 0) {
                adminUser.setIsDeleted(0);
                changed = true;
            }
            if (changed) {
                systemUserMapper.updateById(adminUser);
            }
            return adminUser;
        }

        SystemUser localAdmin = new SystemUser();
        localAdmin.setUsername(buildLocalAdminUsername());
        localAdmin.setPassword(SHA256Util.encrypt(LOCAL_ADMIN_PASSWORD));
        localAdmin.setType(ADMIN_USER_TYPE);
        localAdmin.setEmail(LOCAL_ADMIN_EMAIL);
        localAdmin.setRegisterType(1);
        localAdmin.setStatus(1);
        localAdmin.setIsDeleted(0);
        systemUserMapper.insert(localAdmin);
        return localAdmin;
    }

    private String buildLocalAdminUsername() {
        return "local_admin_" + (System.currentTimeMillis() % 100000);
    }
}
