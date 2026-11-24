# 迭代三答辩准备 - pre3.md

> 评分标准：30分（功能10分 + 技术方案10分 + 代码质量5分 + 界面友好5分）
> 
> 本文件按技术方案评分标准（10分）组织，重点覆盖：
> - B.i: 前端工程构件化开发（4分）
> - B.ii: 后端分层架构 + 接口与实现分离 + 依赖注入（4分）
> - B.iii: ORM框架运用（2分）

---

## 项目概览

### 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端框架 | React + React Router | 组件化开发，路由管理 |
| UI组件库 | Ant Design | 美观的界面组件 |
| 构建工具 | Vite + npm | 快速构建和包管理 |
| 后端框架 | Spring Boot | 快速开发，自动配置 |
| ORM框架 | Spring Data JPA | 对象关系映射 |
| 数据库 | MySQL | 关系型数据库 |
| 前后端通信 | Fetch API | HTTP请求 |
| 用户认证 | Session | 服务器端会话管理 |

### 项目结构

**后端** (`bookstore-backend/src/main/java/com/bookstore/backend/`)：
```
controller/     ← 控制层：接收请求、返回响应
service/        ← 服务层：业务逻辑（接口+实现分离）
repository/     ← 数据层：JPA数据访问
entity/         ← 实体层：数据库表映射
dto/            ← 数据传输对象
util/           ← 工具类（AuthUtil）
config/         ← 配置类（CORS）
```

**前端** (`bookstore-frontend/src/`)：
```
pages/          ← 视图层：页面组件
components/     ← 组件层：可复用组件
api/            ← 服务层：API调用封装
context/        ← 全局状态管理
utils/          ← 工具函数
styles/         ← 样式文件
```

---

## B.i: 前端工程构件化开发（4分）

### Q1: 你的前端工程结构是怎样的？

**口语版：**
> "我的前端工程分为五个部分：pages放页面组件，比如登录页、首页、订单页；components放可复用组件，比如书籍卡片、布局组件；api放API调用封装；context放全局状态管理，比如购物车和登录状态；utils放工具函数，比如价格计算。"

**详细版：**
> "我的前端工程体现了构件化开发的原则，分为五个包：
> 
> 1. **pages（视图层）**：存放页面级组件，比如 Login.jsx、BookList.jsx、Cart.jsx、Orders.jsx、Statistics.jsx 等。每个页面对应一个路由。
> 
> 2. **components（组件层）**：存放可复用的UI组件，比如 BookCard.jsx（书籍卡片）、Layout.jsx（页面布局）。这些组件可以在多个页面中复用。
> 
> 3. **api（服务层）**：存放 API 调用封装，api.js 文件统一管理所有后端接口调用，提供了 fetchBooks()、addToCart()、checkoutOrder() 等方法。前端不再直接写 fetch 请求，而是调用这些封装好的函数。
> 
> 4. **context（状态管理）**：使用 React Context 管理全局状态，CartContext.jsx 管理购物车、订单和登录状态，SearchContext.jsx 管理搜索关键词。
> 
> 5. **utils（工具层）**：存放工具函数，比如 price.js 提供价格解析和计算功能。"

---

### Q2: 为什么要进行构件化开发？

**口语版：**
> "构件化开发可以提高代码复用性，比如书籍卡片组件可以在首页和管理页复用。同时也便于维护，修改一个组件不会影响其他组件。"

**详细版：**
> "构件化开发有三个主要好处：
> 
> 1. **代码复用**：比如 BookCard 组件在 BookList 和 BookManage 两个页面都使用，只需要写一次。
> 
> 2. **便于维护**：每个组件职责单一，修改书籍卡片的样式只需要改 BookCard.jsx，不会影响其他页面。
> 
> 3. **团队协作**：多人开发时，每个人可以负责不同的组件，互不干扰。"

---

### Q3: 你的前端路由是怎么配置的？

**口语版：**
> "使用 React Router，在 App.jsx 中配置路由。登录页是独立路由，其他页面放在 Layout 布局中作为嵌套路由。"

**详细版：**
> "在 App.jsx 中使用 React Router 的 Routes 和 Route 组件配置：
> 
> ```jsx
> <Routes>
>   <Route path=\"/\" element={<AppLayout />}>
>     <Route index element={<BookList />} />
>     <Route path=\"book/:id\" element={<BookDetail />} />
>     <Route path=\"cart\" element={<Cart />} />
>     <Route path=\"orders\" element={<Orders />} />
>     <Route path=\"statistics\" element={<Statistics />} />
>     <Route path=\"admin/books\" element={<BookManage />} />
>     <Route path=\"admin/users\" element={<UserManage />} />
>   </Route>
>   <Route path=\"/login\" element={<Login />} />
> </Routes>
> ```
> 
> 登录页是独立路由，其他页面放在 Layout 中，共享导航栏和侧边栏。"

---

### Q4: 前端如何管理用户登录状态？

**口语版：**
> "使用 React Context 管理登录状态，登录成功后保存到 localStorage，刷新页面时恢复。同时通过 Session 和后端保持同步。"

**详细版：**
> "登录状态管理分为两部分：
> 
> 1. **前端本地存储**：登录成功后，将用户信息（用户名、头像、角色）保存到 localStorage，刷新页面时从 localStorage 恢复登录状态。
> 
> 2. **后端 Session 验证**：刷新页面时，前端会调用 `/users/me` 接口验证后端 Session 是否有效。如果 Session 失效，前端会清除登录状态。
> 
> 3. **全局状态**：通过 CartContext 提供全局的 isLoggedIn、user、login、logout 等方法，任何组件都可以调用。"

---

### Q5: 前后端是怎么通信的？

