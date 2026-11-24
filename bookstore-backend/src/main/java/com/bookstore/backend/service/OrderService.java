package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.BuyNowRequest;
import com.bookstore.backend.dto.CheckoutRequest;

/**
 * 【Service接口】订单服务接口
 * 
 * 【接口与实现分离】
 * 此接口定义订单服务的方法签名，具体实现由OrderServiceImpl提供
 */
public interface OrderService {
    
    /**
     * 获取用户订单列表
     */
    ApiResponse getOrders(String username);
    
    /**
     * 购物车结算（下单）
     */
    ApiResponse checkout(CheckoutRequest request);
    
    /**
     * 立即购买
     */
    ApiResponse buyNow(BuyNowRequest request);
    
    /**
     * 搜索用户订单（时间范围+书籍名称）
     */
    ApiResponse searchUserOrders(String username, String startTime, String endTime, String bookTitle);
    
    /**
     * 获取所有订单（管理员功能）
     */
    ApiResponse getAllOrders();
    
    /**
     * 搜索所有订单（管理员功能）
     */
    ApiResponse searchAllOrders(String startTime, String endTime, String bookTitle);
}