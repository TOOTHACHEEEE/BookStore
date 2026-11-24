# 答辩口头回答准备

## A. 数据库访问（5分）

---

### A.i: 能够解释建立数据库连接和数据持久化操作的完整处理过程

**问题理解：**
数据库连接和数据持久化过程涉及多个步骤，从应用启动到数据读写完成。

**一、数据库连接过程（以 Spring Boot 启动为例）**

**第一步：读取配置**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/bookstore
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:******}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```
这告诉 Spring Boot：数据库在哪儿、叫什么名字、用什么账号登录。

**第二步：pom.xml 引入 JPA 依赖**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
有了这个依赖，Spring Boot 会自动配置数据库访问相关组件。

**第三步：自动创建数据源**
Spring Boot 利用这些配置，自动创建：
- **DataSource（数据源）**：管理数据库连接池
- **EntityManagerFactory**：JPA 的核心对象，用来管理实体
- **TransactionManager**：管理事务

**完整连接流程图：**
```
Spring Boot 启动
       ↓
读取 application.properties
       ↓
检测到 spring-boot-starter-data-jpa 依赖
       ↓
创建 DataSource（数据源）
       ↓
创建 EntityManagerFactory（JPA 核心）
       ↓
扫描 @Entity 注解的类（Book、User、CartItem 等）
       ↓
建立 Java 对象 ↔ 数据库表 的映射关系
       ↓
Spring 容器就绪，Repository 可用
```

---

**二、数据持久化操作过程（以"保存一本书"为例）**

> **注意：** 我们项目的 `BookService` 目前只有查询方法，没有保存方法。但我可以用 `CartService.addToCart()` 来演示数据写入数据库的完整过程。

**代码调用链（以添加购物车为例）：**
```java
// 1. Controller 调用 Service
cartController.addToCart(request)
    ↓
// 2. Service 调用 Repository
cartService.addToCart(request)
    ↓
// 3. Repository 是接口，JPA 实现类来处理
cartItemRepository.save(cartItem)
    ↓
// 4. JPA/Hibernate 执行以下步骤
```

**JPA 内部执行步骤（保存操作）：**

```
1. 开启事务
   BEGIN TRANSACTION;

2. 读取 CartItem 实体对象
   id: null, username: "toothache", bookId: 1, quantity: 1

3. 分析实体类上的注解
   @Entity → 这是一个要存库的类
   @Table(name="cart_item") → 存在 cart_item 表
   @Id → 主键字段
   @GeneratedValue → 主键自增

4. 生成 INSERT SQL
   INSERT INTO cart_item (username, book_id, quantity)
   VALUES ('toothache', 1, 1);

5. 通过 DataSource 从连接池获取数据库连接

6. 执行 SQL

7. 获取自动生成的主键 id = 1001

8. 提交事务
   COMMIT;

9. 把 id 值回填到 cartItem 对象的 id 字段
```

---

**三、查询操作的完整过程（以"查询书籍列表"为例）**

> 查询是"读"操作，持久化是"写"操作，两者过程类似但不完全相同。

**代码调用链：**
```java
// 1. Controller 调用 Service
bookController.getAllBooks()
    ↓
// 2. Service 调用 Repository
bookService.getAllBooks()
    ↓
// 3. Repository 是接口，JPA 实现类来处理
bookRepository.findAll()
    ↓
// 4. JPA/Hibernate 执行以下步骤
```

**JPA 内部执行步骤（查询操作）：**

```
1. 开启事务

2. 生成 SELECT SQL（根据方法名自动生成）
   SELECT id, title, author, price, description, cover, stock
   FROM book;

3. 通过 DataSource 获取数据库连接

4. 执行 SELECT SQL

5. 遍历结果集，每一行创建一个 Book 对象：
   Row 1 → Book{id=1, title="三体", author="刘慈欣", price=68.00}
   Row 2 → Book{id=2, title="流浪地球", author="刘慈欣", price=45.00}

6. 返回 List<Book> 集合