**口语版：**
> "使用 Fetch API，前端请求通过 Vite 代理转发到后端。后端响应统一格式是 {code, message, data}，前端统一拆包。"

**详细版：**
> "前后端通过 Fetch API 通信：
> 
> 1. **请求转发**：Vite 配置了代理，前端请求 `/api/v1/*` 会转发到 `http://localhost:8080/api/v1/*`。
> 
> 2. **响应格式**：后端所有接口返回统一格式：
>    ```json
>    { \"code\": 200, \"message\": \"success\", \"data\": {...} }
>    ```
> 
> 3. **统一处理**：前端 api.js 中的 request 函数统一拆包，只返回 data，错误时抛出异常。
> 
> 4. **Cookie 传递**：请求时添加 `credentials: 'include'`，确保 Session Cookie 能正确传递。"

---

### Q6: 前端如何处理登录状态和后端Session同步？

**口语版：**
> "登录成功后前端保存用户信息到 localStorage，同时后端创建 Session。刷新页面时前端调用 `/users/me` 验证后端 Session 是否有效。请求时添加 `credentials: 'include'` 确保 Cookie 正确传递。"

**详细版：**
> "登录状态管理流程：
> 
> 1. **登录成功**：后端将用户名存入 Session，返回包含 Session ID 的 Cookie。前端将用户信息（用户名、角色、头像）保存到 localStorage。
> 
> 2. **请求时**：所有 API 请求添加 `credentials: 'include'`，确保浏览器自动发送 Session Cookie。
> 
> 3. **刷新页面**：前端从 localStorage 读取用户信息显示，但同时调用 `/users/me` 接口验证后端 Session 是否有效。如果 Session 过期，清除前端登录状态，跳转到登录页。
> 
> 4. **登出**：前端清除 localStorage，调用 `/users/logout` 接口，后端清除 Session。"

---

### Q7: 前端为什么不传递用户名？

**口语版：**
> "为了安全！如果前端传递用户名，攻击者可以修改前端代码伪造身份。现在用户名由后端从 Session 获取，前端无法伪造。"

**详细版：**
> "这是一个重要的安全改造：
> 
> **改造前（有漏洞）：**
> ```javascript
> // 前端发送用户名
> fetch('/cart', {
>   method: 'POST',
>   body: JSON.stringify({ username: '用户A', bookId: 1 })
> })
> ```
> 攻击者可以修改前端代码，把 username 改成其他用户，后端会错误处理其他用户的订单。
> 
> **改造后（安全）：**
> ```javascript
> // 前端不发送用户名
> fetch('/cart', {
>   method: 'POST',
>   body: JSON.stringify({ bookId: 1 }),
>   credentials: 'include'  // 发送Session Cookie
> })
> ```
> 后端从 Session 获取用户名：
> ```java
> String username = authUtil.getCurrentUsername(session);  // 从Session获取
> ```
> 因为 Session 在服务器端，攻击者无法修改，所以身份是安全的。"

---

## B.ii: 后端分层架构（4分）

### Q1: 后端工程的分层架构是怎样的？

**口语版：**
> "后端分为四层：Controller 接收请求，Service 处理业务逻辑，Repository 操作数据库，Entity 映射数据库表。另外还有 DTO 层封装数据传输。"

**详细版：**
> "后端采用标准的四层架构：
> 
> ```
> 请求进来
>     ↓
> ┌─────────────────┐
> │   Controller    │  控制层：接收 HTTP 请求，调用 Service，返回响应
> └────────┬────────┘
>          ↓
> ┌─────────────────┐
> │    Service      │  服务层：处理业务逻辑，管理事务（接口+实现分离）
> └────────┬────────┘
>          ↓
> ┌─────────────────┐
> │  Repository     │  数据层：JPA 数据访问，继承 JpaRepository
> └────────┬────────┘
>          ↓
> ┌─────────────────┐
> │     Entity      │  实体层：Java 对象和数据库表的映射
> └─────────────────┘
> ```
> 
> **各层文件：**
> 
> | 层级 | 文件示例 | 职责 |
> |------|---------|------|
> | Controller | BookController.java | 处理 `/books`、`/book/{id}` 请求 |
> | Service | BookService.java + BookServiceImpl.java | 查询书籍列表、详情 |
> | Repository | BookRepository.java | JPA 查询方法 |
> | Entity | Book.java | 对应 book 表 |
> | DTO | BookView.java | 书籍视图对象，屏蔽实体细节 |"

---

### Q2: 为什么需要分层架构？

**口语版：**
> "分层架构可以解耦，比如修改数据库只需要改 Repository 层，不会影响 Controller 和 Service。同时便于测试，比如可以单独测试 Service 层。"

**详细版：**
> "分层架构有三个核心好处：
> 
> 1. **关注点分离**：每层只做一件事。Controller 只处理 HTTP 请求，Service 只处理业务逻辑，Repository 只操作数据库。
> 
> 2. **易于维护**：如果要从 MySQL 换成 PostgreSQL，只需要改 Repository 和 Entity，Service 和 Controller 不需要动。
> 
> 3. **便于测试**：可以单独测试 Service 层，用 Mock 对象模拟 Repository，不需要连接数据库。"

---

### Q3: 什么是接口与实现分离？你的项目中是怎么体现的？

**口语版：**
> "接口定义方法签名，实现类提供具体逻辑。比如 BookService 是接口，定义了 getAllBooks 方法，BookServiceImpl 是实现类，提供具体实现。"

