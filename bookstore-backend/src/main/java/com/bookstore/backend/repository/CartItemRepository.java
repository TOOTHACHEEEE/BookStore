package com.bookstore.backend.repository;

import com.bookstore.backend.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    // 【自定义查询方法】Spring Data JPA根据方法名自动生成SQL
    // findByUsernameOrderByAddedTimeDesc → SELECT * FROM cart_item WHERE username = ? ORDER BY added_time DESC

    // 查询指定用户的购物车，按添加时间倒序排列
    List<CartItem> findByUsernameOrderByAddedTimeDesc(String username);

    // 查询指定用户是否有指定书籍在购物车（用于判断是否累加数量）
    // 使用JPA关联关系查询，直接传入Book对象
    CartItem findByUsernameAndBook(String username, com.bookstore.backend.entity.Book book);

    // 清空指定用户的购物车
    void deleteByUsername(String username);
}