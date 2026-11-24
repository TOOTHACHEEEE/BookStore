# Java + Spring + 数据库 核心复习问答总结

> 整理自 L25-L34 复习问答与讲义正文，涵盖 JVM、Java 核心、Spring Boot、数据库、JDBC、JPA/ORM 等主题。

---

## 一、JVM 与 Java 核心

### Q1：类加载的「加载 → 链接 → 初始化」三大阶段各做什么？

类加载是 JVM 将 `.class` 字节码文件转换为内存中可使用的 `Class` 对象的过程，共分三个阶段：

**1. 加载（Loading）**
- JVM 按 classpath 路径查找 `.class` 文件，将静态的字节流读入内存
- 在方法区（元空间）中创建该类的运行时数据结构
- 在堆中生成对应的 `java.lang.Class` 对象，作为访问方法区类数据的入口
- 加载来源可以是 `.class` 文件、jar 包、网络流、动态生成的字节码等

**2. 链接（Linking）——再细分为三步**
- **验证（Verify）**：检查字节码合法性——魔数 `0xCAFEBABE`、版本号、方法签名、类型安全、不会访问越界等。如果自行篡改 `.class` 文件，验证阶段会抛出 `VerifyError`
- **准备（Prepare）**：为类的 `static` 字段分配内存，并赋予 JVM 规定的**零值**（`int` 为 0、`boolean` 为 `false`、对象引用为 `null`）。注意：此时赋的是零值，不是代码里写的初始值
- **解析（Resolve）**：将常量池中的**符号引用**（如 "com/example/BookService"）替换为**直接引用**（内存中的实际地址偏移量）。解析可以推迟到初始化之后才真正执行，这叫"延迟解析"

**3. 初始化（Initialization）**
- 执行类构造器 `<clinit>()` 方法：由编译器按代码顺序收集 `static` 字段的赋值语句和 `static {}` 块合并而成
- 把准备阶段的零值替换为用户代码中写的真实初始值
- JVM 保证 `<clinit>()` 在多线程环境下**只执行一次且线程安全**（内部加锁），这也是"懒汉单例"依赖类加载机制就足够安全的原因
- 初始化时机：首次主动使用该类时（new 实例、读取 static 字段、调用 static 方法、反射、子类初始化触发父类先初始化）

---

### Q2：Java 为什么需要 JIT，而不是像 C++ 一样直接编译成机器码？JIT 比静态编译能多做哪些优化？

**跨平台是首要设计动机**。Java 编译产物是平台无关的字节码（bytecode），而非 x86/ARM 机器码。字节码推迟到**运行时**由各平台的 JVM 负责翻译成机器码——"一次编译，到处运行"。这和 C++ 需要在每台目标机器上重新编译完全不同。

**JIT（Just-In-Time）编译器的核心优势在于运行时信息**。静态编译器（如 gcc/clang）在编译时只能看到源代码，无法预知实际运行情况。JIT 可以利用：

1. **实际类型分布（多态内联）**：一个接口变量在运行中 99% 时间指向同一个具体实现类，JIT 可以把它编译为直接调用，跳过虚方法表查找，称为"内联缓存（inline cache）"。比如 `List<String> list = new ArrayList<>()` — 虽然声明是 `List` 接口，但实际类型总是 `ArrayList`，JIT 会做单态内联
2. **热点编译**：JVM 用计数器统计每个方法的调用次数和循环回边次数。达到阈值后，C1（Client Compiler，快速编译）先做基础优化；极度热点方法再由 C2（Server Compiler，激进优化）重新编译，用更耗时但效果更好的算法。冷代码始终解释执行，不浪费编译时间
3. **逃逸分析**：如果一个对象的作用范围不逃出当前方法，JIT 可以**栈上分配**甚至**标量替换**——把对象的字段拆成独立的局部变量，完全避免堆分配和 GC 压力。这是 C++ 无法做到的，因为编译器不知道运行时的调用路径
4. **锁消除与锁粗化**：检测到锁对象是线程私有的，直接去掉同步块（锁消除）；多个连续的 `synchronized` 合并为一个大块（锁粗化），减少加锁/解锁开销

正是因为这些运行时反馈优化，长期运行的 Java 服务性能可以与 C++ 静态编译持平甚至在某些场景超越（C2 的激进内联 + 逃逸分析尤其有效）。

---

### Q3：JVM 的双亲委派机制为什么要存在？给出两个场景说明。

双亲委派（Parent Delegation）的核心思想：一个类加载器在收到加载请求时，不自己先加载，而是**递归委托给父加载器**，直到顶层的 Bootstrap ClassLoader。只有父加载器反馈"找不到"时，子加载器才尝试自己加载。

**场景一：安全——防止核心类被劫持**
假设攻击者在 classpath 放入自己写的 `java.lang.String`，里面有个 `static {}` 块把用户密码发到远程服务器。如果没有双亲委派，应用代码 `new String("hello")` 就可能加载伪造版本。有了双亲委派：AppClassLoader 收到请求 → 委托 ExtClassLoader → 委托 BootstrapClassLoader → Bootstrap 在 `rt.jar`（JDK 9+ 为 jmod）里找到真正的 `String.class` 并返回。伪造版本永远不会被加载，因为父加载器已经找到并返回了。

**场景二：唯一性——确保全 JVM 同一全限定名的核心类只有一份**
Java 的类型系统以「类的全限定名 + 定义它的类加载器」共同标识一个类型。如果同一个 `java.lang.Object` 被两个不同的类加载器各加载一次，就会出现两个不同的 `Class<Object>` 实例。那么 `obj instanceof Object` 可能在一个上下文中为 `true`，另一个为 `false` ——这会导致 `instanceof`、类型转换、`synchronized` 锁对象全部崩溃。双亲委派保证了核心类总是由最顶层的 Bootstrap 加载，整个 JVM 中只有一份。

**特例：Tomcat 为什么要故意打破双亲委派？**
Tomcat 一个容器可能部署多个 webapp，每个 webapp 依赖不同版本的 Spring、不同的第三方库。如果全用同一个类加载器，版本冲突必然发生（Spring 5 vs Spring 6 的类同名但行为不同）。Tomcat 的做法是：每个 webapp 分配一个独立的 WebappClassLoader，加载顺序**反过来**——先自己从 `WEB-INF/lib` 里找，找不到再委托给父加载器。这样每个 webapp 优先使用自己的 Jar 版本，实现了**多版本隔离**。同时还保护了核心类不被覆盖（`java.*` 之类仍禁止 webapp 自行加载）。

---

### Q4：Java 不需要 virtual 关键字也能多态，底层机制是什么？哪些方法不参与多态？

**底层机制**
Java 设计中所有实例方法默认就是"虚方法"（可被子类覆盖）。编译后字节码使用 `invokevirtual` 指令调用实例方法。JVM 在方法区（元空间）为每个类维护一张**虚方法表（vtable）**：数组结构，按方法签名排序，每个槽位指向该类实际使用的那个方法版本。子类覆盖某方法时，相应槽位被替换为子类版本的方法指针。

运行时调用 `obj.foo()` 的流程：
1. 找到 `obj` 实际类型的 `Class` 对象
2. 在方法表中按签名偏移查找到对应槽位
3. 取出方法指针（可能指向父类原版，也可能指向子类覆写版）
4. 跳转执行

整个过程由 `invokevirtual` 指令完成，开发者无需像 C++ 那样显式标记 `virtual`。

JIT 会在此基础上进一步优化：对热点调用点做**内联缓存**——记录前几次的实际类型，如果总是同一类型，就把虚方法调用编译为直接调用加类型校验，省去查表开销。

**不参与多态的方法**
| 修饰符 | 字节码指令 | 原因 |
|--------|-----------|------|
| `final` 方法 | `invokevirtual`（但 JIT 可直接内联） | 编译器确定不可被覆盖，编译期确定目标版本 |
| `static` 方法 | `invokestatic` | 属于类而非对象，不涉及实例类型分派 |
| `private` 方法 | `invokespecial` | 仅当前类可见，子类无从覆盖（子类"同名方法"是完全独立的新方法） |
| 构造器 `<init>` | `invokespecial` | 构造链在编译期已确定（super() / this()） |

---

### Q5：为什么重写 equals 必须同时重写 hashCode？不写会怎样？

**契约要求**
`Object` 类明确规定了 `equals` 和 `hashCode` 的契约：**如果 `a.equals(b)` 返回 `true`，那么 `a.hashCode() == b.hashCode()` 必须成立**。反之不一定成立（不同对象可以有相同哈希值，即哈希碰撞是允许的）。

