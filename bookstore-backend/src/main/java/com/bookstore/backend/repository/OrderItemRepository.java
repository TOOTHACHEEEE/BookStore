package com.bookstore.backend.repository;

import com.bookstore.backend.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
    
    // 按书籍名称模糊查询订单项
    List<OrderItem> findByTitleContainingIgnoreCase(String title);
    
    // 查询某订单中是否包含指定书籍名称
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.title LIKE %:title%")
    List<OrderItem> findByOrderIdAndBookTitle(@Param("orderId") Long orderId, @Param("title") String title);
}
