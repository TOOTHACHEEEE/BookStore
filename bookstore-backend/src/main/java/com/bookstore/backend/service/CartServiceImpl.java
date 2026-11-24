package com.bookstore.backend.service;

import com.bookstore.backend.dto.AddToCartRequest;
import com.bookstore.backend.dto.ApiResponse;
import com.bookstore.backend.dto.CartItemView;
import com.bookstore.backend.entity.Book;
import com.bookstore.backend.entity.CartItem;
import com.bookstore.backend.repository.BookRepository;
import com.bookstore.backend.repository.CartItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 购物车服务实现类
 * 处理购物车相关业务逻辑：查看购物车、添加商品、删除商品、清空购物车
 * 安全特性：删除购物车项时验证用户权限，防止删除其他用户的商品
 * JPA关联：CartItem与Book通过@ManyToOne关联，支持级联查询
 */
@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private BookRepository bookRepository;

    /**
     * 获取用户购物车
     * @param username 用户名（由后端从Session获取）
     * @return 购物车商品列表
     */
    @Override
    public ApiResponse getCart(String username) {
        log.info("Querying cart for user: {}", username);

        if (username == null || username.isEmpty()) {
            return ApiResponse.error("用户名不能为空");
        }

        // 查询用户的所有购物车项，按添加时间倒序排列
        List<CartItem> items = cartItemRepository.findByUsernameOrderByAddedTimeDesc(username);
        List<CartItemView> views = new ArrayList<>();

        for (CartItem item : items) {
            // 通过JPA关联关系直接获取Book对象，无需额外查询数据库
            Book book = item.getBook();
            CartItemView view = toView(item, book);
            views.add(view);
        }

        return ApiResponse.success(views);
    }

    /**
     * 添加商品到购物车
     * 如果商品已存在则增加数量，否则创建新的购物车项
     * @param request 添加请求（包含用户名和书籍ID）
     * @return 操作结果
     */
    @Override
    @Transactional
    public ApiResponse addToCart(AddToCartRequest request) {
        if (request.getUsername() == null || request.getUsername().isEmpty()) {
            return ApiResponse.error("用户名不能为空");
        }
        if (request.getBookId() == null) {
            return new ApiResponse(400, "书籍ID不能为空", null);
        }

        // 查询书籍是否存在
        Optional<Book> bookOpt = bookRepository.findById(request.getBookId());
        if (bookOpt.isEmpty()) {
            return new ApiResponse(404, "书籍不存在", null);
        }

        int qty = request.getQuantity() == null || request.getQuantity() <= 0 ? 1 : request.getQuantity();
        Book book = bookOpt.get();
        
        // 检查购物车中是否已存在该商品
        CartItem existing = cartItemRepository.findByUsernameAndBook(request.getUsername(), book);

        if (existing != null) {
            // 商品已存在，增加数量
            existing.setQuantity(existing.getQuantity() + qty);
            cartItemRepository.save(existing);
        } else {
            // 商品不存在，创建新的购物车项
            CartItem item = new CartItem();
            item.setUsername(request.getUsername());
            // 直接设置Book对象，JPA会自动管理外键关系
            item.setBook(book);
            item.setQuantity(qty);
            item.setAddedTime(java.time.LocalDateTime.now());
            cartItemRepository.save(item);
        }

        return ApiResponse.success("已加入购物车");
    }

    /**
     * 删除购物车中的商品
     * 安全验证：确保用户只能删除自己的购物车项
     * @param id 购物车项ID
     * @param username 当前用户名（由后端从Session获取）
     * @return 操作结果
     */
    @Override
    @Transactional
    public ApiResponse removeFromCart(Long id, String username) {
        if (id == null) {
            return new ApiResponse(400, "购物车项ID不能为空", null);
        }
        if (username == null || username.isEmpty()) {
            return new ApiResponse(401, "用户未登录", null);
        }

        // 查询购物车项是否存在
        Optional<CartItem> itemOpt = cartItemRepository.findById(id);
        if (itemOpt.isEmpty()) {
            return new ApiResponse(404, "购物车项不存在", null);
        }

        // 【安全验证】确保只能删除自己的购物车项
        CartItem item = itemOpt.get();
        if (!item.getUsername().equals(username)) {
            return new ApiResponse(403, "无权删除其他用户的购物车项", null);
        }

        cartItemRepository.deleteById(id);
        return ApiResponse.success("已删除");
    }

    /**
     * 清空用户购物车
     * @param username 用户名（由后端从Session获取）
     * @return 操作结果
     */
    @Override
    @Transactional
    public ApiResponse clearCart(String username) {
        if (username == null || username.isEmpty()) {
            return ApiResponse.error("用户名不能为空");
        }

        cartItemRepository.deleteByUsername(username);
        return ApiResponse.success("已清空购物车");
    }

    /**
     * 将购物车项实体转换为视图对象（DTO）
     * @param item 购物车项实体
     * @param book 书籍实体
     * @return 购物车项视图对象
     */
    private CartItemView toView(CartItem item, Book book) {
        CartItemView v = new CartItemView();
        v.setCartItemId(item.getId());
        v.setBookId(book.getId());
        v.setTitle(book.getTitle());
        v.setAuthor(book.getAuthor());
        v.setCover(book.getCover());
        BigDecimal price = book.getPrice() == null ? BigDecimal.ZERO : book.getPrice();
        v.setPrice("$" + price.setScale(2, RoundingMode.HALF_UP).toPlainString());
        v.setQuantity(item.getQuantity());
        v.setAddedTime(item.getAddedTime() == null ? null : item.getAddedTime().format(FMT));
        return v;
    }
}