**不重写 hashCode 的实际后果**
所有基于哈希的容器（`HashMap`、`HashSet`、`Hashtable`、`LinkedHashMap` 等）的工作流程是两阶段：
1. **定位桶**：调用 `key.hashCode()`，通过 `hash & (table.length - 1)` 计算数组下标
2. **桶内比较**：在桶的链表/红黑树上逐一调用 `key.equals(node.key)` 找匹配

举例说明问题：
```java
// 只重写了 equals，没重写 hashCode
class Product {
    String name;
    BigDecimal price;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Product p)) return false;
        return Objects.equals(name, p.name)
            && Objects.equals(price, p.price);
    }
    // hashCode 使用 Object 默认实现 → 按对象内存地址计算
}

Map<Product, Integer> stock = new HashMap<>();
stock.put(new Product("Book", 100), 5);
stock.get(new Product("Book", 100));  // 返回 null！
```

两个 `new Product("Book", 100)` 虽然内容完全相等（`equals` 返回 `true`），但它们是两个不同的对象实例，内存地址不同。`Object.hashCode()` 的默认实现按地址计算哈希，导致两个对象算出完全不同的哈希值，落入 `HashMap` 的**不同桶**中。`get()` 在空桶中找不到就返回 `null`。

**不仅是 `get` 失败**——`put` 也会存入重复 key、`containsKey` 返回 `false`、`remove` 无法删除、`HashSet` 出现"重复元素"。一句话：**所有哈希容器全部失灵**。

**正确做法**
基于业务关键字段计算哈希，与 `equals` 使用的字段保持一致：
```java
@Override
public int hashCode() {
    return Objects.hash(name, price);
}
```

**额外陷阱：只写 `equals(Product other)` 而不写 `equals(Object o)`**
如果方法签名写成 `equals(Product other)`（参数不是 `Object`），这不是重写（override）而是**重载（overload）**。`Object.equals(Object)` 仍然存在且仍按引用比较。JDK 所有容器内部调的都是 `Object.equals(Object)` ——永远跳过你的"重载版本"，看起来像是"重写没生效"。加 `@Override` 注解可以在编译期就暴露这个错误（编译器报错：方法没有覆盖父类方法）。

---

### Q6：Lambda 表达式底层怎么实现？跟匿名内部类有何区别？

**匿名内部类的实现方式**
```java
list.sort(new Comparator<String>() {
    @Override
    public int compare(String a, String b) {
        return a.length() - b.length();
    }
});
```
编译后，javac 为这个匿名类生成独立的 `.class` 文件（如 `OuterClass$1.class`），里面有完整的构造器、`compare` 方法等。运行时 JVM 加载这个类，`new` 出实例传入 `sort`。如果代码中有 10 个匿名内部类，就生成 10 个 `.class` 文件——**类爆炸**。

**Lambda 的实现方式**
```java
list.sort((a, b) -> a.length() - b.length());
```
编译时 javac **不生成**独立的 `.class` 文件。而是在调用处生成一条 `invokedynamic` 指令 + 一个引导方法（bootstrap method）指向 `LambdaMetafactory`。

运行时流程：
1. JVM 首次执行到 `invokedynamic` 时，调用 `LambdaMetafactory.metafactory()`
2. 该工厂方法按需**动态生成**一个实现类（在内存中，不落盘），该类实现 `Comparator` 接口，`compare` 方法体内联了 Lambda 体的字节码
3. 无捕获（不引用外部变量）的 Lambda：工厂直接返回**单例实例**，后续所有调用复用同一对象——极省内存
4. 有捕获（引用了外部变量）的 Lambda：每次调用生成一个新实例，但内部结构仍比匿名内部类轻量（没有"外部类引用"字段等额外开销）

**关键区别对比**

| 维度 | 匿名内部类 | Lambda |
|------|-----------|--------|
| 编译产物 | 独立 `.class` 文件 | `invokedynamic` 指令，无独立文件 |
| 类生成时机 | 编译期 | 运行时，按需动态生成 |
| 无捕获时 | 每次 `new` 新对象 | 单例复用 |
| `this` 语义 | 指向匿名内部类自身实例 | **透明**，指向外层方法所在对象 |
| 适用场景 | 需要多方法接口（如 `MouseListener`） | 仅函数式接口（单抽象方法） |

**`this` 差异是重要的坑**：在匿名内部类中调用 `this.toString()` 打印的是 `OuterClass$1@123`；Lambda 中调用 `this.toString()` 打印的是外部类实例的 `toString()`。

---

### Q7：虚拟线程相比传统线程为什么能扛百万并发？什么场景不适合？

**传统平台线程的问题**
Java 传统 `Thread` 是 1:1 映射到 OS 内核线程的。每个 OS 线程需要独立的栈空间（默认约 1MB），创建 1000 个线程就吃掉 1GB 内存。OS 能有效调度的线程数受 CPU 核数限制——几百个线程就开始频繁上下文切换，几千个基本不可用。这就是传统 Java 服务无法"一个请求起一个线程"应对高并发的根本原因。

**虚拟线程如何解这个问题**
虚拟线程（Virtual Thread，JEP 444）是 JDK 21 正式特性。核心设计：
1. **超轻量**：每个虚拟线程的栈只有约 **几 KB**（而非 MB 级），栈是动态伸缩的，存储在堆上
2. **M:N 调度**：成千上万个虚拟线程被 JVM 调度器映射到少量（通常 = CPU 核数）的**载体线程（Carrier Thread）**上执行
3. **阻塞时解绑（mount/unmount）**：当虚拟线程执行到 I/O 阻塞操作（读网络、等数据库响应）时，JVM 自动把它的栈帧从载体线程上"卸下来"放回堆，让出载体线程去执行其他虚拟线程。阻塞结束后，JVM 调度器把它重新挂载到可用的载体线程上继续执行

这样一来，一个服务可以轻松创建百万级虚拟线程——绝大多数时间它们都在等待 I/O，只有极少数在载体线程上实际计算。用户代码写法上跟传统 `Thread.start()` 几乎一样，但底层运行方式完全不同。

**不适合虚拟线程的场景**

1. **CPU 密集任务**：纯计算（加密、图像处理、科学计算）不涉及阻塞 I/O，虚拟线程没法"让出"载体线程。一个计算任务把载体线程占满了，其他虚拟线程只能排队等待，收益为零甚至比原生线程更慢（多了调度开销）

2. **长 `synchronized` 块或 JNI 调用**：`synchronized` 持有对象监视器时、调用本地代码（JNI）时，虚拟线程**无法被卸载**——它把载体线程"钉住（pin）"了，导致该载体线程上的其他虚拟线程全部堵塞。JDK 正在逐步用 `ReentrantLock` 替换 JDK 内部的 `synchronized` 以缓解这个问题

3. **大量 `ThreadLocal`**：如果有百万虚拟线程每个都存一堆 `ThreadLocal` 数据，堆内存会被瞬间吃掉。传统线程池中线程数少，`ThreadLocal` 用量可控；虚拟线程数量暴涨后需要谨慎评估

---

## 二、Spring Boot 核心

### Q8：构造器注入 vs 字段注入 vs setter 注入，为什么推荐构造器注入？

Spring 提供了三种依赖注入方式：

**1. 字段注入（Field Injection）**
```java
@Autowired
private BookService bookService;
```
优点：代码最短。缺点：
- 依赖**隐式**——看字段声明无法知道这个类依赖了什么，必须翻整个类的代码
- 字段不能声明 `final`，编译器不帮你保证不可变
- **单元测试困难**：如果没有 Spring 容器，必须用反射（`ReflectionTestUtils`）手动设置字段，或者被迫起 Spring 上下文
- 容易产生**循环依赖**且编译期不报错——A 注入 B，B 注入 A，只有运行时才暴露
- Spring 官方已不建议新项目使用

**2. setter 注入（Setter Injection）**
```java
private BookService bookService;
@Autowired
public void setBookService(BookService bookService) {
    this.bookService = bookService;
}
```
适用于**可选依赖**——依赖可以没有，有的话通过 setter 注入。但这个场景极少出现，大多数情况下依赖是必须的。

**3. 构造器注入（Constructor Injection）✅ 推荐**
```java
private final BookService bookService;
private final OrderRepository orderRepository;

public OrderController(BookService bookService, OrderRepository orderRepository) {
    this.bookService = bookService;
    this.orderRepository = orderRepository;
}
```
优点：
- 依赖**显式**——看构造器参数就知道这个类需要什么，依赖多了构造器会很长，这是**提醒你"类可能做太多事了"的信号**（单一职责原则）
- 字段可声明 `final`——依赖注入后不可变，线程安全
- **循环依赖在启动时就暴露**——Spring 无法实例化互相依赖的两个 Bean，直接报错而不是让问题隐藏到运行时
- **单元测试极简**——测试时直接 `new` 这个类并传入 mock 对象，完全不需要 Spring 上下文

