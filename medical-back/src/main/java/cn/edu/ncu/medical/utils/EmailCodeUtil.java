package cn.edu.ncu.medical.utils;

import cn.edu.ncu.medical.constant.RedisConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
public class EmailCodeUtil {

    private final JavaMailSender mailSender;
    private final RedisCache redisCache;

    // 邮件发送者（与配置中的username一致）
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    public EmailCodeUtil(JavaMailSender mailSender, RedisCache redisCache) {
        this.mailSender = mailSender;
        this.redisCache = redisCache;
    }

    /**
     * 发送验证码邮件
     * @param toEmail 收件邮箱
     * @return 是否发送成功
     */
    public boolean sendVerificationCode(String toEmail) {
        // 生成6位随机验证码
        String verificationCode = generateRandomCode();
        String redisKey = RedisConstant.CODE_PREFIX + toEmail;

        try {
            // 创建简单邮件消息
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("密码找回验证码");
            message.setText("您的验证码是: " + verificationCode + "，有效期为5分钟。请勿泄露此验证码。");

            // 发送邮件
            mailSender.send(message);

            // 存储验证码到Redis并设置过期时间
            redisCache.setString(redisKey, verificationCode)
                    .setExpire(redisKey, (int) RedisConstant.CODE_EXPIRE_SECONDS, TimeUnit.SECONDS);

            return true;
        } catch (MailException e) {
            // 邮件发送失败处理
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 验证邮箱验证码
     * @param email 邮箱地址
     * @param code 用户输入的验证码
     * @return 验证结果
     */
    public boolean verifyCode(String email, String code) {
        String redisKey = RedisConstant.CODE_PREFIX + email;
        String correctCode = redisCache.getString(redisKey);

        // 验证码存在且匹配
        if (correctCode != null && correctCode.equals(code)) {
            // 验证成功后删除Redis中的验证码
            redisCache.delete(redisKey);
            return true;
        }
        return false;
    }

    /**
     * 生成6位随机数字验证码
     */
    private String generateRandomCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(999999));
    }
}