7. 提交事务
```

**四、关键组件协作总结**

| 组件 | 作用 |
|------|------|
| DataSource | 管理数据库连接池，按需创建/复用连接 |
| EntityManager | JPA 核心，执行 CRUD 操作 |
| @Entity + @Table | 告诉 JPA 哪个类对应哪张表 |
| @Id + @GeneratedValue | 告诉 JPA 哪个是主键、如何生成 |
| TransactionManager | 保证一组操作在同一个事务中执行 |
| Repository 接口 | 我们定义的"能力"，JPA 是实现者 |

---

### A.ii: Repository 中的方法是怎么变成 SQL 的？

**详细解释：**

我们定义了一个接口叫 `UserRepository`：

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

这个接口继承了 `JpaRepository`，Spring Data JPA 会自动帮我们创建实现类。

关键是 `findByUsername` 这个方法名——Spring Data JPA 会根据方法名**自动解析**并生成对应的 SQL：

| 方法名 | 自动生成的 SQL |
|--------|---------------|
| `findByUsername(String username)` | `SELECT * FROM user WHERE username = ?` |
| `findById(Long id)` | `SELECT * FROM user WHERE id = ?` |
| `findAll()` | `SELECT * FROM user` |
| `save(User user)` | `INSERT INTO user ...` 或 `UPDATE user ...` |
| `deleteById(Long id)` | `DELETE FROM user WHERE id = ?` |

**好处是什么？**
- 不用自己写 SQL
- 方法名就是语义，看方法名就知道干什么
- 如果需要定制查询，只要按规则命名方法就行

---

### A.iii: 为什么要抽象成实体类？

**详细解释：**

实体类（Entity）就是数据库表在 Java 代码里的"分身"。

比如数据库里有一张 `book` 表：

| id | title | author | price |
|----|-------|--------|-------|
| 1 | 三体 | 刘慈欣 | 68.00 |

我们创建一个 `Book.java` 实体类来对应这张表：

```java
@Entity
@Table(name = "book")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private Double price;
    // getter/setter...
}
```

**为什么要这么做？**

1. **面向对象**：直接操作 Java 对象，不用写 SQL
2. **安全**：可以在 Entity 里控制哪些字段能暴露、哪些不能
3. **维护方便**：改数据库字段时，只要改 Entity，不用到处找 SQL

---

## B. 功能（5分）

> 功能部分主要是演示，不需要口头解释。但你要能现场操作展示：
> - 登录功能
> - 书籍列表
> - 书籍详情
> - 加入购物车
> - 下订单
> - 页面联动（购物车→订单）

---

## C. 前后端集成（5分）

---

### C.i: JSON 数据格式是怎么设计的？

**详细解释：**

后端定义了一个统一的响应格式 `ApiResponse`，所有接口都返回这个格式：

```java
public class ApiResponse {
    private int code;      // 状态码，200表示成功
    private String message; // 提示信息
    private Object data;    // 实际要返回的数据
}
```

前端收到的 JSON 长这样：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {"id": 1, "title": "三体", "price": 68.00},
    {"id": 2, "title": "流浪地球", "price": 45.00}
  ]
}
```

前端只需要从 `data` 字段取数据就行，格式统一，方便处理。

---

### C.ii: 前后端通信的完整过程？

**详细解释（以"查看书籍列表"为例）：**

1. **前端发请求**
   用户打开书籍页面，前端调用 `fetchBooks()`，用 fetch API 发送 GET 请求到 `http://localhost:8080/api/v1/books`

2. **后端 Controller 接收**
   `BookController` 的 `getAllBooks()` 方法收到请求

3. **调用 Service 处理业务**
   Controller 调用 `bookService.getAllBooks()`

4. **Service 调用 Repository 查数据库**
   `bookService` 调用 `bookRepository.findAll()`
   JPA 自动生成 SQL：`SELECT * FROM book`
   数据库返回书籍数据

5. **Service 组装返回数据**
   把数据库查到的 Book 实体转成 BookView（DTO）
   封装成 `ApiResponse.success(views)` 返回

6. **前端收到 JSON**
   fetch 收到响应 JSON
   解析 JSON，取出 `data` 字段的数据
   调用 `setBooks(data)` 更新状态

7. **前端渲染页面**
   React 检测到状态变化
   自动重新渲染页面，展示书籍列表

**背诵一句话：** 前端发请求 → Controller 接 → Service 处理 → Repository 查库 → 返回 JSON → 前端渲染

---

## D. 系统架构（5分）

---

### D.i: 前端工程结构是怎样的？为什么这么分？

**详细解释：**

