# Java + Spring + 数据库 核心复习问答总结（详细版）

> 在原《核心复习问答总结》基础上扩写。每个问题都按“核心结论 → 详细解释 → 例子/常见坑 → 答辩模板”整理，适合答辩前复习和口头表达。

---

## 一、JVM 与 Java 核心

### Q1：类加载的「加载 → 链接 → 初始化」三大阶段各做什么？

**核心结论：** 类加载是 JVM 把 `.class` 字节码变成可运行时数据结构的过程，主要分为加载、链接、初始化。链接又包括验证、准备、解析。

**详细解释：**

1. **加载 Loading**：JVM 根据类的全限定名找到 `.class` 字节流，把它读入内存，并在方法区生成类的元数据，同时在堆中创建一个 `Class` 对象，作为程序访问该类元信息的入口。字节码不一定来自磁盘，也可以来自 jar 包、网络、自定义 ClassLoader 或动态生成。
2. **链接 Linking**：把已经加载进来的类数据变成 JVM 可以安全使用的结构。它包含三步：
   - **验证**：检查 class 文件格式、字节码指令、类型安全等，防止恶意或非法字节码破坏 JVM。
   - **准备**：给 `static` 静态变量分配内存并赋默认零值。例如 `static int count = 10`，准备阶段先是 `0`，不是 `10`。
   - **解析**：把符号引用转换成直接引用。符号引用可以理解为“通过名字描述目标”，直接引用则是真正能定位到目标类、字段、方法的引用。
3. **初始化 Initialization**：执行类构造器 `<clinit>`，给静态变量赋程序员写的真实值，并执行 `static {}` 静态代码块。JVM 保证一个类的初始化线程安全且只执行一次。

**例子：**

```java
class User {
    static int count = 10;
    static {
        System.out.println("User init");
    }
}
```

准备阶段 `count = 0`，初始化阶段才变成 `10`，并执行静态代码块。

**常见追问：什么时候触发初始化？**

常见触发时机包括：`new` 对象、读取或设置静态字段、调用静态方法、反射使用类、初始化子类前先初始化父类、JVM 启动主类。

**答辩模板：** 类加载分为加载、链接、初始化。加载负责把 `.class` 字节码读入内存并创建 `Class` 对象；链接负责验证安全性、给静态变量分配默认值、把符号引用解析成直接引用；初始化阶段才执行静态变量的真实赋值和静态代码块。这个过程让 JVM 能安全、正确地使用 Java 类。

---

### Q2：Java 为什么需要 JIT，而不是像 C++ 一样直接编译成机器码？

**核心结论：** Java 需要 JIT 是为了同时兼顾跨平台和性能。字节码保证跨平台，JIT 把热点代码编译成本地机器码提升性能。

**详细解释：**

C++ 通常在运行前就编译成本地机器码，这种方式启动时执行快，但不同系统、不同 CPU 架构需要编译不同版本。Java 选择先编译成平台无关的 `.class` 字节码，再交给不同平台的 JVM 执行，因此实现“一次编译，到处运行”。

但如果 JVM 永远逐条解释字节码，性能会比较低。于是 JVM 引入 JIT，即即时编译器。程序运行时，JVM 会监控哪些方法或循环执行频率很高，这些代码称为**热点代码**。JIT 会把热点字节码编译成当前机器可以直接执行的本地机器码。

**JIT 能做的优化：**

- **热点编译**：只优化经常执行的代码，避免全量编译浪费。
- **方法内联**：把小方法展开到调用处，减少方法调用开销。
- **逃逸分析**：判断对象是否只在方法内部使用，可能进行栈上分配或标量替换，减少 GC 压力。
- **锁消除**：如果发现对象不会被多线程竞争，JIT 可以去掉无意义的锁。
- **基于运行时类型优化**：JIT 能观察程序真实运行时的类型分布，做静态编译器无法提前知道的优化。

**常见坑：** JIT 有预热成本。短生命周期程序可能还没来得及充分优化就结束了，所以 JIT 对长期运行的服务端程序更有价值。

**答辩模板：** Java 不直接编译成本地机器码，是为了跨平台。Java 先编译成平台无关的字节码，再由不同平台 JVM 执行。为了弥补解释执行性能不足，JVM 使用 JIT 在运行时识别热点代码，并把它编译成本地机器码。JIT 还能利用运行时信息做方法内联、逃逸分析、锁消除等优化，所以长期运行的 Java 服务性能可以很高。

---

### Q3：JVM 的双亲委派机制为什么要存在？

**核心结论：** 双亲委派机制是类加载器收到加载请求后，先交给父加载器加载，父加载器加载不了，子加载器才自己加载。它主要保证核心类安全和类加载唯一性。

**详细解释：**

JVM 中常见类加载器层级：

```text
Bootstrap ClassLoader
        ↑
Platform ClassLoader
        ↑
Application ClassLoader
        ↑
自定义 ClassLoader
```

当应用类加载器要加载一个类时，不会立刻自己找 classpath，而是先委托父加载器。请求会一路向上到 Bootstrap。若上层能加载，就由上层加载；如果都加载不了，才由下层自己加载。

**为什么这样设计？**

1. **安全性**：防止用户伪造核心类。比如用户写一个 `java.lang.String`，如果应用类加载器优先加载它，就会破坏 Java 核心类库。双亲委派会先让 Bootstrap 加载真正的 `String`，避免核心类被替换。
2. **唯一性**：JVM 判断两个类是否相同，不只看类名，还看加载它的 ClassLoader。也就是“全限定名 + 类加载器”共同决定类身份。双亲委派能保证核心类由同一个上层加载器加载，避免重复加载导致 `instanceof`、强制类型转换失败。