**现代最佳实践**：Spring 4.3+ 如果类只有一个构造器，`@Autowired` 可以省略。配合 Lombok：
```java
@RequiredArgsConstructor  // 自动为 final 字段生成构造器
public class OrderController {
    private final BookService bookService;
    private final OrderRepository orderRepository;
}
```
四行代码，依赖显式、final 不可变、单测友好——三全其美。

---

### Q9：分层架构里事务边界为什么放 Service 不放 Controller 或 Repository？

理解这个问题需要从"一次业务操作"的粒度出发。

**放 Controller 的问题**
HTTP 是无状态协议，Controller 的职责是接收 HTTP 请求、做参数校验和格式转换、调用业务层、返回 HTTP 响应。事务应该和业务逻辑绑定，而非和 HTTP 绑定。如果事务边界放在 Controller：
- Controller 要了解哪些操作需要原子性——这是**业务知识**，应由 Service 掌握
- 同一个业务逻辑在不同 Controller 中被调用时（REST API + 页面渲染 + MQ 消息处理），事务逻辑就重复了
- Controller 的职责混杂了"接收 HTTP"和"管理事务"，完全违反单一职责

**放 Repository 的问题**
一次典型的业务操作通常涉及**多个 Repository**：
```java
// 下订单：涉及 3 个 Repository
orderRepository.save(order);        // 写订单
bookRepository.decrementStock(bookId, quantity);  // 扣库存
accountRepository.deductBalance(userId, amount);  // 扣余额
```
如果事务注解 `@Transactional` 加在每个 Repository 方法上，这三个操作就分属三个独立事务：
- 第一个事务提交：订单已写入
- 第二个事务失败：库存没扣成，但订单已经在了——数据库出现"幽灵订单"
- 或者第二个成功、第三个失败：库存扣了、钱没扣——亏大了

这完全破坏了业务的**原子性**——订单、库存、余额要么全成功、要么全回滚。

**Service 是事务的自然边界**
```java
@Service
public class OrderService {
    @Transactional
    public Order createOrder(Long userId, Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId);
        book.decrementStock(quantity);
        bookRepository.save(book);

        Order order = new Order(userId, bookId, quantity, book.getPrice());
        orderRepository.save(order);

        Account account = accountRepository.findByUserId(userId);
        account.deduct(order.getTotalAmount());
        accountRepository.save(account);

        return order;
    }
}
```
`@Transactional` 包裹整个 `createOrder` 方法——里面的所有数据库操作共享同一个数据库连接和事务上下文，任何一步失败，整个事务回滚，数据始终保持一致。事务边界 = 业务操作边界，这正好对应 Service 层的职责：**业务编排**。

**补充：同类内部调用导致事务失效的陷阱**
```java
// 在同一类中
public void publicMethod() {  // 没有 @Transactional
    this.internalMethod();    // 直接 this 调用，绕过了 AOP 代理
}

@Transactional
public void internalMethod() { ... }  // 事务不生效！
```
Spring 的 `@Transactional` 通过 AOP 代理实现——只有从外部调用代理对象时，代理才会在调用前后开启/提交/回滚事务。类内部 `this.xxx()` 调用不走代理，事务注解直接失效。解决方法：把方法拆到另一个 Bean，或注入自身代理（不推荐）。

---

### Q10：Entity 能不能直接返给前端？为什么必须用 DTO？

**结论：不能。** 这是 Spring Boot 实战中最重要的分层纪律之一。

**原因一：安全——暴露内部字段**
JPA Entity 直接映射数据库表，表里常有前端不需要知道的字段：
```java
@Entity
public class Book {
    private Long id;
    private String title;
    private BigDecimal costPrice;   // 成本价——商业秘密！绝不对外
    private BigDecimal salePrice;   // 销售价 —— 前端可以看
    private Long supplierId;        // 供应商 ID——内部敏感信息
    private String internalNote;    // 内部备注
}
```
如果直接把 `Book` Entity 序列化为 JSON 返回，`costPrice`、`supplierId`、`internalNote` 全暴露给外部——任何人都能在浏览器 F12 看到你的进货价。用 DTO 可以只选择前端需要的安全字段。

**原因二：无限递归——JPA 双向关联的死循环**
```java
@Entity
public class Order {
    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;
}

@Entity
public class OrderItem {
    @ManyToOne
    private Order order;  // 反向引用
}
```
序列化 `Order` 时 → Jackson 序列化 `items` 列表 → 每个 `OrderItem` 又序列化 `order` → `order` 又序列化 `items` → ... 无限递归，最终抛出 `StackOverflowError` 或 Jackson 的 `JsonMappingException`。虽然可以用 `@JsonIgnore` / `@JsonManagedReference` / `@JsonBackReference` 打断循环，但这些注解把**序列化关注点**污染到了**持久化层**——Entity 本应只关心数据库映射，不应该知道自己会不会被序列化。

**原因三：API 契约与表结构解耦**
如果 Entity 直接对前端暴露，每次表加个字段——API 返回就多一个字段，前端调用可能被新字段影响（比如 Jackson 反序列化失败）。数据库表结构变更本应是内部行为，不应破坏外部 API 契约。DTO 作为显式的 API 契约层，只有明确声明才会暴露出去。表加 10 个字段，DTO 不更新 = API 不受影响。

**原因四：不同场景需要不同字段**
同一个 `Book` 在列表页（只需 `id + title + price + 作者名`）、详情页（还需简介、目录、评论）、编辑页（只需可编辑字段）、创建请求（无 `id`，有初始值）需要的字段各不相同。一个 Entity 永远满足不了所有场景。对应策略：

```
Controller 层：接收 RequestDTO，转 Entity 给 Service，Service 返回 Entity，Controller 转 ResponseDTO 输出
```

DTO 分层是**必要的纪律**，不是过度设计。

---

### Q11：Spring Data JPA 的 Repository 接口不写实现为什么能用？

```java
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByNameAndPriceLessThan(String name, BigDecimal price);
}
```
只定义了接口，没有实现类、没有 SQL、没有 XML 配置——但它确实能工作。原因在于 Spring Data JPA 在**运行时动态生成代理**。

**启动时发生了什么**
1. `@EnableJpaRepositories`（Spring Boot 自动配置已包含）驱动扫描指定包下所有继承 `Repository` 的接口
2. 对每个 Repository 接口，Spring Data 通过 `JdkDynamicAopProxy` 或 CGLIB 创建一个**代理对象**并注册为 Spring Bean
3. 代理对象内部持有 `EntityManager`（JPA 核心 API），负责实际的数据库交互

**方法调用时发生了什么**
以 `findByNameAndPriceLessThan(String name, BigDecimal price)` 为例：

1. 调用代理对象的 `findByNameAndPriceLessThan` 方法
2. 代理拦截调用，进入 `QueryExecutorMethodInterceptor`
3. **方法名解析**（派生查询/Derived Query）：按驼峰分词拆出 `find` + `By` + `Name` + `And` + `Price` + `LessThan`
4. 组装查询条件：`Name`（精确匹配 `=`）+ `And` + `Price` `LessThan`（`<`）
5. 通过 `EntityManager` 生成 JPQL：`SELECT b FROM Book b WHERE b.name = ?1 AND b.price < ?2`
6. JPA 将 JPQL 翻译为数据库方言的 SQL（MySQL 方言、PostgreSQL 方言等）并执行
7. 将返回的 `ResultSet` 行自动映射为 `Book` 对象列表

**三种查询方式**
| 方式 | 说明 | 适用场景 |
|------|------|---------|
| 派生查询（方法名约定） | 方法名即是查询定义 | 简单条件查询，不超过 2-3 个条件 |
| `@Query` JPQL | 在方法上写 JPQL | 中等复杂查询，跨数据库可移植 |
| `@Query` Native SQL | `nativeQuery = true`，写原始 SQL | 需数据库特有功能（窗口函数、全文索引）|
| 自定义实现 | 接口 + Impl 后缀实现类，注入 `EntityManager` | 极复杂查询（动态条件、多表聚合）|

**自定义实现的写法**：定义一个接口 `BookRepositoryCustom` + 实现类 `BookRepositoryImpl`（后缀必须是 `Impl`），让 `BookRepository extends JpaRepository<Book, Long>, BookRepositoryCustom`。Spring Data 在启动时自动把 Impl 的方法织入代理，调用方只看到 `BookRepository` 一个接口。

---

## 三、数据库理论与 SQL

### Q12：为什么说 SQL 是声明式的？这给数据库带来什么权力？