我们的前端目录结构是这样的：

```
src/
├── components/    ← 公共组件，可以被多个页面复用
├── pages/         ← 页面组件，一个文件对应一个页面
├── api/           ← 所有接口调用，统一管理
├── context/       ← React Context，跨组件共享数据
├── styles/        ← CSS 样式文件
└── App.jsx        ← 根组件，定义路由
```

**为什么这么分？**

| 目录 | 作用 | 举例 |
|------|------|------|
| `components/` | 可复用组件 | BookCard（书籍卡片）、Layout（布局） |
| `pages/` | 页面组件 | BookList（书籍列表页）、Cart（购物车页） |
| `api/` | 接口管理 | 所有 fetch 调用都放这里 |
| `context/` | 共享状态 | 购物车数据、用户登录状态 |
| `styles/` | 样式管理 | 统一的 CSS 文件 |

**好处是：**
- 代码复用：BookCard 写一次，可以在列表页和详情页用
- 易于维护：接口改了就改 api/ 目录，不用到处找
- 职责清晰：一个地方管一件事

---

### D.ii: 后端分层架构是怎样的？

**详细解释：**

后端采用标准三层架构：

```
请求进来
    ↓
┌─────────────────┐
│   Controller    │  控制层：接收请求、返回响应
└────────┬────────┘
         ↓
┌─────────────────┐
│    Service      │  业务层：处理业务逻辑、管理事务
└────────┬────────┘
         ↓
┌─────────────────┐
│  Repository     │  数据层：操作数据库
└────────┬────────┘
         ↓
┌─────────────────┐
│     Entity      │  实体层：Java 对象和数据库表的映射
└─────────────────┘
```

**各层职责：**

| 层级 | 职责 | 我们项目的文件 |
|------|------|---------------|
| Controller | 接收 HTTP 请求，调用 Service，返回响应 | BookController.java |
| Service | 处理业务逻辑，比如加入购物车要检查库存 | BookService.java |
| Repository | 数据访问，定义增删改查方法 | BookRepository.java |
| Entity | 数据库表的 Java 对象映射 | Book.java |

---

### D.iii: 接口与实现分离是什么意思？

**详细解释：**

我们的 Repository 是一个**接口**：

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

但我们没有写实现类！JPA 会自动创建实现类。

**为什么要这样设计？**

就好比空调和空调遥控器的接口：
- 空调（接口）定义：制冷、制热、调温度
- 格力空调（实现）：格力的具体实现
- 美的空调（实现）：美的的具体实现

如果格力空调坏了，换美的空调也能用，因为它们都遵循同一个接口。

**代码里的好处：**
- Repository 是接口，定义数据访问的"能力"
- JPA 是实现，具体操作数据库
- 如果以后要从 MySQL 换成 PostgreSQL，只需要换 JPA 的实现，不用改接口，也不用改 Service 代码

---

## 可能被问到的扩展问题

---

### Q: 为什么购物车要存数据库而不是内存？

**回答：**

内存就是变量，关掉程序就没了。数据库是存在硬盘里的。

购物车的用户体验应该是：用户把书加入购物车 → 退出登录 → 明天再登录 → 购物车里的书还在。

如果存内存里（Java 变量），后端一重启数据就没了。存数据库，后端重启也没关系，数据还在 MySQL 里。

---

### Q: Service 层的作用是什么？

**回答：**

Service 层负责**业务逻辑**。

比如"加入购物车"这个操作：
- 检查这本书是否已经在购物车里
- 如果在，增加数量；不在，新增一条记录
- 返回成功或失败

这些是业务逻辑，不是简单的"存数据"。

Controller 只负责接收请求（"我要加入购物车"）和返回响应（"好的，成功了"），具体的业务处理交给 Service。

---

### Q: 为什么要用 Session 来管理用户身份，而不是从前端传递用户名？

**回答：**

**问题场景：**
如果前端在请求体中传递用户名，比如：
```json
{
  "username": "用户A",
  "bookId": 1
}
```

攻击者可以修改前端代码，发送：
```json
{
  "username": "用户B",  // 伪造其他用户的身份！
  "bookId": 1
}
```

这样攻击者就能以其他用户的名义下单，这就是**身份伪造攻击**。

**解决方案：**
使用后端 Session 来管理用户身份：

