package com.bookstore.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "avatar", length = 500)
    private String avatar;

    /**
     * 【用户角色】区分管理员和普通用户
     * admin: 管理员，可以管理用户、书籍、订单、查看统计
     * customer: 顾客，只能浏览书籍、购买、查看自己的订单
     */
    @Column(name = "role", nullable = false, length = 20)
    private String role = "customer";  // 默认为顾客

    /**
     * 【禁用状态】管理员可以禁用用户
     * true: 已禁用，无法登录
     * false: 正常，可以登录
     */
    @Column(name = "disabled", nullable = false)
    private Boolean disabled = false;  // 默认未禁用

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public User() {
    }

    public User(Long id, String username, String password, String email, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.createdAt = createdAt;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public String toString() {
        return String.format(
                "User[id=%d, username='%s', email='%s', role='%s', disabled=%s]",
                id, username, email, role, disabled);
    }
}