**Tomcat 为什么会打破双亲委派？**

Web 容器里可能同时部署多个项目，不同项目可能依赖不同版本的 Spring 或其他 jar。Tomcat 需要让每个 Web 应用优先加载自己 `WEB-INF/lib` 下的类，实现应用间隔离，所以它会在某些场景下打破传统双亲委派。

**答辩模板：** 双亲委派机制是类加载器先把类加载请求交给父加载器，父加载器加载不了时子加载器才加载。这样可以防止用户伪造 `java.lang.String` 等核心类，保证核心类库安全；也能保证核心类只被统一加载一次，避免类身份混乱。Tomcat 打破它主要是为了让不同 Web 应用使用自己的依赖版本，实现隔离。

---

### Q4：Java 不需要 virtual 关键字也能多态，底层机制是什么？

**核心结论：** Java 普通实例方法默认就是虚方法，运行时通过动态分派，根据对象实际类型决定调用哪个方法。

**详细解释：**

多态指父类引用指向子类对象时，调用方法会执行子类重写后的实现。

```java
Animal a = new Dog();
a.speak(); // 调用 Dog 的 speak
```

Java 和 C++ 不同，C++ 需要 `virtual` 显式声明虚函数，而 Java 中普通实例方法默认支持动态绑定。编译后，普通实例方法调用通常对应字节码指令 `invokevirtual`。执行时 JVM 不只看变量声明类型，而是看对象的实际类型，然后到该类型的方法表中查找目标方法。

**不参与普通多态的方法：**

- `static`：属于类，不属于对象，方法调用按声明类型解析。
- `final`：不能被重写，不需要动态分派。
- `private`：对子类不可见，不能真正重写。
- 构造方法：不能被继承和重写。

**答辩模板：** Java 普通实例方法默认就是虚方法，不需要像 C++ 那样写 `virtual`。当父类引用调用方法时，字节码通常使用 `invokevirtual`，JVM 会根据对象的实际类型在方法表中查找真正执行的方法，这就是动态绑定。`static`、`final`、`private` 方法不参与普通多态。

---

### Q5：为什么重写 equals 必须同时重写 hashCode？

**核心结论：** 因为 Java 规定两个对象如果 `equals` 相等，它们的 `hashCode` 必须相同。哈希容器依赖这个契约工作。

**详细解释：**

`equals()` 用来判断两个对象在业务上是否相等，`hashCode()` 用来生成哈希值，主要服务于 `HashMap`、`HashSet` 等哈希容器。

HashMap 查找 key 的过程：

```text
先算 hashCode 定位桶
再在桶里用 equals 判断具体对象
```

如果只重写 `equals`，不重写 `hashCode`，两个业务相等的对象可能 hashCode 不同，于是会落入不同桶中。这样即使 `equals` 返回 true，HashMap 也可能找不到。

**例子：**

```java
User u1 = new User(1L);
User u2 = new User(1L);

u1.equals(u2);      // true
u1.hashCode() == u2.hashCode(); // false，如果没重写 hashCode
```

```java
map.put(u1, "Tom");
map.get(u2); // 可能是 null
```

**答辩模板：** 哈希容器先用 `hashCode` 定位桶，再用 `equals` 比较桶内对象。Java 的契约要求如果两个对象 `equals` 相等，那么它们的 `hashCode` 必须相同。如果只重写 `equals` 不重写 `hashCode`，业务相等的对象可能进入不同桶，导致 `HashMap.get` 找不到、`HashSet` 去重失败。因此二者必须一起重写。

---

### Q6：Lambda 表达式底层怎么实现？跟匿名内部类有何区别？

**核心结论：** 匿名内部类通常编译生成额外 class 文件；Lambda 主要通过 `invokedynamic` 和 `LambdaMetafactory` 在运行时生成函数式接口实现，更轻量。

**详细解释：**

Lambda 是函数式接口的简洁写法。函数式接口是只有一个抽象方法的接口，例如 `Runnable`、`Callable`、`Function`、`Consumer`。

匿名内部类：

```java
Runnable r = new Runnable() {
    public void run() {
        System.out.println("hi");
    }
};
```

编译后通常生成 `Outer$1.class`。

Lambda：

```java
Runnable r = () -> System.out.println("hi");
```

底层使用 `invokedynamic` 指令，由 `LambdaMetafactory` 在运行时生成目标函数式接口的实现。无捕获 Lambda 还可能复用同一个实例。

**主要区别：**

| 对比 | 匿名内部类 | Lambda |
|---|---|---|
| 适用范围 | 接口、抽象类 | 只能用于函数式接口 |
| 底层 | 编译期生成类 | invokedynamic 运行时生成 |
| `this` 指向 | 匿名内部类对象 | 外层对象 |
| 简洁性 | 较啰嗦 | 简洁 |
| 性能与类数量 | 可能类文件较多 | 更轻量 |

**答辩模板：** Lambda 是函数式接口的简洁实现。匿名内部类会在编译期生成额外的内部类 class 文件，而 Lambda 主要依赖 `invokedynamic` 和 `LambdaMetafactory` 在运行时生成实现，减少类文件数量，也方便 JVM 优化。语义上匿名内部类中的 `this` 指向匿名内部类对象，Lambda 中的 `this` 指向外层对象。

---

### Q7：虚拟线程相比传统线程为什么能扛百万并发？什么场景不适合？

**核心结论：** 传统线程通常一对一绑定操作系统线程，成本高；虚拟线程由 JVM 调度，数量更轻量，阻塞时可以释放底层 carrier thread，适合 IO 密集高并发。

**详细解释：**