1. **登录时**，后端验证用户名密码后，将用户名存入 Session：
```java
// UserController.login()
session.setAttribute("loggedInUser", username);
```

2. **后续请求**，后端从 Session 获取当前用户，不再信任前端：
```java
// OrderController.checkout()
String username = authUtil.getCurrentUsername(session);  // 从Session获取
request.setUsername(username);  // 由后端设置
```

3. **前端请求**，不再传递 username：
```javascript
// api.js - 前端不再发送username
export function checkoutOrder() {
  return request('/orders/checkout', {
    method: 'POST',
    body: {},  // 没有username了！
    credentials: 'include'  // 但要发送Cookie
  })
}
```

**安全原理：**
- Session 由后端创建，存储在服务器端
- 用户无法伪造 Session ID（除非窃取 Cookie）
- 即使攻击者修改前端代码，也无法伪造其他用户的身份，因为后端从 Session 获取的是服务器认可的登录用户

---

### Q: 什么是会话（Session）？它和 Cookie 有什么关系？

**回答：**

**Cookie**：
- 存储在用户浏览器中的小文件
- 可以保存少量数据（如登录状态标识）
- 每次请求都会自动发送到服务器

**Session**：
- 存储在服务器端的数据
- 每个用户有独立的 Session 数据
- 通过 Cookie 中的 Session ID 来识别是哪个用户

**工作流程：**

```
1. 用户登录
   前端 → 后端：用户名、密码
   后端 → Session：创建Session，存储用户信息
   后端 → 前端：返回Cookie（包含Session ID）

2. 后续请求
   前端 → 后端：请求 + Cookie
   后端：根据Cookie中的Session ID，找到对应的Session
   后端：从Session中获取用户信息
   后端：处理请求
```

**我们的实现：**

```java
// 登录时存入Session
session.setAttribute("loggedInUser", username);

// 后续请求从Session获取
String username = (String) session.getAttribute("loggedInUser");
```

---

### Q: 如果用户禁用了浏览器 Cookie，会怎么样？

**回答：**

如果用户禁用了 Cookie，Session 机制会失效，因为：
- 每次请求都无法携带 Session ID
- 后端无法识别用户

**解决方案：**

1. **提示用户开启 Cookie**：大多数网站的做法
2. **使用 URL 重写**：把 Session ID 放在 URL 里（不推荐，不安全）
3. **使用 Token（如 JWT）**：把用户信息加密后存在前端，每次请求携带

对于学生作业项目，要求用户开启 Cookie 是合理的。

---

### Q: 你的项目中如何防止 SQL 注入？

**回答：**

我的项目使用 **Spring Data JPA**，它默认使用**参数化查询**，自动防止 SQL 注入：

**1. Repository 方法名自动生成 SQL：**
```java
List<Book> findByTitleContaining(String title);
// JPA 自动生成：SELECT * FROM book WHERE title LIKE ?
```

**2. 自定义 JPQL 查询使用参数绑定：**
```java
@Query("SELECT o FROM Order o WHERE o.username = :username")
List<Order> findByUsername(@Param("username") String username);
```

**原理：**
参数绑定会将 `?` 替换为参数值，但不会当作 SQL 语句的一部分执行，从而防止注入。

**不安全的写法（JPA 中不推荐）：**
```java
// 危险！不要这样做！
@Query("SELECT o FROM Order o WHERE o.username = '" + username + "'")
```

---

### Q: 你的项目中如何处理用户输入验证？

**回答：**

**前端验证：**
```javascript
// Login.jsx - 注册表单验证
<Form.Item
  name="username"
  rules={[
    { required: true, message: '请输入用户名' },
    { min: 3, message: '用户名至少3个字符' },
  ]}
>
```

```javascript
// 邮箱格式验证
const validateEmail = (_, value) => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRegex.test(value)) {
    return Promise.reject(new Error('邮箱格式不正确'));
  }
  return Promise.resolve();
};
```

**后端验证：**
```java
// UserServiceImpl.register()
if (request.getUsername() == null || request.getUsername().isEmpty()) {
    return ApiResponse.error("用户名不能为空");
}

// 邮箱格式验证
String emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
if (!request.getEmail().matches(emailRegex)) {
    return ApiResponse.error("邮箱格式不正确");
}

// 用户名重复检查
User existingUser = userRepository.findByUsername(request.getUsername());
if (existingUser != null) {
    return ApiResponse.error("用户名已存在");
}
```

