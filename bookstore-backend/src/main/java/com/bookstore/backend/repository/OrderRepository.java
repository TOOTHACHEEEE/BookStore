package com.bookstore.backend.repository;

import com.bookstore.backend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUsernameOrderByOrderTimeDesc(String username);
    
    // 查询所有订单（管理员功能）
    List<Order> findAllByOrderByOrderTimeDesc();
    
    // 按时间范围查询用户订单
    @Query("SELECT o FROM Order o WHERE o.username = :username AND o.orderTime BETWEEN :startTime AND :endTime ORDER BY o.orderTime DESC")
    List<Order> findByUsernameAndTimeRange(@Param("username") String username, 
                                           @Param("startTime") LocalDateTime startTime, 
                                           @Param("endTime") LocalDateTime endTime);
    
    // 按时间范围查询所有订单（管理员功能）
    @Query("SELECT o FROM Order o WHERE o.orderTime BETWEEN :startTime AND :endTime ORDER BY o.orderTime DESC")
    List<Order> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                @Param("endTime") LocalDateTime endTime);
}