传统 Java `Thread` 通常对应 OS 线程。OS 线程创建成本高、栈空间大、上下文切换贵，所以数量不能无限增加。

虚拟线程是 JVM 管理的轻量级线程：

```text
大量虚拟线程 → 少量 carrier thread → OS 线程
```

当虚拟线程执行数据库查询、HTTP 请求等 IO 阻塞操作时，JVM 可以挂起虚拟线程，把底层 carrier thread 释放出来执行其他虚拟线程。因此它能用同步代码风格支撑大量并发请求。

**适合场景：**

- Web 请求并发高；
- 大量等待数据库、Redis、HTTP、文件 IO；
- 希望保留同步代码风格，避免复杂异步回调。

**不适合场景：**

- CPU 密集计算：虚拟线程不能增加 CPU 核心数；
- 长时间持有 `synchronized`：可能钉住 carrier thread；
- 大量使用 `ThreadLocal`：虚拟线程数量大时内存压力高。

**答辩模板：** 虚拟线程能扛高并发，是因为它不是和操作系统线程一对一绑定，而是由 JVM 在少量 carrier thread 上调度。IO 阻塞时虚拟线程可以挂起并释放底层线程，所以非常适合 IO 密集型互联网应用。但它不适合 CPU 密集任务，也要避免长时间 `synchronized` 和大量 `ThreadLocal`。

---

## 二、Spring Boot 核心

### Q8：构造器注入 vs 字段注入 vs setter 注入，为什么推荐构造器注入？

**核心结论：** 推荐构造器注入，因为它依赖显式、支持 `final`、对象创建后状态完整、方便单元测试，也更容易发现循环依赖。

**详细解释：**

Spring 依赖注入的本质是对象不自己创建依赖，而是由容器提供依赖。

三种方式：

1. **字段注入**：

```java
@Autowired
private UserRepository userRepository;
```

优点是简单，缺点是依赖隐藏，不能 `final`，单测不方便，容易让类依赖过多而不明显。

2. **setter 注入**：

```java
@Autowired
public void setUserRepository(UserRepository userRepository) { ... }
```

适合可选依赖，但对象创建后依赖还能被修改。

3. **构造器注入**：

```java
private final UserRepository userRepository;

public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
}
```

构造器注入让对象创建时就拿到必需依赖，对象一旦构造完成就是可用状态。

**常用写法：**

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
}
```

**答辩模板：** 字段注入虽然简洁，但依赖不明显，字段不能 `final`，单测也不方便。setter 注入适合可选依赖。构造器注入能让依赖在构造对象时显式传入，支持 `final` 保证不可变，也方便单元测试和暴露循环依赖，所以 Spring 项目中更推荐构造器注入。

---

### Q9：分层架构里事务边界为什么放 Service 不放 Controller 或 Repository？

**核心结论：** 事务边界应该和一次完整业务操作一致，而 Service 正是业务编排层，所以事务应放在 Service。

**详细解释：**

一次业务通常涉及多个数据库操作。例如下单：

```text
创建订单 → 扣库存 → 扣余额 → 写订单明细
```

这些操作必须要么都成功，要么都失败，这就是事务的原子性。

**为什么不放 Controller？** Controller 是 HTTP 接入层，负责接收请求、参数校验、调用 Service、返回响应。它不应该包含业务事务逻辑，否则 Web 层和业务层耦合，后续如果业务被消息队列、定时任务调用，事务就无法复用。

**为什么不放 Repository？** Repository 通常只负责单表或单实体的数据访问。如果每个 Repository 自己开事务，一次业务会被拆成多个事务。例如订单保存成功提交了，但库存扣减失败回滚，就会造成数据不一致。

**为什么放 Service？** Service 负责组织多个 Repository 完成一个业务用例，天然对应事务边界。

```java
@Transactional
public void createOrder(CreateOrderRequest request) {
    orderRepository.save(order);
    stockRepository.decrease(productId);
    accountRepository.decrease(userId);
}
```

**答辩模板：** 事务应该放在 Service 层，因为 Service 表示一次完整业务操作。Controller 是接入层，不应该关心事务；Repository 是数据访问层，单个 Repository 方法不能代表完整业务。如果事务放在 Repository，多个数据库操作会分散在多个事务里，破坏原子性。Service 能把多个 Repository 调用包在同一个事务中，保证业务一致性。

---

### Q10：Entity 能不能直接返给前端？为什么必须用 DTO？

**核心结论：** 不建议直接返回 Entity。Entity 是数据库持久化模型，DTO 是接口传输模型，二者职责不同。

**详细解释：**

Entity 对应数据库表，包含持久化需要的字段和关联关系。DTO 面向前端接口，只暴露接口需要的数据。

直接返回 Entity 的问题：

1. **泄露敏感字段**：Entity 可能包含成本价、密码、内部状态、删除标记等前端不该知道的字段。
2. **接口和数据库强耦合**：数据库表字段变化会直接影响 API 返回结构，破坏接口稳定性。
3. **JPA 关联递归**：双向关联如 `Order → items → Order` 可能导致 JSON 无限递归。
4. **不同接口字段需求不同**：列表页、详情页、创建请求、更新请求需要的字段不一样，一个 Entity 很难适配全部。
5. **懒加载异常**：Entity 返回到 Controller 后，如果持久化上下文关闭，序列化懒加载字段可能出现异常。

DTO 的好处：

- 只暴露必要字段；
- 保持 API 稳定；
- 避免循环引用；
- 方便做参数校验；
- 解耦数据库结构和前端接口。

**答辩模板：** Entity 不应该直接返回给前端，因为 Entity 是数据库模型，可能包含敏感字段，也可能有 JPA 双向关联导致无限递归，还会让 API 和表结构强耦合。DTO 是专门用于接口传输的对象，可以按接口场景暴露合适字段，隐藏内部实现，保持接口稳定。所以 Controller 层一般应该把 Entity 转成 DTO 再返回。

---

### Q11：Spring Data JPA 的 Repository 接口不写实现为什么能用？

**核心结论：** Spring Data JPA 在启动时会扫描 Repository 接口，并动态生成代理实现类，放入 Spring 容器。

**详细解释：**

我们常写：

```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

