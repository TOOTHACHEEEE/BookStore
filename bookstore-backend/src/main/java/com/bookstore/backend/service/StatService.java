package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;

/**
 * 统计服务接口
 * 
 * 功能：
 * - 热销榜：指定时间范围内各种书的销量情况，按销售量排序
 * - 消费榜：指定时间范围内每个用户的累计消费情况，按购书总金额排序
 * - 个人统计：指定时间范围内自己购买书籍的情况
 */
public interface StatService {
    
    /**
     * 热销榜（管理员功能）
     */
    ApiResponse getHotBooks(String startTime, String endTime);
    
    /**
     * 消费榜（管理员功能）
     */
    ApiResponse getTopUsers(String startTime, String endTime);
    
    /**
     * 个人统计（用户功能）
     */
    ApiResponse getPersonalStat(String username, String startTime, String endTime);
}