**双重验证的好处：**
- 前端验证：提升用户体验，快速反馈
- 后端验证：确保安全，防止绕过前端验证

---

### Q: 为什么要在购物车删除时验证用户名？

**回答：**

**问题场景：**
用户A加入购物车一件商品，获得购物车项ID为1。
攻击者猜测URL，发送删除请求 `DELETE /cart/1`。

如果后端不验证用户名，就会直接删除用户A的购物车项！

**我们的实现：**

```java
// CartServiceImpl.removeFromCart()
public ApiResponse removeFromCart(Long id, String username) {
    // 1. 检查用户是否登录
    if (username == null) {
        return new ApiResponse(401, "用户未登录", null);
    }

    // 2. 检查购物车项是否存在
    Optional<CartItem> itemOpt = cartItemRepository.findById(id);
    if (itemOpt.isEmpty()) {
        return new ApiResponse(404, "购物车项不存在", null);
    }

    // 3. 【关键】验证购物车项是否属于当前用户
    CartItem item = itemOpt.get();
    if (!item.getUsername().equals(username)) {
        return new ApiResponse(403, "无权删除其他用户的购物车项", null);
    }

    // 4. 执行删除
    cartItemRepository.deleteById(id);
    return ApiResponse.success("已删除");
}
```

**安全原理：**
即使攻击者知道购物车项ID，也无法删除其他用户的购物车项，因为后端会验证购物车项的 username 是否与当前 Session 用户匹配。

---

## 附录：Session 相关代码

### AuthUtil.java
```java
@Component
public class AuthUtil {

    private static final String SESSION_USER_KEY = "loggedInUser";

    /**
     * 从Session中获取当前登录用户的用户名
     */
    public String getCurrentUsername(HttpSession session) {
        String username = (String) session.getAttribute(SESSION_USER_KEY);
        if (username == null || username.isEmpty()) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        return username;
    }

    /**
     * 将用户登录信息存入Session
     */
    public void setLoggedInUser(HttpSession session, String username) {
        session.setAttribute(SESSION_USER_KEY, username);
    }

    /**
     * 清除登录信息（登出）
     */
    public void logout(HttpSession session) {
        session.removeAttribute(SESSION_USER_KEY);
    }
}
```

### 前端请求配置
```javascript
async function request(path, options = {}) {
  const config = {
    method: options.method || 'GET',
    credentials: 'include',  // 关键！发送Cookie给后端
    // ...
  };
}
```

**为什么需要 `credentials: 'include'`？**

因为浏览器的同源策略默认不发送 Cookie，需要显式声明才会发送。这个配置告诉浏览器：对于跨域请求，也要带上 Cookie。

---

### Q: 为什么要用 DTO/View 而不是直接返回 Entity？

**回答：**

Entity（实体类）直接返回有三个问题：

1. **安全问题**：Entity 可能包含敏感字段，比如 User 实体有 password 字段，直接返回会把密码暴露给前端

2. **不需要的字段**：Entity 可能有很多字段，前端只需要显示 title、author、price，不需要 description、stock 等

3. **耦合问题**：如果数据库表结构改了，Entity 就变了，前端拿到的数据也会变

所以我们把 Entity 转成 View（也叫 DTO），只返回前端需要的字段：

```java
// Entity：数据库表的所有字段
public class Book {
    private Long id;
    private String title;
    private String author;
    private String description;
    private Double price;
    private Integer stock;
    private String passwordHash; // 敏感字段
}

// View：只给前端看这些
public class BookView {
    private Long id;
    private String title;
    private String author;
    private Double price;
    // 没有 passwordHash，没有 stock
}
```

---

### Q: 事务管理在哪里做的？

**回答：**

在 Service 层，用 `@Transactional` 注解。

比如"下订单"操作：
1. 创建订单记录
2. 扣减库存
3. 清空购物车

这三个操作必须**同时成功或同时失败**，否则会出现数据不一致（比如订单创建了但库存没扣）。

```java
@Service
public class OrderService {

    @Transactional  // 加了这个注解，这整个方法里的操作会自动管理事务
    public ApiResponse checkout(String username) {
        // 创建订单
        orderRepository.save(order);

        // 扣库存
        bookRepository.updateStock(...);

        // 清空购物车
        cartRepository.deleteByUsername(username);

        return ApiResponse.success();
    }
}
```

