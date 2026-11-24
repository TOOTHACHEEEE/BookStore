# 电子书店项目答辩辅助文档

> 本文档按照"先整体、后局部"的思路编写，帮助你理解整个工程的来龙去脉。建议答辩前先通读一遍，再对照代码看。

---

## 一、项目整体架构（先看懂这张图，后面都是细节）

本项目采用**前后端分离**的架构模式。你可以把它想象成一家餐厅：

- **前端** = 餐厅门面 + 服务员 + 菜单（顾客看到、摸到、用到的一切）
- **后端** = 后厨 + 仓库 + 账本（处理真正的业务逻辑、存取数据）
- **数据库(MySQL)** = 仓库里的货架（真正存放数据的地方）
- **HTTP接口(API)** = 服务员和后厨之间的传菜口（前后端唯一的交流通道）

### 技术栈一览

| 层级 | 技术 | 作用 |
|------|------|------|
| 前端 | React 18 + Vite + Ant Design | 构建用户界面 |
| 前端路由 | React Router v6 | 实现页面跳转（首页→详情页→购物车等） |
| 后端 | Spring Boot 3.2.4 + Java 17 | 提供业务逻辑和数据服务 |
| 数据访问 | Spring Data JPA | 简化数据库操作，不用手写SQL |
| 数据库 | MySQL 8 | 持久化存储书籍、用户、订单等数据 |
| 通信协议 | HTTP + JSON | 前后端交换数据的格式 |

---

## 二、后端工程详解（重点，占整个项目的核心）

后端工程在 `bookstore-backend` 文件夹中，是一个标准的 **Maven + Spring Boot** 项目。

### 2.1 项目的"骨架"——Maven与依赖

打开 `pom.xml`，这是 Maven 项目的"购物清单"，告诉编译器需要下载哪些第三方库。

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.4</version>
</parent>
```

**什么是 parent？** 就像你写word文档套了一个模板，Spring Boot 的 parent 已经帮你预设好了各种兼容版本，你不需要手动一个个去指定。

**核心依赖的作用：**

| 依赖名 | 通俗解释 |
|--------|----------|
| `spring-boot-starter-web` | 让项目能接收HTTP请求，变成一台"Web服务器" |
| `spring-boot-starter-data-jpa` | 数据库访问神器，你写一个接口，它自动帮你生成增删改查的代码 |
| `spring-boot-starter-validation` | 检查用户输入是否合法（比如密码不能为空） |
| `mysql-connector-j` | 连接 MySQL 数据库的驱动，相当于不同数据库的"转接头" |
| `fastjson2` | 阿里巴巴的JSON处理库，用于Java对象和JSON字符串互相转换 |

### 2.2 分层架构——代码为什么这么放？

后端代码按照**职责**分成了不同的包（package），这是业界标准做法，就像医院分科室一样：

```
com.bookstore.backend
├── config        ← 配置类（如跨域配置）
├── controller    ← 控制层：接待前端的HTTP请求
├── service       ← 业务逻辑层：处理核心业务，协调各模块
├── dto           ← 数据传输对象：包装给前端看的数据格式
├── entity        ← 实体层：与数据库表一一对应的Java类
├── repository    ← 仓库层：直接操作数据库的接口
```

#### 为什么要分层？

想象你去餐厅吃饭：
- `Controller` = 前台服务员（接待你，听你的需求，最后把菜端给你）
- `Service` = 厨师长（安排怎么做菜，协调各个岗位）—— **本项目已独立抽离**
- `Repository` = 切配师傅（从仓库拿食材，处理好后交给厨师）
- `Entity` = 仓库里的食材档案（每种食材长什么样、有多少库存）
- `DTO` = 给客人看的精美菜单（不是直接把后厨的原材料端上桌）

---

### 2.3 业务逻辑层(Service)——核心业务处理中心

本项目采用标准的三层架构，将业务逻辑从 Controller 层独立出来，形成专门的 `service` 层。

#### Service 层的职责

| 职责 | 说明 |
|------|------|
| **业务逻辑处理** | 实现核心业务规则（如购物车数量累加、订单结算） |
| **事务管理** | 使用 `@Transactional` 保证数据一致性 |
| **模块协调** | 协调多个 Repository 完成复杂业务 |
| **数据转换** | Entity 到 DTO 的转换 |

#### 四个 Service 类详解

**① BookService —— 书籍业务**
```java
@Service
public class BookService {
    @Autowired
    private BookRepository bookRepository;