**详细版：**
> "接口与实现分离是面向对象的设计原则：
> 
> **接口**（BookService.java）：
> ```java
> public interface BookService {
>     ApiResponse getAllBooks();
>     ApiResponse getBookById(Long id);
> }
> ```
> 
> **实现类**（BookServiceImpl.java）：
> ```java
> @Service
> public class BookServiceImpl implements BookService {
>     @Autowired
>     private BookRepository bookRepository;
>     
>     @Override
>     public ApiResponse getAllBooks() {
>         List<Book> books = bookRepository.findAll();
>         return ApiResponse.success(books);
>     }
> }
> ```
> 
> **好处：**
> 
> 1. **解耦**：Controller 只依赖接口，不依赖实现类。
> 
> 2. **易于扩展**：如果需要换一种实现方式，只需要写新的实现类，Controller 不需要改。
> 
> 3. **便于测试**：可以用 Mock 对象替代真实实现进行测试。"

---

### Q4: Spring 依赖注入是什么？有什么好处？

**口语版：**
> "依赖注入就是让 Spring 容器自动创建和注入对象，不需要手动 new。好处是解耦，比如 Service 依赖 Repository，Spring 会自动注入。"

**详细版：**
> "依赖注入（DI）是 Spring 的核心特性：
> 
> **传统方式**（手动创建对象）：
> ```java
> public class BookServiceImpl {
>     private BookRepository bookRepository = new BookRepository();  // 手动创建
> }
> ```
> 
> **依赖注入方式**（Spring 自动注入）：
> ```java
> @Service
> public class BookServiceImpl implements BookService {
>     @Autowired
>     private BookRepository bookRepository;  // Spring 自动注入
> }
> ```
> 
> **好处：**
> 
> 1. **解耦**：Service 不需要知道 Repository 是怎么创建的，降低耦合度。
> 
> 2. **便于测试**：测试时可以注入 Mock 对象，而不是真实的 Repository。
> 
> 3. **统一管理**：所有对象由 Spring 容器管理，生命周期统一。"

---

### Q5: 你的项目中如何保证用户身份安全？（加分项！）

**口语版：**
> "使用 Session 管理用户身份，后端从 Session 获取用户名，不再信任前端传递的用户名。这样可以防止攻击者伪造其他用户身份。"

**详细版：**
> "【安全改造亮点】
> 
> **之前的问题**：前端在请求体中传递用户名，攻击者可以修改前端代码伪造其他用户身份。
> 
> **解决方案**：使用 Session 管理用户身份：
> 
> 1. **登录时**：后端验证用户名密码后，将用户名存入 Session：
>    ```java
>    session.setAttribute(\"loggedInUser\", username);
>    ```
> 
> 2. **后续请求**：后端从 Session 获取用户名，不再从前端接收：
>    ```java
>    String username = authUtil.getCurrentUsername(session);
>    ```
> 
> 3. **前端请求**：不再传递 username，请求时带上 Cookie：
>    ```javascript
>    credentials: 'include'  // 发送 Cookie
>    ```
> 
> **安全原理**：
> 
> - Session 存储在服务器端，用户无法修改
> - 即使攻击者修改前端代码，后端也不会信任，只认可 Session 中存储的真实用户
> - 删除购物车项时还会验证：购物车项的用户名必须与当前 Session 用户一致，否则返回 403"

---

### Q6: 为什么不用 Spring Security？（可能被追问）

**口语版：**
> "项目要求提到了 Spring Security，但考虑到项目规模较小，我选择用 Session + AuthUtil 实现认证。这样更简单直观，也能满足安全需求。"

**详细版：**
> "Spring Security 是功能强大的安全框架，但对于本项目：
> 
> 1. **Session 方案足够**：本项目主要是用户身份认证和权限控制，Session + AuthUtil 已经能满足需求。
> 
> 2. **学习成本低**：Session 机制更直观，答辩时容易解释清楚。
> 
> 3. **扩展性**：如果以后需要更复杂的安全功能（如 OAuth2、JWT），可以在现有基础上扩展。
> 
> 4. **安全保障**：通过 Session 管理用户身份，结合后端验证，已经能防止身份伪造攻击。"

---

### Q5: 后端如何验证用户身份？为什么不使用前端传递的用户名？

**口语版：**
> "后端使用 Session 管理用户身份。用户登录后，用户名存储在服务器端的 Session 中。后续请求时，后端从 Session 获取用户名，不信任前端传递的用户名，这样可以防止身份伪造。"

**详细版：**
> "用户身份验证流程：
> 
> 1. **登录时**：用户提交用户名和密码，验证通过后，后端将用户名存入 Session：
>    ```java
>    session.setAttribute("loggedInUser", "toothache");
>    ```
> 
> 2. **后续请求**：后端从 Session 获取用户名，不接受前端传递的用户名：
>    ```java
>    String username = authUtil.getCurrentUsername(session);  // 从Session获取
>    ```
> 
> 3. **安全性**：Session 存储在服务器端，攻击者无法修改。即使攻击者修改前端代码发送其他用户名，后端也不会信任。
> 
> **文件：** [AuthUtil.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/util/AuthUtil.java)"

---

### Q6: 后端如何实现管理员权限验证？

**口语版：**
> "管理员接口需要权限验证。后端从 Session 获取当前用户，检查其角色是否为 admin，如果不是则返回 403 禁止访问。"

**详细版：**
> "权限验证流程：
> 
> 1. **登录时**：用户角色（admin/customer）存储在数据库中，登录时一起查询。
> 
> 2. **管理员接口**：在 Controller 层调用 `authUtil.checkAdmin(session)` 验证权限：
>    ```java
>    @PostMapping
>    public ApiResponse addBook(@RequestBody BookRequest request, HttpSession session) {
>        authUtil.checkAdmin(session);  // 验证是否为管理员
>        return bookService.addBook(request);
>    }
>    ```
> 
> 3. **AuthUtil.checkAdmin()**：
>    ```java
>    public void checkAdmin(HttpSession session) {
>        String username = getCurrentUsername(session);
>        User user = userRepository.findByUsername(username);
>        if (!"admin".equals(user.getRole())) {
>            throw new RuntimeException("无权限访问");
>        }
>    }
>    ```
> 
> **保护的接口：** 添加/修改/删除书籍、禁用用户、获取所有订单、热销榜、消费榜等。"

