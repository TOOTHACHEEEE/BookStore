package com.bookstore.backend.config;

import com.bookstore.backend.entity.User;
import com.bookstore.backend.repository.UserRepository;
import com.bookstore.backend.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 实现CommandLineRunner接口，在Spring Boot启动完成后自动执行
 * 负责创建默认的管理员账号和演示用户账号
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordUtil passwordUtil;

    /**
     * 启动时执行的初始化逻辑
     */
    @Override
    public void run(String... args) throws Exception {
        initAdminUser();
        initDemoUser();
    }

    /**
     * 初始化管理员账号
     * 如果admin用户不存在则创建，如果存在但密码为明文则自动升级为加密格式
     */
    private void initAdminUser() {
        User admin = userRepository.findByUsername("admin");
        if (admin == null) {
            admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordUtil.encode("admin123"));  // BCrypt加密
            admin.setEmail("admin@example.com");
            admin.setAvatar("https://via.placeholder.com/40");
            admin.setRole("admin");
            admin.setDisabled(false);
            userRepository.save(admin);
            log.info("管理员账号已创建: admin/admin123");
        } else {
            // 兼容旧的明文密码，自动升级为加密格式
            if (!admin.getPassword().startsWith("$2a$")) {
                admin.setPassword(passwordUtil.encode("admin123"));
                userRepository.save(admin);
                log.info("管理员账号密码已更新为加密格式");
            }
        }
    }

    /**
     * 初始化演示用户账号
     * 如果toothache用户不存在则创建，如果存在但密码为明文则自动升级为加密格式
     */
    private void initDemoUser() {
        User user = userRepository.findByUsername("toothache");
        if (user == null) {
            user = new User();
            user.setUsername("toothache");
            user.setPassword(passwordUtil.encode("123456"));  // BCrypt加密
            user.setEmail("toothache@example.com");
            user.setAvatar("https://via.placeholder.com/40");
            user.setRole("customer");
            user.setDisabled(false);
            userRepository.save(user);
            log.info("演示账号已创建: toothache/123456");
        } else {
            // 兼容旧的明文密码，自动升级为加密格式
            if (!user.getPassword().startsWith("$2a$")) {
                user.setPassword(passwordUtil.encode("123456"));
                userRepository.save(user);
                log.info("演示账号密码已更新为加密格式");
            }
        }
    }
}