**声明式 vs 命令式的本质区别**
- **命令式**（C/Java/Python）：你告诉计算机**每一步怎么做**——"打开文件、分配数组、循环每一行、如果条件匹配就加到结果集"
- **声明式**（SQL）：你只告诉数据库**你想要什么结果**——`SELECT title, price FROM book WHERE price > 50 ORDER BY price DESC`——不描述"怎么遍历表、用哪个索引、怎么排序"

**数据库拿到 SQL 后拥有重写执行计划的自由**
DBMS 的查询优化器（Query Optimizer）接收到 SQL 后，会生成多种等价但物理实现不同的**执行计划**：

```
-- 同样的 SQL，不同执行计划可能有数量级的性能差异
SELECT b.title, a.name
FROM book b JOIN author a ON b.author_id = a.id
WHERE b.price > 100
```

可选方案包括：
- **扫描顺序**：先扫 `book` 过滤 `price > 100` 再 JOIN `author`，还是先扫 `author` 再 JOIN `book`？
- **JOIN 算法**：Hash Join（小表建哈希表，大表探测）、Merge Join（两边按主键排好序时归并扫描）、Nested Loop Join（外表小且内表有索引）
- **索引选择**：`price` 上有索引就用索引范围扫描；没有索引就全表扫描
- **中间结果**：先过滤再 JOIN 还是先 JOIN 再过滤？

优化器基于**代价模型**（cost model，估算每种方案的 I/O 次数和 CPU 消耗）选出代价最低的计划。

**如果 SQL 是命令式的**
优化器就无法重写计划——你写了"全表扫"它就得全表扫，即使有更好的索引可用。这就是为什么声明式的设计是数据库性能的根基。"同样语义可由多种物理执行实现"——同一句 SQL 在数据量 100 行和 1 亿行时，优化器会自动选择完全不同的执行策略。

**优化器的代价估算依赖于统计信息**：表的行数、列基数（distinct value 数量）、值分布直方图、索引选择性等。这些通过 `ANALYZE TABLE` 更新。如果统计过期（比如大量数据导入后没 ANALYZE），优化器可能严重误判，选错的执行计划导致本该毫秒的查询跑成分钟。

---

### Q13：候选键、超键、主键的关系是什么？"最小"如何定义？

**从超键到候选键到主键的递进关系**

1. **超键（Super Key）**：能唯一标识表中每一行的**任意**属性集。例如 `Student(id, name, email, phone)`：
   - `{id}` 是超键（一个 id 对应唯一学生）
   - `{email}` 也是超键（假设 email 唯一）
   - `{id, name}` 还是超键（虽然 name 是多余的）
   - `{id, email, phone}` 也是超键

   超键的最大特点：可以包含**冗余**字段。只要唯一，就是超键。

2. **候选键（Candidate Key）**：**最小超键**——从中删除任何一个属性后就不再是超键（失去唯一性）。上例中：
   - `{id}` 是最小超键——删了 `id` 空集不能标识任何行 → 候选键
   - `{email}` 也是候选键
   - `{id, name}` 不是候选键——删了 `name` 还剩 `{id}` 仍然唯一（`name` 是多余的）

   **"最小"是指不可再缩减**，不是指字段数最少。一张表可以有多个候选键。

3. **主键（Primary Key）**：从候选键中**选定的那一个**。选择标准：简单（单列比复合好）、稳定（不常变化）、简短（整数比字符串快）。DBMS 对主键施加了额外的约束：强制 `NOT NULL` + 自动建唯一索引作为聚簇索引（InnoDB）。

**为什么需要区分这些概念？**
- 关系代数和范式理论建立在"函数依赖"（A → B，知道 A 就能唯一确定 B）之上
- 函数依赖的左侧必须是候选键才能满足高范式要求（BCNF）
- 外键只能引用主键或候选键——不是候选键的列不能保证唯一，引用就没意义

**举例**
表 `Enrollment(student_id, course_id, score, semester)`：
- 超键：`{student_id, course_id, semester}`、`{student_id, course_id}`、`{student_id, course_id, score}`
- 候选键：`{student_id, course_id, semester}`（同一个学生可以重修同一门课在不同学期）或者 `{student_id, course_id}`（如果设定"每门课只能选一次"）
- 主键：通常选 `{student_id, course_id}`

---

### Q14：B+ 树为什么比二叉树更适合数据库索引？

**核心差异在磁盘 I/O**
数据库数据存在磁盘上，每次读取一个节点就对应一次磁盘 I/O（假设未命中缓存）。机械硬盘一次随机 I/O 约 5-10ms，SSD 约 0.1ms——比内存访问慢几个数量级。所以索引的性能由**查找路径上需要多少次 I/O**决定。

**二叉树的问题——太"高"**
AVL 或红黑树是二叉的，每个节点最多两个子节点。查找路径长度 = 树的高度 ≈ `log₂(N)`：
- 100 万行 → 高度约 20 → 最长可能 **20 次 I/O**
- 10 亿行 → 高度约 30 → 最长 **30 次 I/O**

每次 I/O 只读几个字节（一个节点只有 key + 两个子节点指针），空间利用率极低。

**B+ 树的优势——又"矮"又"胖"**
B+ 树每个节点不是存一个 key，而是存**整个磁盘页**能装下的所有 key（一个 block 通常 16KB）。按 InnoDB 16KB 页、每个 key+指针约 16 字节计算，一个内部节点可容纳约 **1000 个 key**（即 1000 个子节点指针）。

- 1000 路分叉（非二叉）→ 树高 = `log₁₀₀₀(N)`
- 100 万行 → 高度 3（根 + 1 层内部 + 叶子）
- 10 亿行 → 高度 4

**3-4 次 I/O 就能覆盖几乎任何数据量**。

**B+ 树 vs B 树的额外优势**
B+ 树只在叶子节点存数据（内部节点只作导航），且所有叶子节点用**双向链表串联**：
- **范围查询**：`WHERE id BETWEEN 1000 AND 2000` ——找到 1000 的位置后，直接沿叶子层链表顺序扫描到 2000，无需回溯到上层节点
- **全表扫描**：从链表第一个叶子开始顺序遍历即可
- **ORDER BY 主键**：天然有序，无需额外排序

B 树的叶子节点没有链表，范围查询需要不断回溯父节点，效率低得多。

---

### Q15：聚簇索引为什么每表只能有一个？二级索引怎么工作？

**聚簇索引的本质**
聚簇索引（Clustered Index）不是普通意义上的"索引"——它**规定了数据行在磁盘上的物理存储顺序**。InnoDB 中主键就是聚簇索引，其 B+ 树叶子节点直接存储**完整的行数据**（包括所有列，不是只存 key）。换句话说，表的数据就是按主键排序后存储在 B+ 树的叶子层——**索引即数据**。

**为什么只能有一个？**
磁盘上的物理排列顺序只能有一种——你不可能让同一堆字节同时按 A 列排序又按 B 列排序。这就好比一本书的正文只能按一种顺序印刷——通常按章节编号排序（相当于聚簇索引），而书后的"术语索引"按字母排序（相当于二级索引）。

**二级索引的工作机制**
非主键索引（如 `INDEX idx_name (name)`）是**二级索引（Secondary Index）**。它的 B+ 树结构：
- 内部节点：按 `name` 排序的导航层
- **叶子节点不存完整行数据**——只存「索引列值 + 主键值」

查询 `SELECT * FROM book WHERE name = 'Java核心'` 的完整流程：
1. **索引查找**：在 `idx_name` 的 B+ 树中按 `name = 'Java核心'` 找到叶子节点
2. **获取主键**：从叶子节点拿到主键值（如 `id = 42`）
3. **回表**：拿着 `id = 42` 去聚簇索引的 B+ 树再查一次，拿到完整行数据

步骤 3 就是 **回表（Bookmark Lookup）**——额外一次 B+ 树查找。如果查询需要的列都在二级索引的叶子中（**覆盖索引**），则省去回表：

```sql
-- 不需要回表：idx_name 叶子已有 (name, id)，够用
SELECT id, name FROM book WHERE name = 'Java核心';

-- 需要回表：price 不在 idx_name 中，必须去聚簇索引取
SELECT * FROM book WHERE name = 'Java核心';  -- * 回表

-- 建覆盖索引避免回表
CREATE INDEX idx_name_price ON book(name, price);
-- SELECT name, price FROM book WHERE name = '...' -- 覆盖，不回表
```

**如果表没有显式主键**，InnoDB 会按优先级找一个替代聚簇键：第一个 `UNIQUE NOT NULL` 列 → 隐式生成一个 6 字节的 `row_id`。所以建表时显式指定主键是好的习惯。

---