---

### Q7: 密码是怎么加密存储的？

**口语版：**
> "使用 BCrypt 加密密码。注册时加密存储，登录时验证加密后的密码。同时兼容旧的明文密码，登录时自动升级为加密格式。"

**详细版：**
> "密码处理流程：
> 
> 1. **注册时**：使用 BCrypt 加密密码后存储：
>    ```java
>    user.setPassword(passwordUtil.encode(password));
>    userRepository.save(user);
>    ```
> 
> 2. **登录时**：验证加密后的密码：
>    ```java
>    passwordUtil.matches(inputPassword, storedPassword);
>    ```
> 
> 3. **兼容旧密码**：如果数据库中存在明文密码（之前项目遗留的），登录时会先进行明文比对，比对成功后自动升级为加密格式：
>    ```java
>    if (storedPassword.startsWith("$2a$")) {
>        passwordMatch = passwordUtil.matches(request.getPassword(), storedPassword);
>    } else {
>        passwordMatch = storedPassword.equals(request.getPassword());
>        if (passwordMatch) {
>            user.setPassword(passwordUtil.encode(request.getPassword()));
>            userRepository.save(user);
>        }
>    }
>    ```
> 
> **文件：** [PasswordUtil.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/util/PasswordUtil.java)"

---

### Q8: 后端启动时如何初始化测试账号？

**口语版：**
> "使用 CommandLineRunner 在启动时检查并创建测试账号。管理员账号 admin/admin123 和演示用户 toothache/123456 会自动创建，密码自动加密。"

**详细版：**
> "数据初始化流程：
> 
> 1. **创建 DataInitializer 类**：实现 CommandLineRunner 接口，在 run() 方法中初始化数据。
> 
> 2. **检查管理员账号**：
>    ```java
>    User admin = userRepository.findByUsername("admin");
>    if (admin == null) {
>        admin = new User();
>        admin.setUsername("admin");
>        admin.setPassword(passwordUtil.encode("admin123"));
>        admin.setRole("admin");
>        userRepository.save(admin);
>    }
>    ```
> 
> 3. **检查演示用户**：
>    ```java
>    User user = userRepository.findByUsername("toothache");
>    if (user == null) {
>        user = new User();
>        user.setUsername("toothache");
>        user.setPassword(passwordUtil.encode("123456"));
>        user.setRole("customer");
>        userRepository.save(user);
>    }
>    ```
> 
> 4. **自动升级密码**：如果账号存在但密码是明文，自动升级为加密格式。
> 
> **文件：** [DataInitializer.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/config/DataInitializer.java)"

---

## B.iii: ORM框架运用（2分）

### Q1: 你的实体类的 ID 是怎么生成的？

**口语版：**
> "使用 JPA 的 @GeneratedValue 注解，策略是 GenerationType.IDENTITY，由数据库自动生成自增主键。"

**详细版：**
> "所有实体类的 ID 都使用数据库自增主键：
> 
> ```java
> @Entity
> public class Book {
>     @Id
>     @GeneratedValue(strategy = GenerationType.IDENTITY)
>     private Long id;
> }
> ```
> 
> **GenerationType.IDENTITY**：
> 
> - 数据库自动生成 ID（MySQL 的 AUTO_INCREMENT）
> - 插入数据时不需要设置 ID，数据库会自动分配
> - 简单可靠，适合中小型项目"

---

### Q2: 哪些实体类之间设置了 cascade 关联操作？

**口语版：**
> "Order 和 OrderItem 之间设置了 cascade = CascadeType.ALL。保存订单时会自动保存订单项，删除订单时会自动删除订单项。"

**详细版：**
> "【Order → OrderItem：一对多关联】
> 
> ```java
> @Entity
> public class Order {
>     @OneToMany(mappedBy = \"order\", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
>     private List<OrderItem> orderItems = new ArrayList<>();
> }
> ```
> 
> ```java
> @Entity
> public class OrderItem {
>     @ManyToOne(fetch = FetchType.LAZY)
>     @JoinColumn(name = \"order_id\", nullable = false)
>     private Order order;
> }
> ```
> 
> **cascade = CascadeType.ALL 的效果**：
> 
> 1. **级联保存**：保存 Order 时，自动保存关联的 OrderItem
> 2. **级联删除**：删除 Order 时，自动删除关联的 OrderItem
> 3. **级联更新**：更新 Order 时，自动更新关联的 OrderItem
> 
> **orphanRemoval = true**：如果从 orderItems 集合中移除订单项，会自动删除数据库中的记录。"

---

### Q3: 各个实体间的关联关系是如何实现的？

**口语版：**
> "Order 和 OrderItem 是一对多，CartItem 和 Book 是多对一。使用 @OneToMany 和 @ManyToOne 注解实现。"

**详细版：**
> "项目中有两组关联关系：
> 
> **1. Order ↔ OrderItem（一对多）**
> 
> - 一个订单对应多个订单项
> - Order 端：`@OneToMany(mappedBy = \"order\", cascade = CascadeType.ALL)`
> - OrderItem 端：`@ManyToOne` + `@JoinColumn(name = \"order_id\")`
> 
> **2. CartItem ↔ Book（多对一）**
> 
> - 多个购物车项对应一本书
> - CartItem 端：`@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = \"book_id\")`
> - Book 端：没有反向关联（不需要）
> 
> **FetchType.LAZY**：延迟加载，只有访问关联对象时才查询数据库，提高性能。"

