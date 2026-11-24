package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.BuyNowRequest;
import com.bookstore.backend.entity.Book;
import com.bookstore.backend.entity.Order;
import com.bookstore.backend.entity.OrderItem;
import com.bookstore.backend.repository.BookRepository;
import com.bookstore.backend.repository.OrderItemRepository;
import com.bookstore.backend.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setId(1L);
        testBook.setTitle("三体");
        testBook.setAuthor("刘慈欣");
        testBook.setPrice(new BigDecimal("68.00"));
        testBook.setCover("https://example.com/cover.jpg");
    }

    @Test
    void buyNow_ShouldCreateOrderWithCascadeItems() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            // 验证级联关系：订单项应该被添加到订单的集合中
            assertEquals(1, order.getOrderItems().size());
            OrderItem item = order.getOrderItems().get(0);
            assertEquals(testBook.getId(), item.getBookId());
            assertEquals(testBook.getTitle(), item.getTitle());
            return order;
        });

        when(orderItemRepository.findByOrderId(100L)).thenReturn(new ArrayList<>());

        BuyNowRequest request = new BuyNowRequest();
        request.setUsername("testuser");
        request.setBookId(1L);

        ApiResponse response = orderService.buyNow(request);

        assertEquals(200, response.getCode());
        assertNotNull(response.getData());
    }

    @Test
    void buyNow_ShouldReturnErrorWhenBookNotFound() {
        when(bookRepository.findById(999L)).thenReturn(Optional.empty());

        BuyNowRequest request = new BuyNowRequest();
        request.setUsername("testuser");
        request.setBookId(999L);

        ApiResponse response = orderService.buyNow(request);

        assertEquals(404, response.getCode());
        assertEquals("书籍不存在", response.getMessage());
    }

    @Test
    void buyNow_ShouldReturnErrorWhenUsernameEmpty() {
        BuyNowRequest request = new BuyNowRequest();
        request.setUsername("");
        request.setBookId(1L);

        ApiResponse response = orderService.buyNow(request);

        assertEquals(500, response.getCode());
        assertEquals("用户名不能为空", response.getMessage());
    }
}