加了 `@Transactional` 之后，如果任何一步出错，整个操作都会回滚，不会留下"半成品"数据。

---

## E. 验收迭代新增问题

### E.i: 为什么需要至少分为控制器层、服务层、数据访问对象层、实体层？

**回答：**

分层架构的核心思想是**职责分离**，每层只做一件事，互不干扰。

| 层级 | 职责 | 举例 |
|------|------|------|
| Controller | 接收HTTP请求，返回响应 | 告诉前端"请求收到了" |
| Service | 处理业务逻辑 | 计算订单金额、检查库存 |
| Repository | 操作数据库 | 执行SQL语句 |
| Entity | Java对象和数据库表映射 | Book类对应book表 |

**好处：**
1. **可维护性**：改业务逻辑只需要动Service层，不用改Controller
2. **可测试性**：可以单独测试Service层，不需要启动整个应用
3. **可扩展性**：要加新功能，只需加新的Service和Controller，不用改数据库层
4. **解耦合**：各层之间只通过接口通信，具体实现可以随时换

---

### E.ii: 为什么要进行接口与实现分离？

**回答：**

接口与实现分离就是"说的和做的分开"。

比如我们的Service层：
- `BookService.java` 是**接口**，只定义"能做什么"
- `BookServiceImpl.java` 是**实现类**，实现"具体怎么做"

```java
// 接口：定义能力
public interface BookService {
    ApiResponse getAllBooks();
    ApiResponse getBookById(Long id);
}

// 实现类：具体实现
@Service
public class BookServiceImpl implements BookService {
    @Override
    public ApiResponse getAllBooks() {
        // 具体实现...
    }
}
```

**好处：**
1. **解耦**：Controller依赖接口，不依赖具体实现
2. **可替换**：如果要加缓存，写个 `BookServiceCacheImpl` 就能替换
3. **可测试**：单元测试时可以Mock接口，不用真的操作数据库

---

### E.iii: Spring依赖注入是指什么？依赖注入有什么好处？

**回答：**

依赖注入（DI）就是**Spring容器帮我们创建对象并注入到需要的地方**，不用自己new。

```java
// 不用依赖注入：要自己new对象
public class BookController {
    private BookService bookService = new BookServiceImpl();  // 手动创建
}

// 使用依赖注入：Spring自动注入
public class BookController {
    @Autowired  // 告诉Spring：把BookService的实例注入到这里
    private BookService bookService;  // 不用自己new
}
```

**好处：**
1. **松耦合**：Controller不知道BookService是哪个实现类
2. **易于测试**：测试时可以注入Mock对象
3. **统一管理**：Spring容器管理所有对象的生命周期
4. **减少样板代码**：不用到处写new

---

### E.iv: 实体类的ID是如何生成的？

**回答：**

我们使用 `@GeneratedValue(strategy = GenerationType.IDENTITY)` 来生成主键。

```java
@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
```

**工作原理：**
- `IDENTITY` 策略：让数据库自动生成主键
- MySQL中会自动使用 `AUTO_INCREMENT`
- 插入数据时不用手动设置id，数据库会自动分配

**其他策略对比：**

| 策略 | 说明 |
|------|------|
| `IDENTITY` | 数据库自增，简单可靠（我们用的） |
| `SEQUENCE` | 使用数据库序列，Oracle常用 |
| `TABLE` | 使用一张表来管理主键，通用但性能差 |
| `AUTO` | 让JPA自动选择策略 |

---

### E.v: 哪些实体类之间设置了cascade关联操作？

**回答：**

我们在 `Order` 和 `OrderItem` 之间设置了 `cascade = CascadeType.ALL`。

```java
@Entity
public class Order {
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();
}
```

**cascade = CascadeType.ALL 的效果：**
- 保存订单时，自动保存所有关联的订单项（INSERT）
- 更新订单时，自动更新所有关联的订单项（UPDATE）
- 删除订单时，自动删除所有关联的订单项（DELETE）

**为什么要这样设计？**
- 订单和订单项是"主从关系"，订单项不能脱离订单存在
- 用cascade可以简化代码，不用手动保存每个订单项