### Q16：1NF、2NF、3NF 的核心要求分别是什么？

范式存在的根本动机：**冗余 → 三种异常**：
- **插入异常**：想存某信息却被迫填一堆无关数据（系里没学生时系主任信息无处存）
- **更新异常**：同一事实存多份，改一处忘改另一处（系主任换了，要改该系所有学生的行）
- **删除异常**：删一条记录顺带毁了另一些本该保留的信息（删除最后一个学生顺带删掉了系的所有信息）

**1NF：列值原子性**
每个字段的值不可再分，不允许多值字段。

❌ 违反：
```sql
-- phones = "139xxx,138xxx,137xxx"  三个电话塞一个字段
-- 想查"谁的电话尾号是 1234" → LIKE '%1234%' 全表扫描，无法用索引
-- 要修改其中一个电话 → 字符串替换，极易出错
-- JOIN、范围查询、聚合全部失效
```
✅ 修正：拆出独立表 `user_phone(user_id, phone, type)`，每条电话一行。

**2NF：消除部分依赖**
前提：1NF。要求：**非主键字段必须依赖完整主键，不能只依赖主键的一部分**。

只有**复合主键**的表才可能违反 2NF（单列主键自动满足）。

❌ 违反：
```
Enrollment(student_id, course_id, student_name, course_name, score)
主键：(student_id, course_id)
问题：student_name 只需要 student_id 就能确定（只依赖部分主键）
      course_name 只需要 course_id 就能确定（只依赖部分主键）
```
后果：学生改名要更新所有该生的选课记录；课程还没人选时就无法录入课程名（插入异常）。

✅ 修正：拆为三张表 —— `Student(student_id PK, name)`、`Course(course_id PK, name)`、`Enrollment(student_id FK, course_id FK, score)`。

**3NF：消除传递依赖**
前提：2NF。要求：**非主键字段不能依赖另一个非主键字段**。

❌ 违反：
```
Student(id PK, name, dept, dept_dean)
问题：id → dept → dept_dean（系主任不是直接依赖于学号，而是通过"系"这个非主键字段传递的）
```
后果：系主任换人 → 要更新该系所有学生的行（更新异常）；系暂没有学生 → 系主任信息无处存（插入异常）。

✅ 修正：拆为 `Student(id PK, name, dept FK)` + `Department(dept PK, dean)`。

**3NF 朴素口诀**
> 每个非主键字段直接依赖于主键、只依赖于完整主键、不依赖于别的非主键。
> ——"依赖于主键、依赖于完整主键、无传递依赖"。

**实战取舍**
生产环境并非范式越高越好。3NF 已覆盖绝大多数业务场景。以下场景合理的**反范式**是必要的：
- 报表/分析表：故意冗余避免 JOIN
- 业务快照：订单价格是下单时点的历史，和商品表当前价格是两个不同事实
- 性能瓶颈：JOIN 太多时可适当合并表

---

### Q17：订单中冗余 `unit_price_snapshot` 跟范式追求"消除冗余"矛盾吗？

**不矛盾。** 这是初学者最容易混淆的地方。

范式消除的冗余是**同一事实的多份拷贝**——比如作者名既在 `book` 表里写一遍又在 `author` 表里写一遍。这时如果作者改名，一处改了另一处没改 → 数据不一致 → 范式强制只在一个地方写。

但订单里的 `unit_price_snapshot`（下单时的商品单价）和商品表的 `price`（当前售价）是**两个不同的事实**：

| | 商品表 `price` | 订单行 `unit_price_snapshot` |
|---|---|---|
| 含义 | 商品**当前**售价 | 下单那一瞬间的**历史价格** |
| 会变吗 | 会，涨价/降价 | 不会——已发生的交易价格不能改 |
| 语义 | "这本书现在卖多少钱" | "这笔订单成交时价格是多少" |

这是**时间维度上的不同事实**。书从 100 元涨到 120 元，历史订单的价格**绝不能**跟着变——否则财务报表对不上、用户看到自己的历史订单金额变了会投诉。

**合理冗余 vs 违反范式**
```
-- 商品表存当前价
Book(id, title, current_price) → (1, 'Java核心', 100)

-- 订单表快照下单价格（不是冗余，是业务事实）
Order(id, book_id, unit_price_snapshot, created_at) → (1, 1, 100, '2025-01-15')

-- 如果还存了 book_title（可以从 Book 表查到），这才是反范式冗余
-- Order(id, book_id, book_title, ...) ← book_title 是同一事实的多份拷贝
```

金融/电商系统中大量使用这种"时间点快照"——金额、地址、税率、货币汇率，下单时全快照下来，保证事后审计的一致性。这是**业务合理冗余**，不是数据库理论中的"冗余"。

---

## 四、JDBC 与数据访问

### Q18：SQL 注入的根本原因是什么？为什么 PreparedStatement 能彻底防住？

**根本原因：输入被重新解析为 SQL 语法**

典型注入场景：
```java
// 用户输入：' OR '1'='1' --
String username = request.getParameter("username");
String password = request.getParameter("password");
String sql = "SELECT * FROM user WHERE username = '" + username
           + "' AND password = '" + password + "'";
Statement stmt = conn.createStatement();
ResultSet rs = stmt.executeQuery(sql);
```

用户输入 `admin' --` 作为 username，拼接后：
```sql
SELECT * FROM user WHERE username = 'admin' -- ' AND password = 'xxx'
```
`--` 后面的内容全被注释掉，密码检查被跳过 ——攻击者以 admin 身份登录。

更危险的：`'; DROP TABLE users; --` → 删表。

**问题的本质**：用户输入和 SQL 语句在**同一条字符串**中，数据库的 SQL 解析器无法区分"这是程序员写的 SQL 关键字"还是"这是用户数据里的字符串"。引号闭合、注释符、分号等 SQL 语法符号在输入中会被当做 SQL 指令执行。

**PreparedStatement 如何彻底防住？**

```java
String sql = "SELECT * FROM user WHERE username = ? AND password = ?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, username);  // admin' --
ps.setString(2, password);
ResultSet rs = ps.executeQuery();
```

关键在两步分离：

1. **预编译阶段**（`prepareStatement(sql)` 调用时）：将带占位符 `?` 的 SQL 模板先发给数据库，数据库解析器做词法/语法分析，生成**执行计划**并缓存。此时数据库已经确定了 SQL 的语法结构——`SELECT` 有哪些列、`FROM` 哪些表、`WHERE` 有几个条件、条件之间用什么逻辑连接——这些都是**不可改变的**。

2. **参数绑定阶段**（`setString` + `executeQuery`）：参数值通过**独立的二进制协议**从驱动传给数据库，数据库按 `?` 占位符的位置将参数值填入已经编译好的执行计划。参数只被当作**字面量字符串值**，即使它包含 `'`、`--`、`;` 等特殊字符，数据库也不会对这些内容做 SQL 解析——它们**永远不会被解释为 SQL token**。

如果攻击者输入 `admin' --`，PreparedStatement 的处理逻辑是：查找 username 等于字符串 `"admin' --"`（包括引号和破折号作为字符串内容）的用户——当然找不到，安全失败。

**额外收益：执行计划缓存**
预编译后的执行计划可以跨多次执行复用——同一个 SQL 模板（相同的 `?` 结构）的不同参数值复用同一条执行计划，省去每次解析和优化的开销。批量插入配合 `addBatch()/executeBatch()` 尤其显著，速度可以是逐条插入的 10-100 倍。

---

### Q19：连接池大小为什么不是越大越好？（L31/L27）

**并发收益有物理上限，过了临界点就是负收益**

原因从数据库内部视角分析：

**1. 每个连接对应 DB 端一条工作线程**
当你从连接池借一个连接时，数据库端会分配一条工作线程来处理这个连接的请求。线程数是有限资源。100 个连接 = 数据库内部 100 个工作线程。

**2. CPU 核数决定了真正的并行度**
如果数据库服务器有 8 个 CPU 核，同一时刻最多 8 个线程在真正执行运算。第 9 个及以后的线程要么在等待、要么被 OS 调度器切换。每次上下文切换有以下开销：
- 保存/恢复寄存器状态
- CPU 缓存失效（L1/L2 cache 中属于上一个线程的数据被废弃）
- TLB（页表缓存）刷新

连接数远超 CPU 核数时，上下文切换开销从"可忽略"变成"主导因素"，**吞吐量反而下降**。这不是"越大越慢一点点"，而是典型的生产故障——连接池开 500，CPU 100% 但 80% 都在做切换，有效业务处理不到 20%。