虽然没有实现类，但 Spring Data JPA 会在容器启动时：

1. 扫描继承 `Repository` / `JpaRepository` 的接口；
2. 读取泛型信息，知道实体类型是 `User`，主键类型是 `Long`；
3. 给基础 CRUD 方法绑定默认实现；
4. 解析自定义方法名，例如 `findByUsername`；
5. 运行时创建代理对象；
6. 把代理对象注册到 Spring 容器中。

`findByUsername` 会被解析为类似：

```sql
where username = ?
```

复杂查询可以用 `@Query` 编写 JPQL 或 Native SQL。

**答辩模板：** Repository 接口不写实现也能用，是因为 Spring Data JPA 会在启动时扫描这些接口，根据泛型知道实体和主键类型，并通过动态代理生成实现类。继承 `JpaRepository` 后，基础 CRUD 已有默认实现；自定义方法如 `findByUsername` 会按照方法名约定解析成查询。运行时注入的是 Spring 生成的代理对象。

---

## 三、数据库理论与 SQL

### Q12：为什么说 SQL 是声明式的？这给数据库带来什么权力？

**核心结论：** SQL 只描述“要什么结果”，不描述“怎么执行”，因此数据库优化器可以自由选择执行计划。

**详细解释：**

例如：

```sql
select * from user where age > 18;
```

这条 SQL 只告诉数据库要找年龄大于 18 的用户，但没有告诉数据库是否用索引、从哪一行开始扫、怎么读取磁盘页。

再比如 JOIN：

```sql
select * from orders o
join user u on o.user_id = u.id
where u.city = 'Beijing';
```

数据库可以选择：

- 先查用户再查订单；
- 先查订单再关联用户；
- 使用 Hash Join、Merge Join 或 Nested Loop Join；
- 使用索引或全表扫描；
- 改变条件执行顺序。

这就是声明式语言带来的权力：优化器可以根据表大小、索引、统计信息、数据分布等选择成本最低的执行计划。

**答辩模板：** SQL 是声明式语言，因为它只说明要查询什么结果，而不说明具体怎么遍历数据。这样数据库优化器就可以根据索引、表大小、统计信息等选择执行计划，比如决定 JOIN 顺序、JOIN 算法、是否使用索引。程序员关注结果，数据库负责寻找高效执行方式。

---

### Q13：候选键、超键、主键的关系是什么？

**核心结论：** 超键能唯一标识一行，可以有多余字段；候选键是最小超键；主键是从候选键中选出的主要标识。

**详细解释：**

假设学生表：

```text
student(id, student_no, id_card, name)
```

如果 `id`、`student_no`、`id_card` 都能唯一标识学生，那么它们都可能是候选键。

- **超键**：只要能唯一标识一行就是超键，例如 `{id}`、`{student_no}`、`{id, name}`。
- **候选键**：不能再删字段的超键。例如 `{id}` 是候选键，但 `{id, name}` 不是，因为 `name` 多余。
- **主键**：从候选键中选择一个作为表的主要标识。主键要求唯一、非空、稳定，数据库通常会为主键建立索引。

**注意：** “最小”不是字段数量最少，而是不可再缩减。对于联合候选键，只要去掉任意字段后不能唯一标识，它就是最小的。

**答辩模板：** 超键是能唯一标识一行的任意属性集合，可能包含多余字段；候选键是最小超键，去掉任何字段都不能再唯一标识；主键是从候选键中选定的一个，用作表的主要标识，并强制非空和唯一。

---

### Q14：B+ 树为什么比二叉树更适合数据库索引？

**核心结论：** 数据库索引的关键是减少磁盘 IO。B+ 树分叉多、树高低、叶子有序链表适合范围查询，因此比二叉树更适合数据库。

**详细解释：**

二叉树每个节点最多两个孩子，即使是平衡二叉树，百万级数据高度也大约 20 层。如果每访问一层都可能产生一次磁盘 IO，成本很高。

B+ 树是多路平衡树，一个节点可以存很多 key 和指针。数据库通常让一个节点对应一个磁盘页，例如 16KB。这样一个节点能容纳大量索引项，树会非常矮。几百万甚至上亿数据，B+ 树高度通常也只有 3 到 4 层。

B+ 树还有一个重要特点：所有数据都在叶子节点，叶子节点之间通过有序链表连接。因此范围查询非常高效：先定位范围起点，再沿叶子链表顺序扫描。

**答辩模板：** B+ 树适合数据库索引，核心原因是能减少磁盘 IO。二叉树分叉少，数据量大时树高较高，查询需要访问很多层。B+ 树一个节点能存大量 key，分叉多，树高低，通常几次 IO 就能定位数据。而且 B+ 树叶子节点有序相连，非常适合范围查询和排序扫描。

---

### Q15：聚簇索引为什么每表只能有一个？二级索引怎么工作？

**核心结论：** 聚簇索引决定数据的物理组织顺序，而物理顺序只能有一种，所以每表只能有一个。二级索引叶子节点存索引列和主键值，查询完整行时可能需要回表。

**详细解释：**