    public ApiResponse getAllBooks() { ... }
    public ApiResponse getBookById(Long id) { ... }
}
```

**② UserService —— 用户业务**
```java
@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public ApiResponse register(UserRegisterRequest request) { ... }
    public ApiResponse login(UserLoginRequest request) { ... }
    public ApiResponse getUser(String username) { ... }
}
```

**③ CartService —— 购物车业务**
```java
@Service
public class CartService {
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private BookRepository bookRepository;

    @Transactional
    public ApiResponse addToCart(AddToCartRequest request) { ... }
    public ApiResponse getCart(String username) { ... }
    @Transactional
    public ApiResponse removeFromCart(Long cartItemId) { ... }
    @Transactional
    public ApiResponse clearCart(String username) { ... }
}
```

**④ OrderService —— 订单业务**
```java
@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private BookRepository bookRepository;

    public ApiResponse getOrders(String username) { ... }
    
    @Transactional
    public ApiResponse checkout(CheckoutRequest request) { ... }
    
    @Transactional
    public ApiResponse buyNow(BuyNowRequest request) { ... }
}
```

#### Controller 与 Service 的协作模式

Controller 层变得非常简洁，只负责：
1. 接收 HTTP 请求
2. 参数校验（基本校验）
3. 调用对应的 Service 方法
4. 返回响应

```java
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    @Autowired
    private UserService userService;  // 注入 Service

    @PostMapping("/register")
    public ApiResponse register(@RequestBody UserRegisterRequest request) {
        return userService.register(request);  // 调用 Service
    }
}
```

**分层架构的优势：**
- **职责清晰**：Controller 只处理 HTTP 相关逻辑，Service 只处理业务逻辑
- **易于测试**：可以单独测试 Service 层的业务逻辑，不需要启动整个 Web 容器
- **代码复用**：多个 Controller 可以调用同一个 Service 方法
- **事务管理**：Service 层统一管理事务，保证数据一致性

---

### 2.4 实体层(Entity)——Java对象如何对应数据库表

**核心概念：ORM（对象关系映射）**

数据库里的表是二维的（行和列），Java里是面向对象的（类和对象）。ORM 的作用就是**让Java类自动对应到数据库表**，你操作Java对象，就等于在操作数据库记录。

以 `Book.java`（书籍实体）为例：

```java
@Entity                    // ← 标记这是一个实体类，JPA会管理它
@Table(name = "book")      // ← 对应数据库里的 book 表
public class Book {