**3. 数据库侧的连锁反应**
- DB 内存：每个连接都有查询缓存、排序缓冲区、临时表空间等，1000 个连接 = GB 级内存被吃掉
- 文件描述符：每个连接消耗 1+ 个 fd，OS 和 DB 都有上限
- 锁竞争：并发连接越多，行锁/表锁冲突越激烈，死锁概率增大

**合理值怎么确定？**
实战经验值远小于直觉。一个公式参考：
```
连接数 ≈ CPU 核数 × (1 + 平均查询时间 / 平均等待时间)
```
- 如果每个查询平均 10ms，而用户思考时间 5 秒（5000ms），几十个连接就能服务上万个并发用户
- 如果每个查询 500ms 且无思考时间（后台批处理），连接数应接近 CPU 核数

**HikariCP 的建议很经典**：
```
connections = ((core_count * 2) + effective_spindle_count)
```
- `core_count`：CPU 核数
- `effective_spindle_count`：硬盘数（SSD 可忽略）
- 所以一台 8 核 SSD 服务器，**10-20 个连接即可**。绝不是 100-1000。

**虚拟线程出现后的变化**
虚拟线程解决了 Java 应用端的线程瓶颈（百万虚拟线程轻松创建），但连接池的物理限制没有变——借了连接就占着，虚拟线程不会"自动让出"连接。引入虚拟线程后，**瓶颈整体从应用层线程池下移到连接池和数据库**。合理配置连接池仍然重要。

---

### Q20：JdbcTemplate 解决了哪些原生 JDBC 痛点？仍未解决什么？

**原生 JDBC 的样板地狱**
```java
Connection conn = null;
PreparedStatement ps = null;
ResultSet rs = null;
try {
    conn = dataSource.getConnection();
    ps = conn.prepareStatement("SELECT * FROM book WHERE price < ?");
    ps.setBigDecimal(1, maxPrice);
    rs = ps.executeQuery();
    List<Book> books = new ArrayList<>();
    while (rs.next()) {
        Book b = new Book();
        b.setId(rs.getLong("id"));
        b.setTitle(rs.getString("title"));
        b.setPrice(rs.getBigDecimal("price"));
        books.add(b);
    }
    return books;
} catch (SQLException e) {
    throw new RuntimeException(e);  // 原始异常信息丢失
} finally {
    // 三个 close 的嵌套 try-catch，漏一个就泄漏
    if (rs != null) try { rs.close(); } catch (SQLException e) { /* ignore */ }
    if (ps != null) try { ps.close(); } catch (SQLException e) { /* ignore */ }
    if (conn != null) try { conn.close(); } catch (SQLException e) { /* ignore */ }
}
```
5 行业务逻辑，30 行样板代码。

**JdbcTemplate 解决了什么**

1. **资源自动关闭**：`jdbcTemplate.query(sql, params, rowMapper)` 内部用 `try-with-resources` 自动按 `ResultSet → Statement → Connection` 逆序关闭，永无遗漏

2. **异常翻译**：将晦涩的 `SQLException`（一个错误码走天下）翻译为 Spring 的层次化异常体系。`BadSqlGrammarException`（SQL 语法错）、`DataIntegrityViolationException`（约束冲突）、`CannotGetJdbcConnectionException`（无法连库）等——一眼就知道问题类型

3. **样板封装**：`query()` / `queryForObject()` / `queryForList()` / `update()` / `batchUpdate()` 开箱即用，覆盖 90% 场景

4. **RowMapper 解耦**：将"结果集 → 对象"的映射抽离为独立的 `RowMapper`，可复用、可单测

**仍未解决什么**
```java
String sql = "SELECT id, title, price FROM book WHERE author_id = ?";
// 仍要手写表名 book、字段名 id/title/price——与具体表结构耦合
return jdbcTemplate.query(sql, bookRowMapper, authorId);
```
应用代码仍然**直接写死表名和字段名的 SQL 字符串**。如果：
- `book` 表重命名为 `books`
- `author_id` 改为 `author_fk`
- 换数据库（MySQL → PostgreSQL）、SQL 方言差异需要修改 SQL
→ 需改动所有包含相关 SQL 的业务代码

这些正是 **JPA（ORM）要解决的问题**——将表名/字段名/关系从字符串提升为类型安全的 Java 代码。JdbcTemplate 是 JDBC 的最佳实践封装，ORM 是更高一层的抽象。

---

### Q21：RowMapper 本质是什么？跟 ORM 的关系？

**RowMapper 是"行 → 对象"的手写映射函数**

```java
RowMapper<Book> bookRowMapper = (rs, rowNum) -> {
    Book book = new Book();
    book.setId(rs.getLong("id"));
    book.setTitle(rs.getString("title"));
    return book;
};
```
本质：对 `ResultSet` 当前行的**每个列**，显式调用 `getXxx("columnName")`，塞入 Java 对象的对应字段。数据库列名和 Java 字段名之间的映射全由你手写，没有任何自动化。

**ORM 在做的事——把这层自动化**
JPA/Hibernate 启动时读取 Entity 上的注解：
```java
@Entity
@Table(name = "book")
public class Book {
    @Id @GeneratedValue private Long id;
    @Column(name = "title") private String title;
    @Column(name = "price") private BigDecimal price;
}
```
基于这些元数据，Hibernate 在运行时生成等价的 RowMapper 逻辑：`rs.getLong("id") → book.setId()`、`rs.getString("title") → book.setTitle()` 等。不同的是：
- 表名来自 `@Table`（自动处理数据库方言的引号规则）
- 列名来自 `@Column`（name → 字段的映射声明一次，全局可用）
- 关联关系（`@OneToMany`、`@ManyToOne`）的嵌套映射也自动处理

**所以 RowMapper 是 ORM 的"前身"**。理解 RowMapper 就理解了 ORM 在"行映射"这层做了什么：它替你写了成千上万行 `rs.getString("xxx")` 的样板代码。而 ORM 在此之上还加了：缓存（一级/二级缓存）、脏检查（自动 UPDATE）、级联操作、延迟加载、乐观锁等高级功能。

---

### Q22：为什么聚合关系（Aggregate）不给子实体单独 Repository？

这源自 DDD（领域驱动设计）的**聚合（Aggregate）**概念。

**什么是聚合**
以订单为例：`Order`（订单）是聚合根（Aggregate Root），`OrderItem`（订单项）是子实体。`OrderItem` 脱离 `Order` 没有独立存在的意义——一个订单项"属于某个订单"是它身份的一部分。

**不给子实体单独 Repository 的原因**

1. **业务完整性**：如果 `OrderItemRepository` 可以独立操作：
   ```java
   orderItemRepository.save(new OrderItem(null, bookId, 5));
   // order_id 为 null → 幽灵订单项，不知道属于哪个订单
   orderItemRepository.deleteById(itemId);
   // 删了订单项但 Order.totalAmount 没更新 → 数据不一致
   ```
   所有对 `OrderItem` 的修改都应该通过 `Order` 来验证业务规则（如"订单已发货则不能再修改订单项"）。

2. **级联一致性**：`@OneToMany(cascade = ALL, orphanRemoval = true)` 配合 `mappedBy` 让 JPA 自动管理子实体生命周期——从 `Order.items` 列表移除一个 `OrderItem`，JPA 会自动 DELETE 它。不需要手动调用子实体的 Repository。

3. **事务边界简明**：一次业务操作加载一个 `Order`，修改它的 `items` 列表，save `Order`——一个 Repository 调用，逻辑清晰。如果到处都能直接操作 `OrderItem`，事务边界就散了，不知道一次操作涉及哪些数据。

**什么时候可以有独立 Repository？**
当子实体本身有独立的查询需求且有独立业务含义时，可以给一个只读的 Repository 或专门的 Query Service。但**写操作永远走聚合根**，这是 DDD 聚合的核心规则。

---

## 五、JPA / ORM

### Q23：O/R 阻抗失配的本质是什么？举三个具体表现。

**本质**
面向对象编程和关系数据库是两个根本不同的**建模范式**，它们的核心抽象互不兼容：

| 维度 | 面向对象（O） | 关系数据库（R） |
|------|-------------|---------------|
| 数据结构 | 图/树/网——对象包含对象引用 | 二维表——行和列 |
| 关系表示 | 对象引用（内存指针）| 外键（值匹配）|
| 身份标识 | 内存地址（`==`）| 主键值（`id = 42`）|
| 类型层级 | 继承、多态 | 无原生继承概念 |
| 遍历方向 | 双向（`order.getItems()` / `item.getOrder()`）| 单向（通过外键 JOIN）|

