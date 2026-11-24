package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.AddToCartRequest;

/**
 * 【Service接口】购物车服务接口
 * 
 * 【接口与实现分离】
 * 此接口定义购物车服务的方法签名，具体实现由CartServiceImpl提供
 */
public interface CartService {
    
    /**
     * 获取用户购物车列表
     */
    ApiResponse getCart(String username);
    
    /**
     * 添加商品到购物车
     */
    ApiResponse addToCart(AddToCartRequest request);
    
    /**
     * 删除购物车中的商品（需要验证用户名，防止删除其他用户的购物车项）
     */
    ApiResponse removeFromCart(Long id, String username);
    
    /**
     * 清空购物车
     */
    ApiResponse clearCart(String username);
}