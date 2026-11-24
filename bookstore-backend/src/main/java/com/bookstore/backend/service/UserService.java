package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.UserLoginRequest;
import com.bookstore.backend.dto.UserRegisterRequest;

/**
 * 【Service接口】用户服务接口
 * 
 * 【接口与实现分离】
 * 此接口定义用户服务的方法签名，具体实现由UserServiceImpl提供
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    ApiResponse register(UserRegisterRequest request);
    
    /**
     * 用户登录
     */
    ApiResponse login(UserLoginRequest request);
    
    /**
     * 获取用户信息
     */
    ApiResponse getUser(String username);
    
    /**
     * 获取所有用户列表（管理员功能）
     */
    ApiResponse getAllUsers();
    
    /**
     * 禁用/解禁用户（管理员功能）
     */
    ApiResponse toggleUserDisabled(Long id, Boolean disabled);
}