package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.UserLoginRequest;
import com.bookstore.backend.dto.UserRegisterRequest;
import com.bookstore.backend.entity.User;
import com.bookstore.backend.repository.UserRepository;
import com.bookstore.backend.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户服务实现类
 * 处理用户注册、登录、查询、禁用/解禁等业务逻辑
 * 安全特性：密码使用BCrypt加密存储，支持明文密码兼容升级
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordUtil passwordUtil;

    /**
     * 用户注册
     * @param request 注册请求（用户名、密码、邮箱）
     * @return 注册结果
     */
    @Override
    public ApiResponse register(UserRegisterRequest request) {
        log.info("Registering user: {}", request.getUsername());

        // 参数校验
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            return ApiResponse.error("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return ApiResponse.error("密码不能为空");
        }
        if (request.getEmail() == null || request.getEmail().isEmpty()) {
            return ApiResponse.error("邮箱不能为空");
        }

        // 验证邮箱格式
        String emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
        if (!request.getEmail().matches(emailRegex)) {
            return ApiResponse.error("邮箱格式不正确");
        }

        // 检查用户名是否已存在
        User existingUser = userRepository.findByUsername(request.getUsername());
        if (existingUser != null) {
            return ApiResponse.error("用户名已存在");
        }

        // 创建新用户，密码使用BCrypt加密
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordUtil.encode(request.getPassword()));  // 密码加密存储
        user.setEmail(request.getEmail());
        user.setRole("customer");
        user.setDisabled(false);
        user.setAvatar("https://via.placeholder.com/40");
        user.setCreatedAt(LocalDateTime.now());

        try {
            userRepository.save(user);
            log.info("User registered successfully: {}", user.getUsername());
            return ApiResponse.success("注册成功");
        } catch (Exception e) {
            log.error("Register failed", e);
            return ApiResponse.error("注册失败");
        }
    }

    /**
     * 用户登录
     * 支持自动注册新用户，兼容旧的明文密码（登录时自动升级为加密格式）
     * @param request 登录请求（用户名、密码）
     * @return 登录结果，包含用户信息
     */
    @Override
    public ApiResponse login(UserLoginRequest request) {
        log.info("Login attempt for user: {}", request.getUsername());

        // 参数校验
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            return ApiResponse.error("用户名不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            return ApiResponse.error("密码不能为空");
        }

        User user = userRepository.findByUsername(request.getUsername());

       
        if (user == null) {
            user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordUtil.encode(request.getPassword()));  // 密码加密存储
            user.setRole("customer");
            user.setDisabled(false);
            user.setAvatar("https://via.placeholder.com/40");
            user.setCreatedAt(LocalDateTime.now());
            try {
                user = userRepository.save(user);
                log.info("Auto-registered new user: {}", user.getUsername());
            } catch (Exception e) {
                log.error("Auto register failed", e);
                return ApiResponse.error("登录失败");
            }
        } else {
            // 检查账号是否被禁用
            if (user.getDisabled() != null && user.getDisabled()) {
                return ApiResponse.error("您的账号已经被禁用");
            }
            
            // 密码验证：兼容BCrypt加密密码和旧的明文密码
            String storedPassword = user.getPassword();
            boolean passwordMatch = false;
            
            // 判断密码是否为BCrypt加密格式（以$2a$开头）
            if (storedPassword.startsWith("$2a$")) {
                // BCrypt加密密码，使用matches方法验证
                passwordMatch = passwordUtil.matches(request.getPassword(), storedPassword);
            } else {
                // 旧的明文密码，先进行明文比对
                passwordMatch = storedPassword.equals(request.getPassword());
                // 如果比对成功，自动升级为加密格式
                if (passwordMatch) {
                    user.setPassword(passwordUtil.encode(request.getPassword()));
                    userRepository.save(user);
                    log.info("Auto-upgraded password for user: {}", user.getUsername());
                }
            }
            
            if (!passwordMatch) {
                return ApiResponse.error("密码错误");
            }
        }

        // 返回用户信息（不包含密码）
        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getUsername());
        data.put("email", user.getEmail());
        data.put("avatar", user.getAvatar() == null ? "https://via.placeholder.com/40" : user.getAvatar());
        data.put("role", user.getRole());  // 返回角色信息，前端根据角色显示不同界面
        return ApiResponse.success(data);
    }

    /**
     * 获取单个用户信息
     * @param username 用户名
     * @return 用户信息
     */
    @Override
    public ApiResponse getUser(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return new ApiResponse(404, "用户不存在", null);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getUsername());
        data.put("email", user.getEmail());
        data.put("avatar", user.getAvatar() == null ? "https://via.placeholder.com/40" : user.getAvatar());
        data.put("role", user.getRole());
        data.put("disabled", user.getDisabled());
        return ApiResponse.success(data);
    }

    /**
     * 获取所有用户列表（管理员功能）
     * @return 用户列表
     */
    @Override
    public ApiResponse getAllUsers() {
        log.info("Querying all users");
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userViews = new ArrayList<>();
        
        for (User user : users) {
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("name", user.getUsername());
            data.put("email", user.getEmail());
            data.put("avatar", user.getAvatar() == null ? "https://via.placeholder.com/40" : user.getAvatar());
            data.put("role", user.getRole());
            data.put("disabled", user.getDisabled());
            userViews.add(data);
        }
        
        return ApiResponse.success(userViews);
    }

    /**
     * 禁用/解禁用户（管理员功能）
     * 注意：不能禁用管理员账号
     * @param id 用户ID
     * @param disabled 是否禁用
     * @return 操作结果
     */
    @Override
    public ApiResponse toggleUserDisabled(Long id, Boolean disabled) {
        log.info("Toggling user disabled: id={}, disabled={}", id, disabled);
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return new ApiResponse(404, "用户不存在", null);
        }
        
        User user = userOpt.get();
        // 安全检查：不能禁用管理员账号
        if ("admin".equals(user.getRole())) {
            return ApiResponse.error("不能禁用管理员账号");
        }
        
        user.setDisabled(disabled);
        userRepository.save(user);
        
        return ApiResponse.success(disabled ? "用户已禁用" : "用户已解禁");
    }
}