**表现一：多对多关系**
```java
// OO：两端各一个 Set，自由导航
class Student { Set<Course> courses; }
class Course { Set<Student> students; }
```
关系数据库中，多对多**必须引入中间表**：
```sql
CREATE TABLE enrollment (
    student_id BIGINT,
    course_id BIGINT,
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES student(id),
    FOREIGN KEY (course_id) REFERENCES course(id)
);
```
对象模型里没有"Enrollment"这个概念——`student.getCourses()` 直接得到课程集合。但底层 ORM 必须通过中间表（或中间实体）实现这个映射。这正是 `@ManyToMany` + `@JoinTable` 的背后含义。

**表现二：继承**
```java
abstract class Payment { Long id; BigDecimal amount; }
class CreditCardPayment extends Payment { String cardNumber; }
class WechatPayment extends Payment { String openId; }
```
OO 中继承天然存在。关系库中**没有"继承"这个操作**。JPA 提供三种策略来模拟：

| 策略 | 做法 | 优点 | 缺点 |
|------|------|------|------|
| `SINGLE_TABLE` | 所有子类字段塞一张表 + `dtype` 区分 | 查询最快（单表） | 大量 NULL 列，约束弱 |
| `JOINED` | 父类一张表，每个子类各一张表 JOIN | 强约束，无 NULL | 查询必 JOIN，稍慢 |
| `TABLE_PER_CLASS` | 每个具体类独立建表（包含父类字段） | 简单 | 多态查询要 UNION ALL |

每种都是"模拟"，没有哪种天生和 OO 继承完全等价。

**表现三：对象身份 vs 记录身份**
```java
// 同一个数据库行被加载两次
Book b1 = em.find(Book.class, 1L);
Book b2 = em.find(Book.class, 1L);
System.out.println(b1 == b2);  // true（同一持久化上下文内）

// 不同事务/线程各自加载
// 事务 A
Book b1 = repo.findById(1L).get();
// 事务 B
Book b2 = repo.findById(1L).get();
// b1 == b2 不成立——两个不同的 Java 对象
// b1.equals(b2) 是否成立——取决于你怎么写 equals
```
数据库中一行由主键值 `id=1` 唯一标识。但同一行被加载为两个 Java 对象后，它们内存地址不同（`==` 返回 `false`）。如果两个线程各改各的对象然后 `save`，后提交的会**静默覆盖**先提交的修改——经典"丢失更新"。

解决方式：乐观锁 `@Version`（在表中加 `version` 字段，更新时比对版本号，不匹配就抛 `OptimisticLockException`）、悲观锁（`SELECT ... FOR UPDATE`）、或同持久化上下文内保持对象实例唯一（一级缓存）。

---

### Q24：IDENTITY 与 SEQUENCE 在并发插入时各有何特征？

这是主键生成策略中最关键的两个选择，直接影响批量插入性能。

**IDENTITY（自增列）**
```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long id;
```
对应 MySQL 的 `AUTO_INCREMENT`。机制：由数据库在执行 `INSERT` 时自动生成下一个值。

**致命限制：无法做 JDBC batch insert**。原因：
1. JDBC batch 的工作原理是把多条 INSERT 语句攒在一起一次发给数据库
2. 但 IDENTITY 模式下，必须等数据库执行完 `INSERT`、返回生成的 ID 后，Hibernate 才知道这条记录的 ID
3. 如果 batch 里有 100 条 INSERT，每条都必须等待前一条执行完才能获取下一批的 ID → **JDBC 驱动会逐条执行**，batch 形同虚设

对于需要批量导入大量数据的场景（如数据迁移、日志写入），IDENTITY 是性能灾难。

**SEQUENCE（序列）**
```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "book_seq")
@SequenceGenerator(name = "book_seq", allocationSize = 50)
private Long id;
```
对应 PostgreSQL/Oracle 的 SEQUENCE 对象。Hibernate 默认使用 `allocationSize = 50` 的**预取优化**：

1. Hibernate 调用一次 `SELECT nextval('book_seq')` 从数据库拿到 `N`（序列当前值）
2. 数据库端序列值一次性跳到 `N + 50`
3. Hibernate 在应用内存中获得了 `N ~ N+49` 这 50 个 ID 的"配额"，可以在组装完所有对象后**批量提交 INSERT**

100 条插入 → 只调用 2 次 `nextval` + 2 次 batch INSERT，DB 往返大幅减少。

**代价**：ID 序列不严格连续——应用重启导致内存中的未用 ID 丢失、应用多实例各自预取不同区间的 ID，最终 ID 序列可能是 1, 2, 3, 51, 52, 53（跳号）。对于要求 ID 严格连续的业务（如发票号、法律文书号），需用其他策略。

**MySQL 为什么不支持 SEQUENCE？**
MySQL 本身没有 SEQUENCE 对象（MySQL 8.0+ 对 `SEQUENCE` 的支持有限）。所以在 MySQL 上只能用 IDENTITY 或用 `TABLE` 策略（建一张专门存序列号的表模拟 SEQUENCE，但性能更差）。

**最佳实践**：如果可以选数据库，PostgreSQL + SEQUENCE 是批量写入性能明显优于 MySQL + IDENTITY。

---

### Q25：什么是 N+1 查询问题？怎么避免？

**问题描述**
N+1 查询是 ORM 最常见的性能陷阱：查询 N 个父实体后，遍历访问每个父实体的懒加载关联属性，各触发一次额外的 SQL，总计 N+1 次数据库往返。

**具体例子**
```java
// 查询所有价格 > 50 的书（1 次 SQL）
List<Book> books = bookRepository.findByPriceGreaterThan(new BigDecimal("50"));
// 假设返回 100 本书

// 遍历输出每本书的作者名
for (Book book : books) {
    System.out.println(book.getAuthor().getName());
    // 每次 book.getAuthor() 触发一次懒加载 SQL 取作者信息
    // 100 本书 = 100 次额外 SQL
}
// 总计: 1 + 100 = 101 次 SQL 往返
```
数据库往返次数 = `1(初始查询) + N(每条结果触发的懒加载)`。

如果进一步嵌套（书的出版社、作者的国家...），N 次变 N×M 次——爆炸式增长。

**为什么会发生？**
JPA 的懒加载默认行为：`@ManyToOne` 默认 EAGER（立即加载），`@OneToMany` 默认 LAZY（按需加载）。当关联被标记为 LAZY 时，Hibernate 只生成一个代理对象占位，直到代码真正访问该关联的属性时才发 SQL 查询。上面的循环就是每本书的 `getAuthor()` 首次触发代理加载。

**三种解决方案**

1. **JOIN FETCH（最常用）**：在 JPQL 中显式要求一次 SQL 把关联数据全拉回
   ```java
   @Query("SELECT b FROM Book b JOIN FETCH b.author WHERE b.price > :price")
   List<Book> findByPriceGreaterThan(@Param("price") BigDecimal price);
   ```
   生成的 SQL 是 `SELECT b.*, a.* FROM book b JOIN author a ON b.author_id = a.id WHERE b.price > 50`，一次请求返回所有数据。

2. **@EntityGraph**：在 Repository 方法上声明加载图
   ```java
   @EntityGraph(attributePaths = {"author"})
   List<Book> findByPriceGreaterThan(BigDecimal price);
   ```
   效果等同于 JOIN FETCH，但不需要写 JPQL，更简洁。还可以定义命名的 `@NamedEntityGraph` 复用。

3. **全局策略：默认全部 LAZY**
   实践中建议把 `@ManyToOne` 也从默认的 EAGER 改为显式 LAZY：
   ```java
   @ManyToOne(fetch = FetchType.LAZY)
   private Author author;
   ```
   这样不主动 JOIN FETCH 就不会拉关联数据，避免"无意中"的 N+1。需要的场景显式 FETCH。

**注意事项**：JOIN FETCH 写多了也有代价——如果 `Author` 本身还有超大关联（如作者的全部著作列表），一次性全拉回来可能变成"一卡车"数据，比 N+1 还糟糕。需权衡关联数据量和访问频率。

---

### Q26：`mappedBy` 在双向关联中起什么作用？

**问题场景**：订单（Order）和订单项（OrderItem）是双向一对多关系：
```java
@Entity
public class Order {
    @OneToMany
    private List<OrderItem> items;  // Order 知道自己的所有项
}

@Entity
public class OrderItem {
    @ManyToOne
    private Order order;  // OrderItem 知道属于哪个订单
}
```

在数据库中，这个关系只需要**一张表和一根外键**就能表达：`order_item` 表里有一个 `order_id` 外键列指向 `order` 表。

**如果不写 `mappedBy`**
JPA 看到双方都有关系注解，会认为这是**两个独立的关系**：
- `OrderItem.order` 的 `@ManyToOne` → 在 `order_item` 表上创建外键 `order_id`
- `Order.items` 的 `@OneToMany` → 在 `order` 和 `order_item` 之间创建一张**中间表** `order_order_item`，存 `(order_id, item_id)` 对

