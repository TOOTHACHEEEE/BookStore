package com.bookstore.backend.dto;

/**
 * 加入购物车请求体
 * 
 * 【安全改进】
 * 注意：移除了username字段！
 * 用户名不再从前端传递，而是由后端从Session中获取。
 * 这样可以防止用户伪造其他用户的身份进行操作。
 */
public class AddToCartRequest {
    // 注意：移除了 username 字段
    // 后端通过 Session 获取当前登录用户，不信任前端传递的用户名
    
    private Long bookId;
    private Integer quantity;
    private String username;  // 由后端从Session获取后设置，不从前端接收

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}