在 InnoDB 中，主键索引通常就是聚簇索引。聚簇索引的 B+ 树叶子节点存放完整行数据：

```text
主键 B+ 树叶子节点 = 完整记录
```

一张表的数据只能按一种方式物理组织，就像一本书只能按一种主顺序装订，所以聚簇索引只能有一个。

其他索引都是二级索引。例如：

```sql
create index idx_name on user(name);
```

二级索引叶子节点通常存：

```text
name 值 + 主键 id
```

如果执行：

```sql
select * from user where name = 'Tom';
```

流程是：

```text
查 name 二级索引 → 找到主键 id → 根据 id 查聚簇索引 → 得到完整行
```

第二步根据主键再查完整行，叫**回表**。

如果查询字段都在索引中，例如：

```sql
select name from user where name = 'Tom';
```

就不需要回表，这叫**覆盖索引**。

**答辩模板：** 聚簇索引决定表数据的物理存储顺序，而物理顺序只能有一种，所以每张表只能有一个聚簇索引。InnoDB 中主键索引的叶子节点存完整行数据，其他二级索引叶子节点存索引列和主键值。通过二级索引查完整记录时，需要先找到主键，再根据主键查聚簇索引，这个过程叫回表。

---

### Q16：1NF、2NF、3NF 的核心要求分别是什么？

**核心结论：** 1NF 要求字段原子；2NF 消除对联合主键的部分依赖；3NF 消除非主键字段之间的传递依赖。

**详细解释：**

**第一范式 1NF：字段原子不可再分。**

错误例子：

```text
user(id, name, phones)
phones = '138...,139...'
```

这样手机号难以查询、索引、更新。应该拆成用户表和手机号表。

**第二范式 2NF：非主键字段依赖完整主键。**

常见于联合主键表：

```text
student_course(student_id, course_id, student_name, course_name, score)
```

主键是 `(student_id, course_id)`，但 `student_name` 只依赖 `student_id`，`course_name` 只依赖 `course_id`，这就是部分依赖。应拆成学生表、课程表、选课表。

**第三范式 3NF：非主键字段不能依赖其他非主键字段。**

错误例子：

```text
student(id, name, dept_id, dept_name, dept_dean)
```

`dept_name`、`dept_dean` 依赖 `dept_id`，而 `dept_id` 是非主键字段，这就是传递依赖。应拆出部门表。

**范式解决的问题：**

- 更新异常；
- 插入异常；
- 删除异常；
- 不合理数据冗余。

**答辩模板：** 1NF 要求字段值原子不可再分；2NF 要求非主键字段依赖完整主键，主要解决联合主键下的部分依赖；3NF 要求非主键字段不能依赖其他非主键字段，消除传递依赖。范式的目的是减少不合理冗余，避免更新、插入、删除异常。

---

### Q17：订单中冗余 unit_price_snapshot 跟范式“消除冗余”矛盾吗？

**核心结论：** 不矛盾。商品当前价格和订单成交价格是两个不同事实，订单价格快照是合理冗余。

**详细解释：**

商品表中的 `price` 表示商品当前价格。订单中的 `unit_price_snapshot` 表示用户下单那一刻的成交单价。

如果订单不保存价格快照，只引用商品当前价格，那么商品涨价后，历史订单金额也会变化，这显然是错误的。

所以：

```text
product.price = 当前价格
order_item.unit_price_snapshot = 历史成交价格
```

二者不是同一个事实。

范式反对的是同一事实重复存储导致不一致，而业务快照记录的是历史事实，常见于订单、支付、合同、发票等场景。

**答辩模板：** 订单中的 `unit_price_snapshot` 不违背范式精神。范式要消除的是同一事实的重复存储，但商品表价格表示当前价格，订单价格快照表示下单时的历史成交价格，是两个不同业务事实。商品价格变化不能影响历史订单，所以这种冗余是合理的业务快照设计。

---

## 四、JDBC 与数据访问

### Q18：SQL 注入的根本原因是什么？为什么 PreparedStatement 能挡住？

**核心结论：** SQL 注入的根本原因是把用户输入拼接进 SQL，使输入被当作 SQL 语法解析。PreparedStatement 通过 SQL 模板和参数分离防止注入。

**详细解释：**

危险写法：

```java
String sql = "select * from user where username='" + username
           + "' and password='" + password + "'";
```

如果用户输入：

```text
' or '1'='1
```

拼接后可能改变原 SQL 逻辑，从而绕过登录或读取非法数据。

PreparedStatement 写法：

```java
String sql = "select * from user where username=? and password=?";
PreparedStatement ps = conn.prepareStatement(sql);
ps.setString(1, username);
ps.setString(2, password);
```

数据库先接收带 `?` 的 SQL 模板，SQL 结构已经确定。后续参数按类型单独传入，只会被当作普通字面量，不会再被解析成 SQL 关键字或操作符。

**额外好处：**

- 类型安全，如 `setInt`、`setString`；
- 代码更清晰；
- 部分数据库可以复用执行计划。

**答辩模板：** SQL 注入的根本原因是用户输入和 SQL 语句字符串拼接，导致输入内容被数据库当作 SQL 语法执行。PreparedStatement 使用占位符，先把 SQL 模板预编译，SQL 结构已经固定，参数再按类型单独传入，只作为普通数据值，不会改变 SQL 逻辑，因此能防止 SQL 注入。

---

### Q19：连接池大小为什么不是越大越好？

**核心结论：** 连接池能复用连接，但连接数过多会消耗数据库资源，增加线程切换、锁竞争和内存开销，吞吐可能反而下降。

**详细解释：**

