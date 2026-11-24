# 前后端通信流程（背诵稿+代码位置）

## 五步流程法

### 第一步：前端发请求
**讲解内容：**
用户点击按钮或进入页面，前端调用封装好的 API 函数，发送 HTTP 请求到后端接口。

**展示代码：**
- `bookstore-frontend/src/api/api.js:42-44` - fetchBooks 函数
- `bookstore-frontend/src/api/api.js:13-38` - request 通用请求函数

**代码片段：**
```javascript
export function fetchBooks() {
  return request('/books')
}

async function request(path, options = {}) {
  const url = `${BASE_URL}${path}`
  const resp = await fetch(url, config)
  const json = await resp.json()
  return json.data
}
```

---

### 第二步：后端接收请求
**讲解内容：**
Spring Boot 的 Controller 层接收请求，不处理业务逻辑，直接调用对应的 Service。

**展示代码：**
- `bookstore-backend/src/main/java/com/bookstore/backend/controller/BookController.java:19-24`

**代码片段：**
```java
@RestController
@RequestMapping("/api/v1")
public class BookController {
    @Autowired
    private BookService bookService;

    @GetMapping("/books")
    public ApiResponse getAllBooks() {
        return bookService.getAllBooks();
    }
}
```

---

### 第三步：访问数据库
**讲解内容：**
Service 层调用 Repository 层，使用 Spring Data JPA 执行 SQL，从 MySQL 读取数据。

**展示代码：**
- `bookstore-backend/src/main/java/com/bookstore/backend/service/BookService.java:26-35`

**代码片段：**
```java
@Service
public class BookService {
    @Autowired
    private BookRepository bookRepository;

    public ApiResponse getAllBooks() {
        List<Book> books = bookRepository.findAll();  // JPA自动生成SQL
        // ...
    }
}
```

---

### 第四步：组装返回数据
**讲解内容：**
Service 层处理业务逻辑，将实体类转换为 View 对象，封装成统一响应格式返回。

**展示代码：**
- `bookstore-backend/src/main/java/com/bookstore/backend/service/BookService.java:26-42`

**代码片段：**
```java
public ApiResponse getAllBooks() {
    List<Book> books = bookRepository.findAll();
    List<BookView> views = new ArrayList<>();
    for (Book b : books) {
        views.add(toView(b));  // 实体转DTO
    }
    return ApiResponse.success(views);  // 封装统一响应
}
```

---

### 第五步：前端渲染展示
**讲解内容：**
前端收到 JSON 响应，解析数据并更新组件状态，React 自动重新渲染页面。

**展示代码：**
- `bookstore-frontend/src/pages/BookList.jsx:20-28`

**代码片段：**
```jsx
const [books, setBooks] = useState([]);

useEffect(() => {
    fetchBooks().then(data => {
        setBooks(data);  // 更新状态
    });
}, []);

// 渲染到页面
{books.map(book => <BookCard key={book.id} book={book} />)}
```

---

## 关键技术点对照

| 环节 | 技术 | 代码位置 |
|------|------|----------|
| 前端请求 | Fetch API | `api.js:13-38` |
| 后端接收 | @RestController | `BookController.java` |
| 业务逻辑 | @Service | `BookService.java` |
| 数据访问 | Spring Data JPA | `BookRepository.java` |
| 数据库 | MySQL | `schema.sql` |
| 响应格式 | JSON + ApiResponse | `ApiResponse.java` |

---

## 一句话总结

**前端发请求 → Controller 接 → Service 处理 → Repository 查库 → 返回 JSON → 前端渲染**

---

## 演示路线图

```
1. 打开 api.js 展示 fetchBooks 和 request 函数
2. 打开 BookController.java 展示 @GetMapping 和 Service 调用
3. 打开 BookService.java 展示 Repository 调用和数据转换
4. 打开 BookRepository.java 展示接口定义
5. 打开 BookList.jsx 展示 useEffect 和状态更新
```
