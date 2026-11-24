package com.bookstore.backend.controller;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.UserLoginRequest;
import com.bookstore.backend.dto.UserRegisterRequest;
import com.bookstore.backend.service.UserService;
import com.bookstore.backend.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器
 * 处理用户相关的HTTP请求，包括注册、登录、登出、用户管理等
 * 安全特性：登录成功后将用户名存入Session，后续请求从Session获取用户身份
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private AuthUtil authUtil;

    /**
     * 用户注册
     * POST http://localhost:8080/api/v1/users/register
     * @param request 注册请求（用户名、密码、邮箱）
     * @return 注册结果
     */
    @PostMapping("/register")
    public ApiResponse register(@RequestBody UserRegisterRequest request) {
        log.info("Controller: register, username={}", request.getUsername());
        return userService.register(request);
    }

    /**
     * 用户登录
     * POST http://localhost:8080/api/v1/users/login
     * 登录成功后将用户名存入Session，建立用户会话
     * @param request 登录请求（用户名、密码）
     * @param session HTTP会话对象
     * @return 登录结果，包含用户信息
     */
    @PostMapping("/login")
    public ApiResponse login(@RequestBody UserLoginRequest request, HttpSession session) {
        log.info("Controller: login, username={}", request.getUsername());
        ApiResponse response = userService.login(request);
        
        // 登录成功后，将用户名存入Session（核心安全机制）
        if (response.getCode() == 200) {
            authUtil.setLoggedInUser(session, request.getUsername());
            log.info("User logged in and stored in session: {}", request.getUsername());
        }
        
        return response;
    }

    /**
     * 用户登出
     * POST http://localhost:8080/api/v1/users/logout
     * 从Session中移除用户信息，结束用户会话
     * @param session HTTP会话对象
     * @return 登出结果
     */
    @PostMapping("/logout")
    public ApiResponse logout(HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        authUtil.logout(session);
        log.info("User logged out: {}", username);
        return ApiResponse.success("登出成功");
    }

    /**
     * 获取当前登录用户信息
     * GET http://localhost:8080/api/v1/users/me
     * 从Session获取用户名，验证用户是否已登录
     * @param session HTTP会话对象
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public ApiResponse getCurrentUser(HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        log.info("Controller: getCurrentUser, username={}", username);
        return userService.getUser(username);
    }

    /**
     * 获取指定用户信息
     * GET http://localhost:8080/api/v1/users/{username}
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/{username}")
    public ApiResponse getUser(@PathVariable String username) {
        log.info("Controller: getUser, username={}", username);
        return userService.getUser(username);
    }

    /**
     * 获取所有用户列表（管理员功能）
     * GET http://localhost:8080/api/v1/users
     * 需要管理员权限验证
     * @param session HTTP会话对象
     * @return 用户列表
     */
    @GetMapping("")
    public ApiResponse getAllUsers(HttpSession session) {
        // 管理员权限验证
        ApiResponse authCheck = authUtil.checkAdmin(session);
        if (authCheck != null) {
            return authCheck;
        }
        log.info("Controller: getAllUsers");
        return userService.getAllUsers();
    }

    /**
     * 禁用/解禁用户（管理员功能）
     * PUT http://localhost:8080/api/v1/users/{id}/disabled?disabled=true
     * 需要管理员权限验证
     * @param id 用户ID
     * @param disabled 是否禁用
     * @param session HTTP会话对象
     * @return 操作结果
     */
    @PutMapping("/{id}/disabled")
    public ApiResponse toggleUserDisabled(@PathVariable Long id, @RequestParam Boolean disabled, HttpSession session) {
        // 管理员权限验证
        ApiResponse authCheck = authUtil.checkAdmin(session);
        if (authCheck != null) {
            return authCheck;
        }
        log.info("Controller: toggleUserDisabled, id={}, disabled={}", id, disabled);
        return userService.toggleUserDisabled(id, disabled);
    }
}