数据库连接不是普通对象。每个连接在数据库端通常都要占用会话状态、内存、文件描述符、工作线程或任务结构。

连接池太小，请求会排队等待连接，应用吞吐受限。连接池太大，数据库会面对过多并发 SQL：

- CPU 核数有限，过多线程导致上下文切换；
- 内存和文件描述符消耗增大；
- 锁竞争变严重；
- 慢 SQL 同时堆积，数据库更容易被拖垮；
- 连接多不等于数据库处理能力强。

合理连接池大小需要结合数据库 CPU、SQL 平均耗时、业务并发、连接等待时间、慢查询、锁等待等指标调优。实际项目中通常从几十级别开始，而不是几百几千盲目增大。

**答辩模板：** 连接池不是越大越好。连接池的作用是复用连接并控制并发访问数据库的数量。每个连接都会占用数据库端资源，连接过多会带来线程上下文切换、锁竞争、内存占用和 IO 竞争，可能让吞吐下降。合理大小要根据数据库能力、SQL 执行时间和业务并发调优。

---

### Q20：JdbcTemplate 解决了哪些原生 JDBC 痛点？仍未解决什么？

**核心结论：** JdbcTemplate 解决了资源管理、异常转换、样板代码和结果映射问题，但仍需要手写 SQL，不能自动管理对象关系。

**详细解释：**

原生 JDBC 代码繁琐：获取连接、创建 PreparedStatement、设置参数、执行 SQL、遍历 ResultSet、关闭资源、处理 SQLException。任何一步都容易出错，尤其是资源关闭。

JdbcTemplate 提供模板方法：

```java
jdbcTemplate.query(sql, rowMapper, param);
jdbcTemplate.update(sql, params);
```

它解决了：

1. 自动关闭连接、Statement、ResultSet；
2. 把 `SQLException` 转成 Spring 的 `DataAccessException`；
3. 减少重复样板代码；
4. 用 RowMapper 简化行到对象的映射；
5. 支持批量操作。

但它仍未解决：

- 仍要手写 SQL；
- 表名字段名和代码耦合；
- 仍要维护 RowMapper；
- 不会自动处理一对多、多对多、懒加载、级联、脏检查。

这些是 JPA/ORM 进一步抽象的方向。

**答辩模板：** JdbcTemplate 封装了原生 JDBC 的样板代码，自动管理资源关闭，统一异常转换，并通过 query、update、batchUpdate 等方法简化数据库操作，RowMapper 也让结果集映射更方便。但它仍然需要开发者手写 SQL 和字段映射，和表结构耦合，也不自动处理复杂对象关系，这些需要 ORM 框架进一步解决。

---

### Q21：RowMapper 本质是什么？跟 ORM 的关系？

**核心结论：** RowMapper 是“数据库一行记录到 Java 对象”的手写映射函数；ORM 是把这种映射自动化、系统化。

**详细解释：**

RowMapper 接口核心方法：

```java
T mapRow(ResultSet rs, int rowNum)
```

它告诉程序：

```text
id 列 → user.id
name 列 → user.name
age 列 → user.age
```

例如：

```java
RowMapper<User> mapper = (rs, rowNum) -> {
    User u = new User();
    u.setId(rs.getLong("id"));
    u.setName(rs.getString("name"));
    return u;
};
```

ORM 则通过注解声明映射：

```java
@Entity
@Table(name = "user")
class User {
    @Id
    private Long id;

    @Column(name = "name")
    private String name;
}
```

框架读取这些元数据后，自动完成对象和表之间的转换。

RowMapper 只解决“查询结果变对象”，ORM 还解决对象保存、关联关系、懒加载、级联、一级缓存、脏检查等问题。

**答辩模板：** RowMapper 本质上是手写的行到对象映射函数。每读取 ResultSet 的一行，就把列值设置到对象字段。ORM 可以看作对这种映射的自动化，通过 `@Entity`、`@Column`、`@OneToMany` 等注解声明表、字段和关系映射，由框架自动完成对象和关系数据之间的转换。

---

### Q22：为什么聚合关系 Aggregate 不给子实体单独 Repository？

**核心结论：** 聚合根负责维护聚合内部一致性，子实体应通过聚合根操作，避免外部绕过业务规则直接修改子实体。

**详细解释：**

以订单为例：

```text
Order 聚合根
  └── OrderItem 子实体
```

`OrderItem` 脱离 `Order` 通常没有独立业务意义。如果给 `OrderItem` 单独 Repository，外部就可能直接新增、删除、修改订单项，从而绕过订单状态、总金额、库存规则等业务约束。

错误风险：

- 订单已支付，还能直接改订单项；
- 删除订单项后订单总金额没更新；
- 创建了没有订单的订单项；
- 子实体和聚合根状态不一致。

正确方式是通过聚合根方法维护规则：

```java
order.addItem(product, quantity);
order.removeItem(itemId);
orderRepository.save(order);
```

JPA 中可以配合级联和 orphanRemoval，让保存聚合根时统一管理子实体。

**答辩模板：** 聚合关系中一般只给聚合根建 Repository，不给子实体单独建 Repository。因为子实体脱离聚合根没有完整业务意义，外部直接 CRUD 子实体会绕过聚合根的业务规则，破坏一致性。比如订单项应该通过订单来新增、删除和修改，由订单维护总金额、状态和明细一致性，最后通过 OrderRepository 保存整个聚合。

---

## 五、JPA / ORM

### Q23：O/R 阻抗失配的本质是什么？举三个具体表现。

**核心结论：** O/R 阻抗失配是对象模型和关系模型表达方式不一致导致的天然矛盾。

**详细解释：**