    @Id                   // ← 这是主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // ← 主键自增
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;  // ← 对应 book 表的 title 字段

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;  // ← 用BigDecimal存钱，避免浮点数精度问题
    // ... 其他字段省略
}
```

**注解详解（答辩可能会被问到）：**

| 注解 | 作用 |
|------|------|
| `@Entity` | 告诉JPA：这个类要映射到数据库表 |
| `@Table(name = "xxx")` | 指定对应的数据库表名 |
| `@Id` | 标记主键字段（一张表必须有主键） |
| `@GeneratedValue` | 主键如何生成，`IDENTITY`表示交给数据库自增 |
| `@Column` | 标记普通字段，`nullable=false`表示必填，`length`限制长度 |

**项目中所有的实体：**
- `Book`：书籍信息（书名、作者、价格、库存等）
- `User`：用户信息（用户名、密码、邮箱等）
- `CartItem`：购物车项（谁把哪本书加进了购物车，加了几本）
- `Order`：订单（谁买的，总价多少，什么时候下单）
- `OrderItem`：订单明细（某个订单里包含了哪些书，每本几本）

**表之间的关系（重点理解）：**
- 一个用户有多个购物车项 → 一对多（通过 `username` 字段关联）
- 一个用户有多个订单 → 一对多（通过 `username` 字段关联）
- 一个订单有多个订单项 → 一对多（通过 `order_id` 外键关联）
- 购物车项引用一本书 → 多对一（通过 `book_id` 外键关联）

---

### 2.4 数据库设计——schema.sql

打开 `src/main/resources/schema.sql`，这是数据库的**建表脚本**。项目每次启动都会执行它（由 `application.properties` 中的 `spring.sql.init.mode=always` 控制）。

**五张表的结构：**

1. **`book` 表** —— 存储书籍商品信息
   - `id`: 主键，自增
   - `title`, `author`, `price`, `isbn`, `cover`, `description`, `publisher`, `publish_date`, `pages`, `stock`
   - `stock` 字段表示库存数量

2. **`user` 表** —— 存储注册用户
   - `id`: 主键
   - `username`: 唯一，登录账号
   - `password`: 密码（注意：本项目是明文存储，生产环境应该加密）
   - `email`, `avatar`, `created_at`

3. **`cart_item` 表** —— 购物车
   - `id`: 主键
   - `username`: 属于哪个用户
   - `book_id`: 对应哪本书（外键，关联 `book.id`）
   - `quantity`: 数量
   - `added_time`: 加入时间

4. **`orders` 表** —— 订单主表
   - `id`: 主键
   - `username`: 下单用户
   - `total_price`: 订单总金额
   - `status`: 订单状态（默认"已支付"）
   - `order_time`: 下单时间

5. **`order_item` 表** —— 订单明细
   - `id`: 主键
   - `order_id`: 属于哪个订单（外键，关联 `orders.id`）
   - `book_id`, `title`, `author`, `cover`, `price`, `quantity`
   - 注意：这里冗余存储了书籍信息（title, author等），目的是**快照**——即使以后书的信息改了，订单记录不变

**外键约束（FOREIGN KEY）的作用：**
```sql
CONSTRAINT `fk_cart_book` FOREIGN KEY (`book_id`) REFERENCES `book` (`id`) ON DELETE CASCADE
```
意思是：如果 `book` 表里的某本书被删除了，所有引用这本书的购物车记录也会自动删除（`CASCADE` 级联删除）。

---

### 2.5 数据访问层(Repository)——不写SQL也能查数据库

这是 Spring Data JPA 最神奇的地方。你只需要定义一个**接口**，继承 `JpaRepository`，就能自动获得一堆数据库操作方法。

以 `BookRepository.java` 为例：

```java
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    // 这里什么都不写，就已经有了：
    // findAll()        —— 查所有书
    // findById(id)     —— 按ID查一本书
    // save(book)       —— 保存/更新一本书
    // deleteById(id)   —— 按ID删除一本书
    // ... 等等几十个方法
}
```

**自定义查询——"约定大于配置"：**

JPA 支持通过**方法名**自动推断查询逻辑：

```java
// UserRepository
User findByUsername(String username);
// JPA 自动翻译成：SELECT * FROM user WHERE username = ?

// CartItemRepository
List<CartItem> findByUsernameOrderByAddedTimeDesc(String username);
// 自动翻译成：SELECT * FROM cart_item WHERE username = ? ORDER BY added_time DESC

CartItem findByUsernameAndBookId(String username, Long bookId);
// 自动翻译成：SELECT * FROM cart_item WHERE username = ? AND book_id = ?

void deleteByUsername(String username);
// 自动翻译成：DELETE FROM cart_item WHERE username = ?
```

**`Optional<T>` 是什么？**

`findById` 返回的是 `Optional<Book>`，它像一个"可能为空的盒子"。因为按ID查数据，可能查得到也可能查不到，用 `Optional` 可以避免空指针异常。

```java
Optional<Book> bookOpt = bookRepository.findById(id);
if (bookOpt.isEmpty()) {
    // 书不存在，返回404
}
Book book = bookOpt.get();  // 从盒子里取出书
```

---

### 2.6 控制层(Controller)——RESTful API 设计

`Controller` 是后端接收前端请求的入口。本项目采用 **RESTful** 风格设计接口。

**什么是 RESTful？** 简单来说就是用 HTTP 的动词（GET/POST/PUT/DELETE）来表达操作，用 URL 来表达资源。

| HTTP方法 | 含义 | 本项目例子 |
|----------|------|-----------|
| GET | 获取数据 | `GET /api/v1/books` 获取所有书籍 |
| POST | 创建数据 | `POST /api/v1/users/register` 注册用户 |
| DELETE | 删除数据 | `DELETE /api/v1/cart/5` 删除购物车第5项 |

#### 四个控制器详解：

**① BookController —— 书籍查询**
```java
@RestController              // ← 标记这是REST控制器，返回JSON而不是HTML页面
@RequestMapping("/api/v1")   // ← 基础路径，所有方法前面都带这个前缀
public class BookController {

