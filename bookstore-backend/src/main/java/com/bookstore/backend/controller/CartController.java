package com.bookstore.backend.controller;

import com.bookstore.backend.dto.AddToCartRequest;
import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.service.CartService;
import com.bookstore.backend.util.AuthUtil;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 【控制器层】购物车控制器
 * 
 * 【安全改进】
 * 所有方法不再从前端获取username，而是从Session中获取当前登录用户。
 * 这样可以防止用户伪造其他用户的身份进行操作。
 */
@RestController
@RequestMapping("/api/v1/cart")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthUtil authUtil;

    // GET http://localhost:8080/api/v1/cart
    // 获取当前用户的购物车
    @GetMapping
    public ApiResponse getCart(HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        log.info("Controller: getCart, username={}", username);
        return cartService.getCart(username);
    }

    // POST http://localhost:8080/api/v1/cart
    // 添加商品到购物车
    @PostMapping
    public ApiResponse addToCart(@RequestBody AddToCartRequest request, HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        log.info("Controller: addToCart, username={}, bookId={}", username, request.getBookId());
        
        // 手动设置用户名到请求对象中（由后端从Session获取，不再信任前端）
        request.setUsername(username);
        
        return cartService.addToCart(request);
    }

    // DELETE http://localhost:8080/api/v1/cart/{cartItemId}
    // 从购物车移除商品
    @DeleteMapping("/{cartItemId}")
    public ApiResponse removeFromCart(@PathVariable Long cartItemId, HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        log.info("Controller: removeFromCart, username={}, cartItemId={}", username, cartItemId);
        return cartService.removeFromCart(cartItemId, username);
    }

    // DELETE http://localhost:8080/api/v1/cart
    // 清空购物车
    @DeleteMapping
    public ApiResponse clearCart(HttpSession session) {
        String username = authUtil.getCurrentUsername(session);
        log.info("Controller: clearCart, username={}", username);
        return cartService.clearCart(username);
    }
}