---

### Q4: 什么是 ORM？

**口语版：**
> "ORM 就是对象关系映射，把 Java 对象和数据库表对应起来，直接操作对象就能操作数据库，不用写 SQL。"

**详细版：**
> "ORM（Object-Relational Mapping）：
> 
> **映射关系**：
> 
> | Java 对象 | 数据库 |
> |----------|--------|
> | Class（类） | Table（表） |
> | Object（对象） | Row（行） |
> | Field（字段） | Column（列） |
> 
> **示例**：
> 
> ```java
> @Entity
> @Table(name = \"book\")
> public class Book {
>     @Id
>     private Long id;
>     private String title;
>     private String author;
> }
> ```
> 
> **好处：**
> 
> 1. **面向对象编程**：直接操作 Java 对象，不用写 SQL
> 2. **安全**：防止 SQL 注入（JPA 使用参数化查询）
> 3. **跨数据库**：换数据库时不需要改代码，JPA 自动适配"

---

### Q5: JPA 中的 @Transactional 注解有什么作用？

**口语版：**
> "@Transactional 用来管理事务，保证一组操作要么全部成功，要么全部回滚。比如下单时创建订单和扣减库存必须同时成功。"

**详细版：**
> "@Transactional 注解用来声明事务：
> 
> ```java
> @Service
> public class OrderServiceImpl {
>     @Override
>     @Transactional
>     public ApiResponse checkout(CheckoutRequest request) {
>         // 1. 创建订单
>         Order order = new Order();
>         orderRepository.save(order);
>         
>         // 2. 创建订单项
>         for (CartItem c : cartItems) {
>             OrderItem item = new OrderItem();
>             order.getOrderItems().add(item);
>         }
>         
>         // 3. 扣减库存
>         book.setStock(book.getStock() - quantity);
>         bookRepository.save(book);
>         
>         // 4. 清空购物车
>         cartItemRepository.deleteByUsername(username);
>     }
> }
> ```
> 
> **事务保证**：如果第3步扣减库存失败，第1、2步会回滚，数据库不会有脏数据。
> 
> **传播行为**：默认是 REQUIRED，如果当前没有事务就新建一个，如果有就加入当前事务。"

---

### Q6: 你的项目中如何防止 SQL 注入？

**口语版：**
> "使用 Spring Data JPA，它自动使用参数化查询，参数值不会被当作 SQL 语句执行，从而防止 SQL 注入。"

**详细版：**
> "JPA 默认使用参数化查询，自动防止 SQL 注入：
> 
> **方法名查询**：
> ```java
> List<Book> findByTitleContaining(String title);
> // 生成 SQL：SELECT * FROM book WHERE title LIKE ?
> ```
> 
> **自定义查询**：
> ```java
> @Query(\"SELECT o FROM Order o WHERE o.username = :username\")
> List<Order> findByUsername(@Param(\"username\") String username);
> ```
> 
> **原理**：参数绑定会将 `?` 替换为参数值，但不会当作 SQL 语句的一部分执行。即使传入恶意字符串，也只会作为参数值，不会被执行。"

---

## 代码质量（5分）

### Q1: 你的项目有测试代码吗？

**口语版：**
> "有，在 test 目录下有 BookServiceTest 和 OrderServiceTest，使用 JUnit 和 Mockito 进行单元测试。"

**详细版：**
> "测试文件位于 `src/test/java/com/bookstore/backend/service/`：
> 
> **BookServiceTest.java**：测试书籍查询功能
> - testGetAllBooks()
> - testGetBookById()
> 
> **OrderServiceTest.java**：测试订单创建和级联保存
> - testBuyNow()
> - testCheckout()
> 
> **测试方式**：使用 Mockito 模拟 Repository，不需要连接真实数据库。"

---

### Q2: 代码中有注释吗？

**口语版：**
> "有，每个类和关键方法都有注释，解释功能和设计思路。特别是安全改进部分，有详细的注释说明。"

**详细版：**
> "代码注释包括：
> 
> 1. **类注释**：每个 Controller、Service、Entity 都有类注释，说明职责。
> 
> 2. **方法注释**：关键业务方法有注释，说明功能。
> 
> 3. **安全注释**：Session 安全改造部分有详细注释，说明为什么这样设计。
> 
> 4. **JPA 注释**：实体关联关系有注释，说明 cascade 和 fetch 策略。"

---

## 功能演示要点（10分）

### 演示顺序

1. **登录**：使用普通用户（toothache/123456）和管理员（admin/admin123）分别登录，展示不同界面
2. **浏览书籍**：搜索书籍，查看详情
3. **加入购物车**：将书籍加入购物车
4. **下单**：结算购物车，展示订单生成
5. **订单管理**：搜索订单（时间范围、书籍名称）
6. **书籍管理（管理员）**：添加、编辑、删除书籍
7. **用户管理（管理员）**：禁用/解禁用户
8. **统计功能**：热销榜、消费榜、个人统计

### 功能清单对照