    @Autowired               // ← 自动注入：Spring会自动把BookRepository的实例塞进来
    private BookRepository bookRepository;

    @GetMapping("/books")    // ← 处理 GET /api/v1/books 请求
    public ApiResponse getAllBooks() { ... }

    @GetMapping("/book/{id}") // ← {id}是路径参数，如 /api/v1/book/3
    public ApiResponse getBookById(@PathVariable Long id) { ... }
}
```

**② UserController —— 用户注册/登录/查询**

这里有一个**特殊的业务逻辑**：登录时如果用户不存在，会自动注册（简化演示用）。

```java
@PostMapping("/login")
public ApiResponse login(@RequestBody UserLoginRequest request) {
    User user = userRepository.findByUsername(request.getUsername());
    if (user == null) {
        // 用户不存在？自动创建！
        user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        userRepository.save(user);
    } else if (!user.getPassword().equals(request.getPassword())) {
        return ApiResponse.error("密码错误");
    }
    // 返回用户信息给前端
    return ApiResponse.success(data);
}
```

`@RequestBody` 表示把前端发来的 JSON 数据自动转成 Java 对象。例如前端发送：
```json
{"username":"toothache", "password":"123456"}
```
Spring 会自动把它转成 `UserLoginRequest` 对象。

**③ CartController —— 购物车操作**

核心逻辑：添加到购物车时，如果同一本书已经在购物车里了，就**增加数量**，而不是新增一条记录。

```java
@PostMapping
@Transactional   // ← 事务注解，下面解释
public ApiResponse addToCart(@RequestBody AddToCartRequest request) {
    CartItem existing = cartItemRepository.findByUsernameAndBookId(...);
    if (existing != null) {
        existing.setQuantity(existing.getQuantity() + qty);  // 数量+1
        cartItemRepository.save(existing);
    } else {
        // 新建购物车项
    }
}
```

**④ OrderController —— 订单结算**

两个核心接口：
- `/checkout`：结算购物车（把购物车所有商品转成订单，然后清空购物车）
- `/buyNow`：立即购买（跳过购物车，直接为单本书生成订单）

结算逻辑（事务性操作）：
```java
@PostMapping("/checkout")
@Transactional   // ← 关键！保证下面所有操作要么全成功，要么全回滚
public ApiResponse checkout(@RequestBody CheckoutRequest request) {
    // 1. 查出购物车所有商品
    // 2. 计算总价
    // 3. 创建订单（orders表插入记录）
    // 4. 创建订单明细（order_item表插入多条记录）
    // 5. 清空购物车
    // 如果第4步失败了，前面已经插入的订单数据会自动回滚，不会出现"钱扣了但订单没记录"的情况
}
```

**`@Transactional`（事务）通俗解释：**

想象银行转账：A给B转100元，需要两步：A的账户减100，B的账户加100。如果第一步成功后第二步失败了，那100元就凭空消失了。事务的作用就是：**这两步要么都成功，要么都不做**。Spring 的 `@Transactional` 就是帮你保证这个的。

---

### 2.7 DTO——为什么不用Entity直接返回给前端？

DTO（Data Transfer Object，数据传输对象）是前端和后端之间的"快递包裹"。

**为什么需要DTO？直接返回Entity不行吗？**

有几个原因：
1. **数据格式化**：例如 `Book` 实体里的 `price` 是 `BigDecimal` 类型，前端希望看到 `"$49.90"` 字符串，DTO负责做这个转换。
2. **保护敏感字段**：`User` 实体有 `password` 字段，绝不能直接返回给前端。DTO只挑选安全的字段暴露。
3. **解耦**：后端数据库结构变了，只要DTO不变，前端就不受影响。

**本项目中的DTO：**

- `BookView`：给前端看的书籍信息（price变成了美元字符串）
- `CartItemView`：购物车项（包含了书的完整信息，方便前端直接展示）
- `OrderView`：订单视图（包含嵌套的订单项列表）
- `ApiResponse`：统一的响应包装（后面讲）
- `UserLoginRequest` / `UserRegisterRequest` / `AddToCartRequest` / `CheckoutRequest` / `BuyNowRequest`：接收前端参数的"请求DTO"

---

### 2.8 统一响应格式——ApiResponse

本项目所有接口返回的数据都包在统一的"信封"里：

```java
public class ApiResponse {
    private int code;       // 状态码：200成功，500错误，404不存在
    private String message; // 提示信息
    private Object data;    // 真正的数据（可以是对象、列表、字符串等）
}
```

例如成功时：
```json
{
  "code": 200,
  "message": "success",
  "data": [{"id":1, "title":"三体", ...}]
}
```

失败时：
```json
{
  "code": 500,
  "message": "用户名不能为空",
  "data": null
}
```

**好处：** 前端处理响应时可以统一判断 `code === 200`，不用每种情况都写不同的处理逻辑。

---

### 2.9 跨域配置(CORS)

前后端分离开发时，前端跑在 `http://localhost:3000`，后端跑在 `http://localhost:8080`，**域名+端口不同**，浏览器会阻止它们通信，这叫"**跨域**"（CORS）。

