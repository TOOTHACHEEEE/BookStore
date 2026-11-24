package com.bookstore.backend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 密码加密工具类
 * 使用BCrypt算法对密码进行单向哈希加密
 * BCrypt特点：每次加密结果不同（加盐），安全性高，防止彩虹表攻击
 */
@Component
public class PasswordUtil {

    /** BCrypt加密器，强度默认10 */
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 对密码进行BCrypt加密
     * @param rawPassword 原始密码
     * @return 加密后的密码（格式：$2a$10$...）
     */
    public String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码是否匹配
     * @param rawPassword 用户输入的原始密码
     * @param encodedPassword 数据库中存储的加密密码
     * @return true表示密码匹配，false表示不匹配
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}