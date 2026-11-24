package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.StatView;
import com.bookstore.backend.entity.Order;
import com.bookstore.backend.entity.OrderItem;
import com.bookstore.backend.repository.OrderItemRepository;
import com.bookstore.backend.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatServiceImpl implements StatService {

    private static final Logger log = LoggerFactory.getLogger(StatServiceImpl.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Override
    public ApiResponse getHotBooks(String startTime, String endTime) {
        log.info("Calculating hot books stat, startTime={}, endTime={}", startTime, endTime);
        
        List<Order> orders = getOrdersByTimeRange(startTime, endTime);
        
        // 按书籍ID统计销量
        Map<Long, StatView.HotBook> bookStatMap = new HashMap<>();
        
        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                Long bookId = item.getBookId();
                StatView.HotBook stat = bookStatMap.getOrDefault(bookId, new StatView.HotBook());
                stat.setBookId(bookId);
                stat.setTitle(item.getTitle());
                stat.setAuthor(item.getAuthor());
                stat.setTotalQuantity(stat.getTotalQuantity() == null ? item.getQuantity() : stat.getTotalQuantity() + item.getQuantity());
                BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                stat.setTotalAmount(stat.getTotalAmount() == null ? subtotal : stat.getTotalAmount().add(subtotal));
                bookStatMap.put(bookId, stat);
            }
        }
        
        // 按销量排序
        List<StatView.HotBook> hotBooks = new ArrayList<>(bookStatMap.values());
        hotBooks.sort(Comparator.comparing(StatView.HotBook::getTotalQuantity).reversed());
        
        return ApiResponse.success(hotBooks);
    }

    @Override
    public ApiResponse getTopUsers(String startTime, String endTime) {
        log.info("Calculating top users stat, startTime={}, endTime={}", startTime, endTime);
        
        List<Order> orders = getOrdersByTimeRange(startTime, endTime);
        
        // 按用户统计消费
        Map<String, StatView.TopUser> userStatMap = new HashMap<>();
        
        for (Order order : orders) {
            String username = order.getUsername();
            StatView.TopUser stat = userStatMap.getOrDefault(username, new StatView.TopUser());
            stat.setUsername(username);
            
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            int totalBooks = items.stream().mapToInt(OrderItem::getQuantity).sum();
            stat.setTotalBooks(stat.getTotalBooks() == null ? totalBooks : stat.getTotalBooks() + totalBooks);
            stat.setTotalAmount(stat.getTotalAmount() == null ? order.getTotalPrice() : stat.getTotalAmount().add(order.getTotalPrice()));
            userStatMap.put(username, stat);
        }
        
        // 按消费金额排序
        List<StatView.TopUser> topUsers = new ArrayList<>(userStatMap.values());
        topUsers.sort(Comparator.comparing(StatView.TopUser::getTotalAmount).reversed());
        
        return ApiResponse.success(topUsers);
    }

    @Override
    public ApiResponse getPersonalStat(String username, String startTime, String endTime) {
        log.info("Calculating personal stat for user={}, startTime={}, endTime={}", username, startTime, endTime);
        
        List<Order> orders;
        if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startTime + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endTime + "T23:59:59");
            orders = orderRepository.findByUsernameAndTimeRange(username, start, end);
        } else {
            orders = orderRepository.findByUsernameOrderByOrderTimeDesc(username);
        }
        
        // 按书籍统计购买情况
        Map<Long, StatView.PersonalStat> bookStatMap = new HashMap<>();
        int totalBooks = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (Order order : orders) {
            totalAmount = totalAmount.add(order.getTotalPrice() == null ? BigDecimal.ZERO : order.getTotalPrice());
            
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                totalBooks += item.getQuantity();
                
                Long bookId = item.getBookId();
                StatView.PersonalStat stat = bookStatMap.getOrDefault(bookId, new StatView.PersonalStat());
                stat.setBookId(bookId);
                stat.setTitle(item.getTitle());
                stat.setQuantity(stat.getQuantity() == null ? item.getQuantity() : stat.getQuantity() + item.getQuantity());
                BigDecimal subtotal = item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                stat.setSubtotal(stat.getSubtotal() == null ? subtotal : stat.getSubtotal().add(subtotal));
                bookStatMap.put(bookId, stat);
            }
        }
        
        // 构造返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("books", new ArrayList<>(bookStatMap.values()));
        result.put("totalBooks", totalBooks);
        result.put("totalAmount", totalAmount);
        
        return ApiResponse.success(result);
    }

    /**
     * 按时间范围查询所有订单
     */
    private List<Order> getOrdersByTimeRange(String startTime, String endTime) {
        if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startTime + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endTime + "T23:59:59");
            return orderRepository.findByTimeRange(start, end);
        } else {
            return orderRepository.findAllByOrderByOrderTimeDesc();
        }
    }
}