`CorsConfig.java` 的作用是告诉 Spring Boot：**允许任何来源访问本服务的任何接口**（开发环境的宽松配置）。

```java
@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOriginPatterns(Collections.singletonList("*"));  // 允许任何域名
        corsConfiguration.addAllowedHeader(CorsConfiguration.ALL);                  // 允许任何请求头
        corsConfiguration.addAllowedMethod(CorsConfiguration.ALL);                  // 允许任何HTTP方法
        corsConfiguration.setAllowCredentials(true);                                // 允许携带Cookie
        // ...
    }
}
```

---

### 2.10 配置文件——application.properties 逐行解释

```properties
# 数据库连接地址
# jdbc:mysql://  表示用JDBC连接MySQL
# localhost:3306 MySQL运行的地址和端口
# /bookstore      数据库名
# 后面一长串是参数：UTF-8编码、UTC时区、允许公钥检索、不用SSL、库不存在时自动创建
spring.datasource.url=jdbc:mysql://localhost:3306/bookstore?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false&createDatabaseIfNotExist=true

spring.datasource.username=root      # 数据库用户名
spring.datasource.password=...       # 数据库密码
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver  # MySQL驱动类名

# JPA配置
spring.jpa.hibernate.ddl-auto=none   # Hibernate不自动建表（我们用schema.sql手工建）
spring.jpa.show-sql=true             # 在控制台打印执行的SQL语句（方便调试）
spring.jpa.properties.hibernate.format_sql=true  # SQL格式化，更易读

# SQL初始化配置
spring.sql.init.mode=always          # 每次启动都执行schema.sql
spring.sql.init.schema-locations=classpath:schema.sql  # 建表脚本的位置
spring.jpa.defer-datasource-initialization=true  # 让schema.sql在JPA初始化之后执行

server.port=8080                     # 后端服务运行在8080端口
```

---

## 三、前端工程与后端的关联（前端讲关联后端的部分）

前端工程在 `bookstore-frontend` 文件夹中，是一个 **React + Vite** 项目。

### 3.1 前端技术栈简介

| 技术 | 作用 |
|------|------|
| React 18 | 构建用户界面的JavaScript库，用"组件"思想组织页面 |
| Vite | 前端构建工具（类似Webpack但更快），负责编译、打包、启动开发服务器 |
| React Router | 前端路由，实现点击链接切换页面而不刷新浏览器 |
| Ant Design | 阿里巴巴的UI组件库，提供按钮、表单、弹窗等现成样式 |

### 3.2 前后端如何通信——代理配置

前端开发服务器跑在 `http://localhost:3000`，后端在 `http://localhost:8080`。直接发请求会跨域。

`vite.config.js` 中配置了**代理（Proxy）**：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 把所有以 /api 开头的请求转发到8080端口
      changeOrigin: true,
    },
  },
}
```

**通俗解释：** 前端请求 `/api/v1/books`，Vite 开发服务器偷偷把这个请求转发给 `http://localhost:8080/api/v1/books`，再把后端的响应原样还给前端。因为浏览器只和 `localhost:3000` 打交道，所以不会触发跨域拦截。

### 3.3 API封装层——api.js

`src/api/api.js` 是前端和后端之间的**唯一桥梁**。所有前后端交互都通过这个文件：

