package com.bookstore.backend.service;

import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.BuyNowRequest;
import com.bookstore.backend.dto.CheckoutRequest;
import com.bookstore.backend.dto.OrderView;
import com.bookstore.backend.entity.Book;
import com.bookstore.backend.entity.CartItem;
import com.bookstore.backend.entity.Order;
import com.bookstore.backend.entity.OrderItem;
import com.bookstore.backend.repository.BookRepository;
import com.bookstore.backend.repository.CartItemRepository;
import com.bookstore.backend.repository.OrderItemRepository;
import com.bookstore.backend.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 【Service实现类】订单服务实现
 * 
 * 实现OrderService接口，提供具体的业务逻辑
 */
@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private BookRepository bookRepository;

    @Override
    public ApiResponse getOrders(String username) {
        log.info("Querying orders for user: {}", username);
        List<Order> orders = orderRepository.findByUsernameOrderByOrderTimeDesc(username);
        List<OrderView> views = new ArrayList<>();
        for (Order o : orders) {
            views.add(toView(o));
        }
        return ApiResponse.success(views);
    }

    @Override
    @Transactional
    public ApiResponse checkout(CheckoutRequest request) {
        String username = request.getUsername();

        if (username == null || username.isEmpty()) {
            return ApiResponse.error("用户名不能为空");
        }

        List<CartItem> cartItems = cartItemRepository.findByUsernameOrderByAddedTimeDesc(username);
        if (cartItems.isEmpty()) {
            return ApiResponse.error("购物车为空");
        }

        Order order = new Order();
        order.setUsername(username);
        order.setStatus("已支付");
        order.setOrderTime(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem c : cartItems) {
            // 【JPA关联】通过关联关系直接获取Book对象，无需额外查询
            Book book = c.getBook();
            if (book == null) {
                continue;
            }

            // 【库存检查】购买前检查库存是否足够
            if (book.getStock() == null || book.getStock() < c.getQuantity()) {
                log.warn("Insufficient stock for book: {}. Required: {}, Available: {}", 
                    book.getTitle(), c.getQuantity(), book.getStock());
                continue;  // 库存不足则跳过该书
            }

            BigDecimal subtotal = book.getPrice().multiply(BigDecimal.valueOf(c.getQuantity()));
            total = total.add(subtotal);

            OrderItem oi = new OrderItem();
            oi.setBookId(book.getId());
            oi.setTitle(book.getTitle());
            oi.setAuthor(book.getAuthor());
            oi.setCover(book.getCover());
            oi.setPrice(book.getPrice());
            oi.setQuantity(c.getQuantity());
            orderItems.add(oi);

            // 【库存扣减】购买后减少库存
            book.setStock(book.getStock() - c.getQuantity());
            bookRepository.save(book);  // 更新库存
        }

        order.setTotalPrice(total);
        // 【级联保存】通过JPA关联关系，直接将订单项添加到订单的集合中
        // cascade = CascadeType.ALL 会自动保存所有关联的订单项
        for (OrderItem oi : orderItems) {
            oi.setOrder(order);
            order.getOrderItems().add(oi);
        }
        Order saved = orderRepository.save(order);

        cartItemRepository.deleteByUsername(username);

        log.info("Order created. id={}, total={}", saved.getId(), total);
        return ApiResponse.success(toView(saved));
    }

    @Override
    @Transactional
    public ApiResponse buyNow(BuyNowRequest request) {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            return ApiResponse.error("用户名不能为空");
        }
        if (request.getBookId() == null) {
            return ApiResponse.error("书籍ID不能为空");
        }

        Optional<Book> bookOpt = bookRepository.findById(request.getBookId());
        if (bookOpt.isEmpty()) {
            return new ApiResponse(404, "书籍不存在", null);
        }
        Book book = bookOpt.get();

        // 【库存检查】购买前检查库存是否足够
        if (book.getStock() == null || book.getStock() < 1) {
            return ApiResponse.error("库存不足，无法购买");
        }

        // 【库存扣减】购买后减少库存
        book.setStock(book.getStock() - 1);
        bookRepository.save(book);

        Order order = new Order();
        order.setUsername(request.getUsername());
        order.setStatus("已支付");
        order.setOrderTime(LocalDateTime.now());
        order.setTotalPrice(book.getPrice());

        // 【级联保存】直接将订单项添加到订单的集合中
        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setBookId(book.getId());
        oi.setTitle(book.getTitle());
        oi.setAuthor(book.getAuthor());
        oi.setCover(book.getCover());
        oi.setPrice(book.getPrice());
        oi.setQuantity(1);
        order.getOrderItems().add(oi);

        // cascade = CascadeType.ALL 会自动保存订单和关联的订单项
        Order saved = orderRepository.save(order);

        log.info("BuyNow order created. id={}", saved.getId());
        return ApiResponse.success(toView(saved));
    }

    private OrderView toView(Order order) {
        OrderView view = new OrderView();
        view.setId(order.getId());
        view.setUsername(order.getUsername());
        view.setStatus(order.getStatus());
        view.setTotal(order.getTotalPrice() == null ? "0" : order.getTotalPrice().toPlainString());
        view.setOrderTime(order.getOrderTime() == null ? null : order.getOrderTime().format(FMT));

        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        List<OrderView.OrderItemView> itemViews = new ArrayList<>();
        for (OrderItem oi : items) {
            OrderView.OrderItemView iv = new OrderView.OrderItemView();
            iv.setId(oi.getId());
            iv.setBookId(oi.getBookId());
            iv.setTitle(oi.getTitle());
            iv.setAuthor(oi.getAuthor());
            iv.setCover(oi.getCover());
            iv.setPrice("$" + oi.getPrice().toPlainString());
            iv.setQuantity(oi.getQuantity());
            itemViews.add(iv);
        }
        view.setItems(itemViews);

        return view;
    }

    @Override
    public ApiResponse searchUserOrders(String username, String startTime, String endTime, String bookTitle) {
        log.info("Searching orders for user: {}, startTime: {}, endTime: {}, bookTitle: {}", 
            username, startTime, endTime, bookTitle);
        
        List<Order> orders;
        
        // 时间范围查询
        if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startTime + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endTime + "T23:59:59");
            orders = orderRepository.findByUsernameAndTimeRange(username, start, end);
        } else {
            orders = orderRepository.findByUsernameOrderByOrderTimeDesc(username);
        }
        
        // 书籍名称过滤
        if (bookTitle != null && !bookTitle.isEmpty()) {
            orders = filterOrdersByBookTitle(orders, bookTitle);
        }
        
        List<OrderView> views = new ArrayList<>();
        for (Order o : orders) {
            views.add(toView(o));
        }
        
        return ApiResponse.success(views);
    }

    @Override
    public ApiResponse getAllOrders() {
        log.info("Querying all orders (admin)");
        List<Order> orders = orderRepository.findAllByOrderByOrderTimeDesc();
        
        List<OrderView> views = new ArrayList<>();
        for (Order o : orders) {
            views.add(toView(o));
        }
        
        return ApiResponse.success(views);
    }

    @Override
    public ApiResponse searchAllOrders(String startTime, String endTime, String bookTitle) {
        log.info("Searching all orders (admin), startTime: {}, endTime: {}, bookTitle: {}", 
            startTime, endTime, bookTitle);
        
        List<Order> orders;
        
        // 时间范围查询
        if (startTime != null && endTime != null && !startTime.isEmpty() && !endTime.isEmpty()) {
            LocalDateTime start = LocalDateTime.parse(startTime + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(endTime + "T23:59:59");
            orders = orderRepository.findByTimeRange(start, end);
        } else {
            orders = orderRepository.findAllByOrderByOrderTimeDesc();
        }
        
        // 书籍名称过滤
        if (bookTitle != null && !bookTitle.isEmpty()) {
            orders = filterOrdersByBookTitle(orders, bookTitle);
        }
        
        List<OrderView> views = new ArrayList<>();
        for (Order o : orders) {
            views.add(toView(o));
        }
        
        return ApiResponse.success(views);
    }

    /**
     * 按书籍名称过滤订单
     */
    private List<Order> filterOrdersByBookTitle(List<Order> orders, String bookTitle) {
        List<Order> filtered = new ArrayList<>();
        for (Order order : orders) {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : items) {
                if (item.getTitle() != null && item.getTitle().toLowerCase().contains(bookTitle.toLowerCase())) {
                    filtered.add(order);
                    break;  // 一个订单只需匹配一次
                }
            }
        }
        return filtered;
    }
}