---

### E.vi: 各个实体间的关联关系是如何实现的？

**回答：**

我们项目中有以下关联关系：

**1. Order → OrderItem（一对多）**
```java
// Order.java - 一的一方
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> orderItems;

// OrderItem.java - 多的一方
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id")
private Order order;
```

**2. CartItem → Book（多对一）**
```java
// CartItem.java - 多的一方
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "book_id")
private Book book;
```

**关联关系总结：**

| 关系类型 | 实体 | 注解 |
|---------|------|------|
| 一对多 | Order → OrderItem | @OneToMany + @ManyToOne |
| 多对一 | CartItem → Book | @ManyToOne |

**@JoinColumn的作用：**
- 指定外键列名，比如 `@JoinColumn(name = "order_id")` 表示外键列是 `order_id`
- 如果不指定，JPA会自动生成列名，格式是"关联实体名_主键名"

**fetch = FetchType.LAZY的作用：**
- 懒加载，只有真正用到关联对象时才查询数据库
- 比如查询订单时，不会立即查询订单项，只有调用 `order.getOrderItems()` 时才查询

---

### E.vii: 什么是ORM？

**回答：**

ORM（Object-Relational Mapping）就是**对象关系映射**，把Java对象和数据库表对应起来。

**不用ORM（原生SQL）：**
```java
String sql = "SELECT * FROM book WHERE id = 1";
ResultSet rs = statement.executeQuery(sql);
Book book = new Book();
book.setId(rs.getLong("id"));
book.setTitle(rs.getString("title"));
```

**用ORM（JPA）：**
```java
Book book = bookRepository.findById(1L).orElse(null);
```

**ORM的好处：**
1. **不用写SQL**：JPA自动生成SQL
2. **面向对象**：操作Java对象，不是操作表
3. **跨数据库**：换数据库不用改代码
4. **自动管理事务**：`@Transactional` 注解自动管理

---

### E.viii: JPA的生命周期回调有哪些？

**回答：**

JPA提供了几个注解来标记实体生命周期的回调方法：

| 注解 | 触发时机 |
|------|---------|
| `@PrePersist` | 实体保存到数据库之前 |
| `@PostPersist` | 实体保存到数据库之后 |
| `@PreUpdate` | 实体更新到数据库之前 |
| `@PostUpdate` | 实体更新到数据库之后 |
| `@PreRemove` | 实体从数据库删除之前 |
| `@PostRemove` | 实体从数据库删除之后 |

**使用示例：**
```java
@Entity
public class Book {
    @PrePersist
    protected void onCreate() {
        // 保存前设置创建时间
        this.createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        // 更新前设置更新时间
        this.updatedAt = LocalDateTime.now();
    }
}
```

---

### E.ix: @Transactional的传播行为有哪些？

**回答：**

`@Transactional` 的传播行为决定了事务如何在方法间传播：

| 传播行为 | 说明 |
|---------|------|
| `REQUIRED` | 如果当前有事务就加入，没有就新建（默认） |
| `REQUIRES_NEW` | 总是新建一个事务，挂起当前事务 |
| `SUPPORTS` | 如果有事务就加入，没有就以非事务方式执行 |
| `NOT_SUPPORTED` | 以非事务方式执行，挂起当前事务 |
| `MANDATORY` | 必须在事务中执行，否则抛出异常 |
| `NEVER` | 必须不在事务中执行，否则抛出异常 |

**使用示例：**
```java
@Transactional(propagation = Propagation.REQUIRED)  // 默认
public void checkout() { ... }
```

---

### E.x: 前端结构是否符合Component、Service、View、Util的要求？

**回答：**

是的，我们的前端结构符合要求：

```
src/
├── components/    ← Component：公共组件（BookCard、Layout）
├── pages/         ← View：页面组件（BookList、Cart、Orders）
├── api/           ← Service：接口服务（api.js）
├── utils/         ← Util：工具函数（price.js）
├── context/       ← 状态管理（CartContext、SearchContext）
└── styles/        ← 样式文件
```

| 目录 | 对应要求 | 说明 |
|------|---------|------|
| `components/` | Component | 可复用的UI组件 |
| `pages/` | View | 页面级组件 |
| `api/` | Service | 接口调用服务 |
| `utils/` | Util | 工具函数 |