```javascript
const BASE_URL = '/api/v1'

// 通用请求函数：自动处理JSON序列化、统一拆包ApiResponse
async function request(path, options = {}) {
  const resp = await fetch(url, config)   // 发送HTTP请求
  const json = await resp.json()          // 把响应体解析成JSON
  if (json.code !== 200) {                // 检查后端返回的状态码
    throw new Error(json.message || '请求失败')
  }
  return json.data                         // 只返回data部分给调用者
}
```

**前端使用例子：**
```javascript
import { fetchBooks, addToCart, checkoutOrder } from './api/api'

// 获取所有书籍
const books = await fetchBooks()   // 内部调用 GET /api/v1/books

// 加入购物车
await addToCart('toothache', 3, 1) // 内部调用 POST /api/v1/cart

// 结算
await checkoutOrder('toothache')   // 内部调用 POST /api/v1/orders/checkout
```

### 3.4 状态管理与数据流

`src/context/CartContext.jsx` 是前端的核心"大脑"，用 **React Context + Hooks** 管理全局状态。

**为什么叫 Context（上下文）？**

想象一个家族，爷爷有一个保险箱（状态），孙子需要用钱。传统方式是爸爸传给孩子、孩子再传给孙子（逐层prop传递），非常麻烦。Context就像在家里装了个广播：爷爷把保险箱放Context里，任何后代组件都能直接取用。

**数据流（以购物车为例）：**

1. 用户点击"加入购物车"按钮
2. `CartContext` 里的 `addToCart` 函数被调用
3. `addToCart` 调用 `apiAddToCart(user.name, book.id, 1)` → 发送POST请求到后端
4. 后端 `CartController` 处理，把记录存入 `cart_item` 表
5. 请求成功后，`refreshCart(user.name)` 被调用 → 发送GET请求重新拉取购物车数据
6. `setCart(data)` 更新React状态 → 页面自动刷新显示最新购物车

**登录状态的持久化：**

因为HTTP是无状态的，刷新页面后所有JavaScript变量都会丢失。本项目把登录状态（用户名、头像）保存在浏览器的 `localStorage` 里，下次打开页面时自动恢复：

```javascript
// 登录时存入
localStorage.setItem('isLoggedIn', 'true')
localStorage.setItem('user', JSON.stringify(userInfo))

// 刷新页面时读取
const savedUser = localStorage.getItem('user')
if (savedUser) {
  setUser(JSON.parse(savedUser))
}
```

注意：购物车数据和订单数据**不存localStorage**，每次从后端实时获取，保证多端数据一致。

### 3.5 前端路由结构

```
/              → 书籍列表页（BookList）
/book/:id      → 书籍详情页（BookDetail），:id是路径参数
/cart          → 购物车页（Cart）
/orders        → 订单页（Orders）
/profile       → 个人中心页（Profile）
/login         → 登录页（Login）
```

---

## 四、核心业务流程串讲（答辩时可以用这个逻辑来阐述）

### 场景：用户浏览书籍 → 加入购物车 → 结算下单

**第一步：展示书籍列表**
1. 用户打开首页，前端 `BookList` 组件挂载
2. `useEffect` 中调用 `fetchBooks()` → `GET /api/v1/books`
3. Vite代理把请求转发到 `localhost:8080`
4. 后端 `BookController.getAllBooks()` 被触发
5. `bookRepository.findAll()` 查询数据库的 `book` 表
6. 返回的 `List<Book>` 被转成 `List<BookView>`（价格加$符号）
7. 包装成 `ApiResponse` 返回JSON给前端
8. 前端拿到书籍数组，用 `BookCard` 组件渲染出一张张卡片

**第二步：加入购物车**
1. 用户点击某本书的"加入购物车"按钮
2. `CartContext.addToCart(book)` 被调用
3. 调用 `apiAddToCart(user.name, book.id, 1)` → `POST /api/v1/cart`
4. 后端 `CartController.addToCart()` 接收 `AddToCartRequest`
5. 检查这本书是否已经在购物车里（`findByUsernameAndBookId`）
6. 如果已存在：数量+1；如果不存在：新建 `CartItem` 记录
7. 返回 `ApiResponse.success("已加入购物车")`
8. 前端自动调用 `refreshCart()` 重新拉取购物车数据，更新页面角标

