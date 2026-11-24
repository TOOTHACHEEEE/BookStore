package com.bookstore.backend.controller;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.BuyNowRequest;
import com.bookstore.backend.dto.CheckoutRequest;
import com.bookstore.backend.service.OrderService;
import com.bookstore.backend.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 【控制器层】订单控制器
 * 
 * 【安全改进】
 * 所有方法不再从前端获取username，而是从Session中获取当前登录用户。
 * 这样可以防止用户伪造其他用户的身份进行操作。
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService orderService;

    @Autowired
    private AuthUtil authUtil;

    // GET http://localhost:8080/api/v1/orders
    // 获取当前用户的订单列表
    @GetMapping
    public ApiResponse getOrders(HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        log.info("Controller: getOrders, username={}", username);
        return orderService.getOrders(username);
    }

    // GET http://localhost:8080/api/v1/orders/search
    // 搜索用户订单（时间范围+书籍名称）
    @GetMapping("/search")
    public ApiResponse searchUserOrders(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String bookTitle,
            HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        log.info("Controller: searchUserOrders, username={}, startTime={}, endTime={}, bookTitle={}", 
            username, startTime, endTime, bookTitle);
        return orderService.searchUserOrders(username, startTime, endTime, bookTitle);
    }

    @GetMapping("/all")
    public ApiResponse getAllOrders(HttpSession session) {
        ApiResponse authCheck = authUtil.checkAdmin(session);
        if (authCheck != null) {
            return authCheck;
        }
        log.info("Controller: getAllOrders (admin)");
        return orderService.getAllOrders();
    }

    @GetMapping("/searchAll")
    public ApiResponse searchAllOrders(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) String bookTitle,
            HttpSession session) {
        ApiResponse authCheck = authUtil.checkAdmin(session);
        if (authCheck != null) {
            return authCheck;
        }
        log.info("Controller: searchAllOrders (admin), startTime={}, endTime={}, bookTitle={}", 
            startTime, endTime, bookTitle);
        return orderService.searchAllOrders(startTime, endTime, bookTitle);
    }

    // POST http://localhost:8080/api/v1/orders/checkout
    // 购物车结算（下单）
    @PostMapping("/checkout")
    public ApiResponse checkout(@RequestBody CheckoutRequest request, HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        log.info("Controller: checkout, username={}", username);
        
        // 手动设置用户名到请求对象中（由后端从Session获取，不再信任前端）
        request.setUsername(username);
        
        return orderService.checkout(request);
    }

    // POST http://localhost:8080/api/v1/orders/buyNow
    // 立即购买（不经过购物车）
    @PostMapping("/buyNow")
    public ApiResponse buyNow(@RequestBody BuyNowRequest request, HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        log.info("Controller: buyNow, username={}, bookId={}", username, request.getBookId());
        
        // 手动设置用户名到请求对象中（由后端从Session获取，不再信任前端）
        request.setUsername(username);
        
        return orderService.buyNow(request);
    }
}