Java 使用对象模型：对象、引用、集合、继承、方法、对象身份。数据库使用关系模型：表、行、列、主键、外键、关系运算。两者不是同一种思想。

具体表现：

1. **关联关系不同**：对象里可以直接引用另一个对象，数据库只能用外键保存 id。
2. **多对多表达不同**：Java 里可以是 `Set<Course>`，数据库必须用中间表 `student_course`。
3. **继承不同**：Java 天然支持继承，关系库没有直接继承概念，JPA 需要选择单表、JOINED、多表等映射策略。
4. **身份不同**：Java 对象有内存地址身份，数据库记录用主键标识。同一行记录如果被加载成两个对象，就可能产生状态冲突。
5. **访问方式不同**：对象通过导航访问 `order.getUser()`，数据库需要 JOIN 查询。

**答辩模板：** O/R 阻抗失配的本质是对象模型和关系模型不一致。对象模型有引用、集合、继承和对象身份，关系模型是表、行、列、主键和外键。比如对象多对多在数据库里必须用中间表；Java 继承在关系库中需要额外映射策略；对象按引用识别，数据库按主键识别。这些差异就是 ORM 要处理但不能完全消除的问题。

---

### Q24：IDENTITY 与 SEQUENCE 在并发插入时各有何特征？

**核心结论：** IDENTITY 依赖数据库插入后生成主键，不利于批量插入；SEQUENCE 可以提前取号，更适合批处理和高并发插入。

**详细解释：**

`IDENTITY` 常见于 MySQL 的 `auto_increment`。应用执行 insert 后，数据库生成 id，再返回给应用。因此 ORM 必须插入一条拿一条 id，批量插入优化受限制。

`SEQUENCE` 常见于 PostgreSQL、Oracle 等。应用可以先从序列中获取一个或一批 id，再在内存中给实体分配 id，然后批量 insert。

对比：

| 策略 | 特点 | 优点 | 缺点 |
|---|---|---|---|
| IDENTITY | 插入后由数据库生成 id | 简单，MySQL 常用 | 不利于 batch insert |
| SEQUENCE | 插入前先获取 id | 可预取，批量插入效率高 | id 可能不连续 |

ID 不连续通常不是问题，因为主键要求唯一、稳定，不要求连续。

**答辩模板：** IDENTITY 通常依赖数据库自增列，必须执行插入后才能拿到主键，所以 ORM 很难做批量插入。SEQUENCE 是数据库序列，应用可以提前获取一批 ID，在内存中分配给多个实体后批量插入，因此并发和批处理性能更好。SEQUENCE 可能出现 ID 不连续，但主键本来就只要求唯一，不要求连续。

---

### Q25：什么是 N+1 查询问题？怎么避免？

**核心结论：** N+1 是先查 1 次父对象列表，再对 N 个父对象分别查询关联对象，总共 N+1 次 SQL。可以用 JOIN FETCH、EntityGraph、批量抓取或 DTO 查询避免。

**详细解释：**

例如：

```java
List<Order> orders = orderRepository.findAll();
for (Order order : orders) {
    order.getItems().size();
}
```

可能产生：

```sql
select * from orders;
select * from order_item where order_id = 1;
select * from order_item where order_id = 2;
select * from order_item where order_id = 3;
...
```

如果有 100 个订单，就执行 101 次 SQL。单条 SQL 可能不慢，但大量数据库往返会显著拖慢接口，并占用连接池。

**解决方式：**

1. `JOIN FETCH`：一次查询拉取关联数据。
2. `@EntityGraph`：声明本次查询需要加载哪些关联。
3. Batch Size：让 Hibernate 批量加载关联，而不是一个个查。
4. DTO 查询：页面需要什么字段就直接查什么字段，避免加载整个对象图。

**注意：** 不要盲目 JOIN FETCH 大集合，否则可能造成结果集膨胀和分页问题。

**答辩模板：** N+1 查询是 ORM 中常见性能问题，指先查询一次父对象列表，然后遍历 N 个父对象时，每个对象的懒加载关联又各查一次，总共 N+1 次 SQL。解决方式包括 `JOIN FETCH`、`@EntityGraph`、批量抓取或直接使用 DTO 查询。实践中一般默认 LAZY，需要时显式预加载，避免无意识触发大量 SQL。

---

### Q26：mappedBy 在双向关联中起什么作用？

**核心结论：** `mappedBy` 用来声明当前这一侧不是关联拥有方，真正维护外键的是对方字段。

**详细解释：**

一对多关系中，数据库外键通常在“多”的一方。例如：

```text
order_item(order_id)
```

JPA 中：

```java
class Order {
    @OneToMany(mappedBy = "order")
    private List<OrderItem> items;
}

class OrderItem {
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
}
```

`mappedBy = "order"` 的意思是：`Order.items` 不是关系拥有方，外键由 `OrderItem.order` 这个字段维护。

如果不写 `mappedBy`，JPA 可能认为这是另一段独立关系，创建中间表或重复维护外键。

**常见坑：** 双向关联要维护对象两边的一致性：

```java
public void addItem(OrderItem item) {
    items.add(item);
    item.setOrder(this);
}
```

真正影响数据库外键的是拥有方，也就是 `OrderItem.order`。

**答辩模板：** `mappedBy` 用在双向关联的非拥有方，表示这个关联由对方字段维护。在订单和订单项中，外键 `order_id` 在订单项表，所以 `OrderItem.order` 是拥有方，`Order.items` 写 `mappedBy = "order"` 表示自己只是反向映射。如果不写，JPA 可能创建额外中间表或重复外键。

---

## 六、安全与架构

### Q27：密码为什么必须用 bcrypt 而不是 SHA256？