**第三步：购物车结算**
1. 用户进入购物车页面，点击"结算"
2. `CartContext.checkout()` 被调用 → `POST /api/v1/orders/checkout`
3. 后端 `OrderController.checkout()` 启动事务（`@Transactional`）
4. 查出该用户所有购物车项 → 计算总价 → 创建 `Order` 记录 → 创建多条 `OrderItem` 记录 → 清空购物车
5. 所有数据库操作成功后，事务提交
6. 前端结算成功后，同时刷新购物车和订单列表
7. 用户跳转到订单页面，看到新订单

---

## 五、答辩常见问题与回答建议

### Q1：为什么用Spring Boot？
**答：** Spring Boot是Java生态中最流行的微服务框架，它的"约定大于配置"理念大大减少了样板代码。内嵌Tomcat服务器，不需要额外部署WAR包。配合Spring Data JPA可以快速完成数据库操作。

### Q2：什么是JPA？和MyBatis有什么区别？
**答：** JPA（Java Persistence API）是Java的ORM标准，Spring Data JPA是它的实现封装。写接口就能自动生成CRUD，适合快速开发。MyBatis需要手写SQL，灵活性更高但代码量更大。本项目业务简单，用JPA更高效。

### Q3：DTO和Entity有什么区别？
**答：** Entity对应数据库表结构，DTO面向接口契约。Entity可能包含敏感字段（如密码）或不适合直接展示的格式（如BigDecimal），DTO负责做数据转换和保护，解耦前后端。

### Q4：@Transactional有什么用？
**答：** 保证一组数据库操作的原子性。比如结算时涉及"插入订单+插入订单项+删除购物车"三步，任何一步失败都会全部回滚，不会出现数据不一致。

### Q5：前端用什么方式管理状态？
**答：** 使用React原生的Context + useState/useReducer管理全局状态。购物车、订单、登录信息通过Context共享给所有组件。对于更复杂的应用可以考虑Redux或Zustand。

### Q6：前后端分离的好处是什么？
**答：** ① 前后端可以并行开发，互不阻塞；② 一套后端可以服务多个前端（如PC网页、小程序、App）；③ 职责清晰，前端专注交互，后端专注业务和数据；④ 技术栈独立，可以分别升级。

---

## 六、项目目录速查

```
bookstore/
├── bookstore-backend/               ← 后端工程
│   ├── pom.xml                      ← Maven配置（依赖管理）
│   ├── src/main/java/com/bookstore/backend/
│   │   ├── BookstoreBackendApplication.java   ← 启动类（main方法在这里）
│   │   ├── config/CorsConfig.java             ← 跨域配置
│   │   ├── controller/                        ← 四个控制器
│   │   ├── service/                           ← 四个业务逻辑类（新增）
│   │   ├── dto/                               ← 数据传输对象
│   │   ├── entity/                            ← 五个实体类（对应五张表）
│   │   └── repository/                        ← 五个仓库接口
│   └── src/main/resources/
│       ├── application.properties   ← Spring Boot配置文件
│       └── schema.sql               ← 数据库建表脚本+初始数据
│
└── bookstore-frontend/              ← 前端工程
    ├── package.json                 ← npm依赖配置
    ├── vite.config.js               ← Vite配置（含代理）
    └── src/
        ├── api/api.js               ← 前后端通信唯一入口
        ├── App.jsx                  ← 根组件（路由配置）
        ├── context/CartContext.jsx  ← 全局状态管理
        ├── components/              ← 可复用组件（Layout, BookCard）
        └── pages/                   ← 页面级组件（首页、详情、购物车、订单等）
```

---

## 七、启动步骤（答辩前自己先跑一遍）

1. **启动MySQL**，确保能连接上 `localhost:3306`
2. **启动后端**：
   ```bash
   cd bookstore-backend
   ./mvnw spring-boot:run    # Mac/Linux
   mvnw.cmd spring-boot:run  # Windows
   ```
   看到 `Started BookstoreBackendApplication` 表示成功，端口8080。
3. **启动前端**：
   ```bash
   cd bookstore-frontend
   npm install    # 第一次需要安装依赖
   npm run dev    # 启动开发服务器
   ```
   看到 `Local: http://localhost:3000/` 表示成功。
4. 浏览器打开 `http://localhost:3000`，开始演示。

---

> 祝答辩顺利！如果有某个概念还是不理解，建议把对应部分的代码和本文档对照着看，多打断点调试几次就明白了。
