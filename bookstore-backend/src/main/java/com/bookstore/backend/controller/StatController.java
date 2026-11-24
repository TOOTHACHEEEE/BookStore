package com.bookstore.backend.controller;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.service.StatService;
import com.bookstore.backend.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 【控制器层】统计控制器
 * 
 * 功能：
 * - 热销榜（管理员功能）
 * - 消费榜（管理员功能）
 * - 个人统计（用户功能）
 * 
 * 【安全改进】
 * 个人统计方法不再从前端获取username，而是从Session中获取当前登录用户。
 */
@RestController
@RequestMapping("/api/v1/stat")
public class StatController {

    private static final Logger log = LoggerFactory.getLogger(StatController.class);

    @Autowired
    private StatService statService;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping("/hotBooks")
    public ApiResponse getHotBooks(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpSession session) {
        ApiResponse authCheck = authUtil.checkAdmin(session);
        if (authCheck != null) {
            return authCheck;
        }
        log.info("Controller: getHotBooks, startTime={}, endTime={}", startTime, endTime);
        return statService.getHotBooks(startTime, endTime);
    }

    @GetMapping("/topUsers")
    public ApiResponse getTopUsers(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpSession session) {
        ApiResponse authCheck = authUtil.checkAdmin(session);
        if (authCheck != null) {
            return authCheck;
        }
        log.info("Controller: getTopUsers, startTime={}, endTime={}", startTime, endTime);
        return statService.getTopUsers(startTime, endTime);
    }

    // GET http://localhost:8080/api/v1/stat/personal
    // 个人统计（用户功能）
    @GetMapping("/personal")
    public ApiResponse getPersonalStat(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        log.info("Controller: getPersonalStat, username={}, startTime={}, endTime={}", username, startTime, endTime);
        return statService.getPersonalStat(username, startTime, endTime);
    }
}