| 功能点 | 对应文件 | 演示要点 |
|--------|---------|---------|
| A.i 用户禁用/解禁 | UserManage.jsx | 禁用用户后，该用户无法登录 |
| B.i 登录验证 | Login.jsx | 未输入时提示 |
| B.ii 禁用用户提示 | UserServiceImpl.java | 提示"您的账号已经被禁用" |
| B.iii 角色差异 | Layout.jsx | 管理员显示管理菜单 |
| B.iv 注册表单 | Login.jsx | 用户名、密码、重复密码、邮箱 |
| B.v 注册验证 | UserServiceImpl.java | 用户名重复、密码一致、邮箱格式 |
| C.i 书籍列表 | BookManage.jsx | 显示书名、作者、封面、ISBN、库存 |
| C.ii 搜索 | BookManage.jsx | 按书名搜索 |
| C.iii 修改 | BookManage.jsx | 修改属性 |
| C.iv 添加删除 | BookManage.jsx | 添加新书、删除旧书 |
| D.i 浏览书籍 | BookList.jsx | 共用书籍列表 |
| D.ii 搜索 | BookList.jsx | 按书名搜索 |
| D.iii 详情 | BookDetail.jsx | 异步获取详情 |
| E.i 加入购物车 | Cart.jsx | 添加书籍到购物车 |
| E.ii 浏览购物车 | Cart.jsx | 查看购物车 |
| E.iii 购买扣库存 | OrderServiceImpl.java | 库存减少 |
| E.iv 生成订单 | Orders.jsx | 订单存入数据库 |
| F.i 用户订单 | Orders.jsx | 用户查看自己的订单 |
| F.ii 管理员订单 | Orders.jsx | 管理员查看所有订单 |
| F.iii 订单搜索 | Orders.jsx | 时间范围、书籍名称过滤 |
| G.i 热销榜 | Statistics.jsx | 按销量排序 |
| G.ii 消费榜 | Statistics.jsx | 按消费金额排序 |
| G.iii 个人统计 | Statistics.jsx | 个人购书统计 |

---

## 答辩技巧

### 1. 演示前准备

- 确保后端和前端都能正常启动
- 准备好测试账号：admin/admin123（管理员）、toothache/123456（普通用户）
- 打开浏览器开发者工具（F12），方便展示网络请求

### 2. 回答问题技巧

- **先给结论**：用一句话回答问题
- **再给解释**：举项目中的例子
- **最后讲原理**：说明为什么这样设计

### 3. 常见追问准备

| 可能追问 | 回答要点 |
|----------|---------|
| 为什么用 Session 而不是 JWT？ | Session 更简单，适合中小型项目；JWT 需要额外的 token 管理 |
| 你的 CORS 是怎么配置的？ | CorsConfig.java 配置了允许跨域请求，允许前端域名访问 |
| 如果并发下单怎么办？ | 使用 @Transactional 保证事务一致性；库存检查和扣减在同一个事务中 |
| 前端为什么用 Context 管理状态？ | Context 适合全局状态管理，避免 props 层层传递 |

---

## 登录完整流程（从前端到数据库）

### 场景：用户 toothache 使用密码 123456 登录

---

### 阶段一：前端发起登录请求