结果：数据库中出现了**两份**关系表达——`order_item.order_id` 外键 + `order_order_item` 中间表。在关系上做了冗余，同一对关系被维护了两遍，更新时可能出现不一致。

**写了 `mappedBy` 之后**
```java
@Entity
public class Order {
    @OneToMany(mappedBy = "order")  // "order" 是 OrderItem 里那个字段名
    private List<OrderItem> items;
}
```
`mappedBy = "order"` 的含义：**"我不负责定义这个关系，我已经被 OrderItem 的 `order` 字段映射了。真正的外键在那边。"**

数据库结果：只有 `order_item.order_id` 一根外键，没有中间表。JPA 知道了 `Order.items` 和 `OrderItem.order` 是同一关系的两端而非两个独立关系。

**如何确定哪方写 `mappedBy`？**
记住规则：**外键在哪张表，哪方就是主控方；另一方就是被控方，写 `mappedBy`**。在 `@ManyToOne` + `@OneToMany` 的场景中，外键总是在"多"的那方表（`order_item.order_id`），所以 `@ManyToOne` 是主控方，`@OneToMany` 是被控方。

```java
// OrderItem 是主控方——它持有外键
@ManyToOne
@JoinColumn(name = "order_id")  // 外键在此定义
private Order order;

// Order 是被控方——它只是关系的反向引用
@OneToMany(mappedBy = "order", cascade = ALL, orphanRemoval = true)
private List<OrderItem> items = new ArrayList<>();
```

**业务操作上**：关联的建立/断开通俗做法是写一个辅助方法，同时处理两端：
```java
public void addItem(OrderItem item) {
    items.add(item);         // 维护集合
    item.setOrder(this);     // 维护外键引用（重要！否则 order_id 为 null）
}
```

---

## 六、安全与架构

### Q27：密码为什么必须用 bcrypt 而不是 SHA256？

**核心差异：速度**

SHA256 的设计目标是**快**——计算一个哈希极快，适合校验文件完整性、数字签名等场景。但这对密码存储是**灾难性的**：

- 一块消费级 GPU 每秒可计算 **数十亿次** SHA256
- 攻击者拿到数据库的 `(salt, hash)` 后，离线暴力穷举 8 位以下密码**只需几小时**
- 密码破解无需"黑进服务器"——数据库泄露一次，所有弱密码用户全部暴露

**bcrypt/Argon2 的设计哲学：故意慢**

bcrypt 引入了 **cost factor（工作因子）**，控制加密迭代次数：
```
cost = 10 → 2^10 = 1024 轮
cost = 12 → 2^12 = 4096 轮（推荐，每次约 100ms-300ms）
cost = 14 → 2^14 = 16384 轮（每次约 1s）
```

用户登录时多等 100ms-300ms 完全无感，但攻击者用 GPU 穷举时：
- 原本几小时能穷举完的密码空间，现在需要**几年甚至几十年**
- 每多一个 cost 级别，穷举时间翻倍

**bcrypt 的其他优势**

1. **自带 salt 管理**：bcrypt 的 hash 输出自带 salt（`$2a$10$...` 格式中包含了随机 salt 和 cost 参数），不需要额外存储和管理 salt 字段

2. **抗 GPU/ASIC 加速**：bcrypt 算法设计中使用了对 GPU 不友好的内存访问模式，GPU 并行加速效果远不如对 SHA256 明显。Argon2 更进一步，参数中显式指定内存消耗量，让专用硬件（FPGA/ASIC）的暴力破解成本高到不可行

**为什么不是 SHA256 + Salt 就够？**
加了 salt 只解决了"多个用户同密码 hash 不同"的问题（防彩虹表），没有解决"单次 hash 计算太快"的问题。攻击者只需要拿到 salt（salt 是公开存储的），对每个候选人密码 `sha256(salt + guess)` 计算一下，速度和攻击未加 salt 的情况几乎一样快——因为 SHA256 本身就是计算极快的。

**最佳实践**
- bcrypt：成熟、久经考验，Spring Security 默认推荐（`BCryptPasswordEncoder`）
- Argon2：2015 年密码哈希竞赛冠军，抗 GPU/ASIC 更优，内存消耗可配置，是新一代标准。Java 可通过 `argon2-jvm` 或 Spring Security 5+ 内置支持使用

---

### Q28：Docker 容器与虚拟机的核心区别是什么？为什么微服务偏好容器？

**架构差异**

| | 虚拟机（VM）| 容器（Docker）|
|------|-----------|-------------|
| 虚拟化层 | Hypervisor（如 VMware、KVM）虚拟化完整硬件（CPU、内存、磁盘、网卡）| Linux 内核的 cgroups + namespaces 做进程级资源隔离 |
| Guest OS | 每个 VM 运行完整操作系统（含内核），如 Ubuntu Server | **共享宿主机内核**，无独立 OS |
| 启动 | 完整 OS 启动流程 → 数十秒到分钟级 | 进程启动 → 毫秒级 |
| 镜像大小 | 含完整 OS → GB 级（如 Ubuntu VM 镜像 2-5GB）| 只含应用 + 依赖 → MB 级 |
| 隔离强度 | 强——硬件虚拟化，Hypervisor 隔离不同 VM 的硬件资源 | 中等——进程级隔离，共享内核 |
| 资源开销 | 每个 VM 独立内核占用内存和 CPU | 无独立内核，仅应用进程 |

**为什么微服务偏好容器？**

1. **实例数量多**：微服务架构下，一个应用拆成十几个甚至几十个服务。用 VM——每个服务一个 VM = 几十个完整 OS 在跑，资源浪费巨大。用容器——每个服务一个容器，所有容器共享宿主机的内核，资源利用率接近裸机。

2. **生命周期短**：微服务实例需要快速启动（自动扩缩容、滚动更新、故障恢复时新实例秒级上线）。VM 分钟级启动无法满足弹性伸缩需求；容器毫秒级启动使**水平自动扩缩（HPA）**成为现实。

3. **镜像分发快**：CI/CD 流水线中频繁构建和部署。MB 级容器镜像推送到镜像仓库只需几秒；GB 级 VM 镜像传输是瓶颈。

4. **环境一致性**：容器镜像把应用 + JDK + 系统库 + 配置全部打包为一个不可变单元。"本机能跑，服务器不行"的问题大幅减少——同一镜像在开发、测试、生产环境行为一致。

**容器的隔离不是"完美"的**
由于共享内核，容器之间的隔离不如 VM。一个容器内的 root 用户如果有内核漏洞，可能突破到其他容器或宿主机。因此：
- 安全要求极高的场景（政府、金融核心系统）可能仍用 VM
- 云厂商常用"VM 里跑容器"的组合——用 VM 做租户级隔离，内部用容器编排微服务
- Kubernetes 集群中需要配置 SecurityContext、PodSecurityPolicy 等加固措施

---

## 七、关键对比速记

| 对比 | 结论 |
|------|------|
| `@ManyToOne` vs `@OneToMany` 默认 fetch | `@ManyToOne` 默认 **EAGER**；`@OneToMany` 默认 **LAZY**（实践中建议全改 LAZY）|
| JPQL vs Native SQL | JPQL 操作实体类，跨数据库可移植；Native SQL 可用数据库特有功能（窗口函数、全文索引），但绑死数据库 |
| 行存 vs 列存 | 行存 → **OLTP**（整行 CRUD，MySQL/PG）；列存 → **OLAP**（聚合分析，ClickHouse/Parquet）|
| Maven vs Gradle | Maven 约定优于配置、生态最广；Gradle 灵活 DSL、增量构建快、Android 默认 |
| 积极加载 vs 懒加载 | EAGER → 聚合关系（订单+订单项几乎总是一起用）；LAZY → 子集合大或不常访问，需要时 JOIN FETCH |
| IDENTITY vs SEQUENCE | IDENTITY 无法 batch insert；SEQUENCE 可预取批量 ID、适合高并发写入 |
| CHAR vs VARCHAR | CHAR(N) 定长，O(1) 直接寻址（适合固定格式如手机号/UUID）；VARCHAR(N) 变长省空间（适合姓名/地址）|
| Session vs JWT | Session：服务端状态，需 Redis 共享；JWT：无状态，token 自带身份信息，天然支持水平扩展 |
| VM vs Docker | VM：硬件虚拟化 + 独立 OS，强隔离、重；Docker：cgroups + 共享内核，轻量、毫秒启动 |

---

> 核心原则：理解"为什么"比记住"是什么"更重要。每个问题背后都是生产环境真实踩过的坑。