**核心结论：** SHA256 太快，不适合密码存储；bcrypt 是专门用于密码哈希的慢算法，自带 salt，能显著提高暴力破解成本。

**详细解释：**

密码不能明文保存，也不应该“加密后保存再解密”。正确方式是保存不可逆哈希。用户登录时，把输入密码用同样算法处理后与数据库中的哈希比较。

SHA256 是通用哈希算法，设计目标是快。快对文件校验很好，但对密码存储很危险。数据库泄露后，攻击者可以用 GPU 每秒尝试大量密码，即使加盐，也挡不住高速暴力破解。

bcrypt 的特点：

- 自动包含 salt；
- 故意计算慢；
- 可通过 cost 参数调整耗时；
- 相同密码每次生成结果也不同；
- 正常用户登录只慢一点，但攻击者批量爆破成本极高。

**答辩模板：** 密码不能用普通 SHA256 存储，因为 SHA256 太快，数据库泄露后攻击者可以用 GPU 快速暴力破解。bcrypt 是专门为密码存储设计的慢哈希算法，自带 salt，并且可以调整计算成本。这样正常用户登录影响很小，但攻击者大规模尝试密码的成本会大幅提高，所以密码应该用 bcrypt、Argon2 等慢哈希算法。

---

### Q28：Docker 容器与虚拟机的核心区别是什么？

**核心结论：** 虚拟机虚拟化整套硬件和完整操作系统；Docker 容器是共享宿主机内核的隔离进程，更轻量、启动更快，但隔离强度一般弱于虚拟机。

**详细解释：**

虚拟机通过 Hypervisor 模拟硬件，每个 VM 都有完整 Guest OS：

```text
应用 → 依赖库 → Guest OS → 虚拟硬件 → Hypervisor → 宿主机
```

Docker 容器不包含完整操作系统内核，而是在宿主机内核上运行隔离进程，主要依赖：

- namespace：隔离进程、网络、文件系统等视图；
- cgroups：限制 CPU、内存、IO 等资源；
- UnionFS：镜像分层。

对比：

| 对比 | 虚拟机 | Docker 容器 |
|---|---|---|
| 虚拟化层 | Hypervisor + Guest OS | 共享宿主机内核 |
| 启动速度 | 慢，秒级或分钟级 | 快，毫秒到秒级 |
| 镜像大小 | 通常 GB 级 | 通常 MB 到几百 MB |
| 资源开销 | 大 | 小 |
| 隔离强度 | 更强 | 相对弱 |
| 适合场景 | 强隔离、多系统内核 | 微服务、快速部署、弹性扩缩容 |

**答辩模板：** 虚拟机是通过 Hypervisor 模拟硬件，每个虚拟机都有完整操作系统，所以隔离强但资源开销大。Docker 容器本质是宿主机上的隔离进程，共享宿主机内核，通过 namespace 做环境隔离，通过 cgroups 做资源限制，因此更轻量、启动更快、镜像更小，适合微服务和快速部署，但隔离性通常不如虚拟机。

---

## 七、关键对比速记（扩展版）

### 1. `@ManyToOne` vs `@OneToMany` 默认 fetch

- `@ManyToOne` 默认 `EAGER`：因为多对一通常只关联一个对象。
- `@OneToMany` 默认 `LAZY`：因为一对多可能是很大的集合，立即加载风险高。

实践中，很多项目会把 `@ManyToOne` 也显式设置为 `LAZY`，避免列表查询时加载过多关联对象。

### 2. JPQL vs Native SQL

- JPQL 面向实体和属性，可移植性好。
- Native SQL 面向具体数据库表和字段，能使用数据库特有能力，但可移植性差。

常规业务查询优先 JPQL 或 Repository 方法；复杂报表、特殊函数、性能调优时可以使用 Native SQL。

### 3. 行存 vs 列存

- 行存适合 OLTP：一次读写整行，例如订单创建、用户修改。
- 列存适合 OLAP：只扫描少数列做聚合统计，例如销售额汇总、数据分析。

### 4. Maven vs Gradle

- Maven 约定强、XML 配置、生态成熟，适合标准企业项目。
- Gradle 灵活、DSL 强、构建性能好，Android 和复杂构建中常见。

### 5. 积极加载 vs 懒加载

- EAGER：查询主对象时立即加载关联对象，简单但可能性能差。
- LAZY：访问关联对象时才加载，节省资源但可能触发 N+1 或懒加载异常。

推荐：默认 LAZY，需要时用 `JOIN FETCH`、`EntityGraph` 或 DTO 查询明确加载。

---

## 八、答辩复习建议

答辩时不要只回答“是什么”，最好按下面顺序组织：

```text
1. 它是什么
2. 为什么需要它
3. 它怎么解决问题
4. 项目里不用它会有什么坑
```

例如 PreparedStatement：

```text
它是预编译 SQL 对象；
它解决 SQL 注入；
它通过 SQL 模板和参数分离实现；
如果不用它，登录、搜索等用户输入场景可能被注入攻击。
```

最容易被追问的高频点：

1. JIT 为什么能提高性能；
2. 双亲委派如何保证安全；
3. Tomcat 为什么打破双亲委派；
4. equals 和 hashCode 的契约；
5. DTO 为什么不能省；
6. 事务为什么放 Service；
7. PreparedStatement 防注入原理；
8. B+ 树为什么适合索引；
9. 二级索引和回表；
10. N+1 查询如何避免；
11. mappedBy 和关联拥有方；
12. Docker 和虚拟机区别。

> 复习重点：把每个技术点放到“安全、性能、一致性、解耦、可维护性”这几个角度里理解，比死记定义更适合答辩。