**文件：** [Login.jsx](file:///d:/桌面/bookstore/bookstore-frontend/src/pages/Login.jsx)

1. 用户在登录页面输入用户名 `toothache` 和密码 `123456`
2. 点击登录按钮，触发 `handleLogin` 函数
3. 调用 API：`loginUser(username, password)`

**文件：** [api.js](file:///d:/桌面/bookstore/bookstore-frontend/src/api/api.js#L130-L135)

4. 发送 HTTP POST 请求：
```
POST /api/v1/users/login
Content-Type: application/json
credentials: include

Body:
{
  "username": "toothache",
  "password": "123456"
}
```

---

### 阶段二：后端 Controller 接收请求

**文件：** [UserController.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/controller/UserController.java#L51-L63)

1. Spring Boot 接收到请求，路由到 `UserController.login()` 方法
2. 方法签名：`login(@RequestBody UserLoginRequest request, HttpSession session)`
3. 调用 `userService.login(request)` 处理业务逻辑

---

### 阶段三：后端 Service 处理登录逻辑

**文件：** [UserServiceImpl.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/service/UserServiceImpl.java#L95-L163)

1. **参数校验**：检查用户名和密码是否为空
2. **查询数据库**：`userRepository.findByUsername("toothache")`
3. **用户存在**：找到用户记录

**密码验证流程（关键）：**
```java
// 判断密码是否为BCrypt加密格式（以$2a$开头）
if (storedPassword.startsWith("$2a$")) {
    // BCrypt加密密码，使用matches方法验证
    passwordMatch = passwordUtil.matches(request.getPassword(), storedPassword);
} else {
    // 旧的明文密码，先进行明文比对
    passwordMatch = storedPassword.equals(request.getPassword());
    // 如果比对成功，自动升级为加密格式
    if (passwordMatch) {
        user.setPassword(passwordUtil.encode(request.getPassword()));
        userRepository.save(user);
    }
}
```

4. **检查账号状态**：如果用户被禁用，返回错误
5. **构建响应**：返回用户信息（不包含密码）

---

### 阶段四：数据库操作

**文件：** [UserRepository.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/repository/UserRepository.java)

1. **查询用户**：执行 SQL `SELECT * FROM user WHERE username = 'toothache'`
2. **密码验证**：
   - 如果密码已加密（`$2a$`开头）：使用 BCrypt 算法验证
   - 如果密码是明文：直接比对，比对成功后自动加密并保存

**数据库表结构（user表）：**
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    avatar VARCHAR(255),
    role VARCHAR(20) DEFAULT 'customer',
    disabled BOOLEAN DEFAULT FALSE,
    created_at DATETIME
);
```

**示例数据：**
| id | username | password | role | disabled |
|----|----------|----------|------|----------|
| 1 | admin | $2a$10$... | admin | false |
| 2 | toothache | $2a$10$... | customer | false |

---

### 阶段五：后端创建 Session

**文件：** [UserController.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/controller/UserController.java#L56-L59)

1. 登录成功后，将用户名存入 Session：
```java
if (response.getCode() == 200) {
    authUtil.setLoggedInUser(session, request.getUsername());
    // 等价于：session.setAttribute("loggedInUser", "toothache");
}
```

2. Spring Boot 自动生成 Session ID（如 `JSESSIONID=abc123xyz`）
3. 通过 HTTP 响应头返回 Cookie：
```
Set-Cookie: JSESSIONID=abc123xyz; Path=/; HttpOnly
```

---

### 阶段六：前端处理响应并保存登录状态

**文件：** [Login.jsx](file:///d:/桌面/bookstore/bookstore-frontend/src/pages/Login.jsx)

1. 接收后端响应：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "id": 2,
    "name": "toothache",
    "email": "toothache@example.com",
    "avatar": "https://via.placeholder.com/40",
    "role": "customer"
  }
}
```

2. 调用 `cartContext.login(userInfo)` 保存登录状态

**文件：** [CartContext.jsx](file:///d:/桌面/bookstore/bookstore-frontend/src/context/CartContext.jsx#L135-L141)

3. 更新 React 状态和 localStorage：
```javascript
const login = (userInfo) => {
    setIsLoggedIn(true);
    setUser(userInfo);
    localStorage.setItem('isLoggedIn', 'true');
    localStorage.setItem('user', JSON.stringify(userInfo));
};
```

4. 页面跳转到首页（`/home`）

---

### 阶段七：登录后的请求流程

**以获取购物车为例：**

**文件：** [api.js](file:///d:/桌面/bookstore/bookstore-frontend/src/api/api.js#L58-L60)

1. 前端发送请求：
```
GET /api/v1/cart
Cookie: JSESSIONID=abc123xyz
credentials: include
```

**文件：** [CartController.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/controller/CartController.java#L26)

2. 后端从 Session 获取用户名：
```java
String username = authUtil.getCurrentUsername(session);
// 等价于：String username = (String) session.getAttribute("loggedInUser");
// 结果：username = "toothache"
```

3. 调用 `cartService.getCart(username)` 查询购物车

**文件：** [CartItemRepository.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/repository/CartItemRepository.java)

4. 数据库查询：`SELECT * FROM cart_item WHERE username = 'toothache'`

5. 返回购物车数据给前端

---

### 完整流程图

```
用户浏览器                              服务器                                   数据库
    |                                     |                                     |
    |  ① 输入用户名密码                    |                                     |
    |  点击登录按钮                        |                                     |
    |                                     |                                     |
    |  ② POST /users/login                |                                     |
    |  {username:"toothache",             |                                     |
    |   password:"123456"}                |                                     |
    |─────────────────────────────────────>                                     |
    |                                     |                                     |
    |                                     |  ③ userRepository.findByUsername()  |
    |                                     |────────────────────────────────────> |
    |                                     |                                     |
    |                                     |  ④ SELECT * FROM user              |
    |                                     |     WHERE username='toothache'      |
    |                                     |<────────────────────────────────────|
    |                                     |                                     |
    |                                     |  ⑤ 密码验证（BCrypt）               |
    |                                     |                                     |
    |                                     |  ⑥ session.setAttribute()          |
    |                                     |     创建Session记录                  |
    |                                     |                                     |
    |  ⑦ 返回响应 + Set-Cookie            |                                     |
    |  {code:200, data:{userInfo}}        |                                     |
    |<─────────────────────────────────────|                                     |
    |                                     |                                     |
    |  ⑧ 保存到 localStorage              |                                     |
    |  跳转首页                           |                                     |
    |                                     |                                     |
    |  ⑨ GET /cart                        |                                     |
    |  Cookie: JSESSIONID=abc123          |                                     |
    |─────────────────────────────────────>                                     |
    |                                     |                                     |
    |                                     |  ⑩ session.getAttribute()           |
    |                                     |     获取用户名                       |
    |                                     |                                     |
    |                                     |  ⑪ 查询购物车                       |
    |                                     |────────────────────────────────────> |
    |                                     |                                     |
    |  ⑫ 返回购物车数据                   |                                     |
    |<─────────────────────────────────────|                                     |
```

---

### 关键安全机制总结

| 机制 | 实现方式 | 作用 |
|------|---------|------|
| **Session 管理** | Spring Boot 内置 Session | 存储用户身份，防止伪造 |
| **密码加密** | BCryptPasswordEncoder | 防止密码泄露 |
| **Cookie 设置** | HttpOnly 属性 | 防止 XSS 攻击窃取 Session ID |
| **前端不传递用户名** | API 请求中不含 username | 防止前端伪造身份 |
| **管理员权限验证** | AuthUtil.checkAdmin() | 保护管理员接口 |

---

## 注册完整流程（从前端到数据库）

### 场景：用户注册新账号 newuser / 123456 / newuser@example.com

---

### 阶段一：前端发起注册请求

**文件：** [Login.jsx](file:///d:/桌面/bookstore/bookstore-frontend/src/pages/Login.jsx)

1. 用户在登录页面点击"注册"链接，切换到注册表单
2. 输入用户名 `newuser`、密码 `123456`、邮箱 `newuser@example.com`
3. 点击注册按钮，触发注册逻辑
4. 调用 API：`registerUser(username, password, email)`

**文件：** [api.js](file:///d:/桌面/bookstore/bookstore-frontend/src/api/api.js#L143-L148)

5. 发送 HTTP POST 请求：
```
POST /api/v1/users/register
Content-Type: application/json

Body:
{
  "username": "newuser",
  "password": "123456",
  "email": "newuser@example.com"
}
```

---

### 阶段二：后端 Controller 接收请求

**文件：** [UserController.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/controller/UserController.java#L37-L41)

1. Spring Boot 接收到请求，路由到 `UserController.register()` 方法
2. 方法签名：`register(@RequestBody UserRegisterRequest request)`
3. **注意**：注册不需要 Session，因为还没有登录

---

### 阶段三：后端 Service 处理注册逻辑

**文件：** [UserServiceImpl.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/service/UserServiceImpl.java#L37-L87)

1. **参数校验**：检查用户名、密码、邮箱是否为空
2. **邮箱格式验证**：
```java
String emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
if (!request.getEmail().matches(emailRegex)) {
    return ApiResponse.error("邮箱格式不正确");
}
```

3. **用户名重复检查**：
```java
User existingUser = userRepository.findByUsername(request.getUsername());
if (existingUser != null) {
    return ApiResponse.error("用户名已存在");
}
```

4. **创建新用户**：
```java
User user = new User();
user.setUsername(request.getUsername());
user.setPassword(passwordUtil.encode(request.getPassword()));  // 密码加密存储！
user.setEmail(request.getEmail());
user.setRole("customer");
user.setDisabled(false);
user.setAvatar("https://via.placeholder.com/40");
user.setCreatedAt(LocalDateTime.now());
```

5. **保存到数据库**：`userRepository.save(user)`

---

### 阶段四：数据库操作

**文件：** [UserRepository.java](file:///d:/桌面/bookstore/bookstore-backend/src/main/java/com/bookstore/backend/repository/UserRepository.java)

1. **查询用户名是否存在**：执行 SQL `SELECT * FROM user WHERE username = 'newuser'`
2. **插入新用户**：执行 SQL
```sql
INSERT INTO user (username, password, email, avatar, role, disabled, created_at)
VALUES (
    'newuser',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye...',  -- BCrypt加密后的密码
    'newuser@example.com',
    'https://via.placeholder.com/40',
    'customer',
    FALSE,
    '2026-07-05 10:30:00'
);
```

**数据库表结构（user表）：**
```sql
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    avatar VARCHAR(255),
    role VARCHAR(20) DEFAULT 'customer',
    disabled BOOLEAN DEFAULT FALSE,
    created_at DATETIME
);
```

**插入后的数据：**
| id | username | password | email | role | disabled |
|----|----------|----------|-------|------|----------|
| 3 | newuser | $2a$10$... | newuser@example.com | customer | false |

---

### 阶段五：后端返回响应

1. 注册成功，返回响应：
```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

2. **注意**：注册不创建 Session！用户需要手动登录

---

### 阶段六：前端处理响应

**文件：** [Login.jsx](file:///d:/桌面/bookstore/bookstore-frontend/src/pages/Login.jsx)

1. 接收后端响应，显示"注册成功"提示
2. 页面跳转到登录表单，等待用户登录

---

### 完整流程图

```
用户浏览器                              服务器                                   数据库
    |                                     |                                     |
    |  ① 输入用户名密码邮箱                |                                     |
    |  点击注册按钮                        |                                     |
    |                                     |                                     |
    |  ② POST /users/register             |                                     |
    |  {username:"newuser",               |                                     |
    |   password:"123456",                |                                     |
    |   email:"newuser@example.com"}      |                                     |
    |─────────────────────────────────────>                                     |
    |                                     |                                     |
    |                                     |  ③ 参数校验 + 邮箱格式验证           |
    |                                     |                                     |
    |                                     |  ④ 查询用户名是否重复                |
    |                                     |────────────────────────────────────> |
    |                                     |                                     |
    |                                     |  ⑤ SELECT * FROM user              |
    |                                     |     WHERE username='newuser'        |
    |                                     |<────────────────────────────────────|
    |                                     |                                     |
    |                                     |  ⑥ 用户名不存在，创建新用户         |
    |                                     |     密码加密（BCrypt）              |
    |                                     |                                     |
    |                                     |  ⑦ INSERT INTO user                |
    |                                     |────────────────────────────────────> |
    |                                     |                                     |
    |  ⑧ 返回响应 {"code":200}            |                                     |
    |<─────────────────────────────────────|                                     |
    |                                     |                                     |
    |  ⑨ 显示注册成功，跳转登录页           |                                     |
    |                                     |                                     |
```

---

### 注册 vs 登录 关键区别

| 对比项 | 登录流程 | 注册流程 |
|--------|---------|---------|
| **API路径** | `POST /users/login` | `POST /users/register` |
| **请求参数** | `{username, password}` | `{username, password, email}` |
| **参数校验** | 检查非空 | 检查非空 + 邮箱格式验证 |
| **用户名处理** | 查询用户是否存在 | **检查用户名是否重复** |
| **密码处理** | **验证**密码（matches） | **加密**密码（encode） |
| **数据库操作** | SELECT 查询 | INSERT 插入 |
| **创建Session** | ✅ 创建Session | ❌ 不创建Session |
| **返回结果** | 返回用户信息 | 返回成功消息 |
| **后续操作** | 跳转首页 | 跳转登录页 |

---

## 总结

### 项目亮点

1. **安全设计**：使用 Session 管理用户身份，防止身份伪造攻击
2. **架构清晰**：前后端都采用分层架构，职责明确
3. **JPA 关联**：Order 和 OrderItem 的级联操作简化了代码
4. **构件化开发**：前端组件复用率高，便于维护

### 评分对照

| 评分项 | 得分 | 依据 |
|--------|------|------|
| A. 功能实现 | 10/10 | 所有功能点都已实现 |
| B.i 前端架构 | 4/4 | Component、Service、View、Util 结构清晰 |
| B.ii 后端架构 | 4/4 | Controller、Service、Repository、Entity，接口与实现分离，依赖注入 |
| B.iii ORM | 2/2 | JPA 使用正确，实体关联和 cascade 配置合理 |
| C. 代码质量 | 5/5 | 测试代码、注释完整 |
| D. 界面友好 | 5/5 | Ant Design，符合电商习惯 |

**总分：30/30**