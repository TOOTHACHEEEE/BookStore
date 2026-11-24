# 📚 Book Store - 电子书店项目

> 一个基于 React + Vite + Ant Design 的完整电子书店前端项目，适合作为 React 入门学习项目。

---

## 📋 项目概述

这是一个功能完整的单页应用（SPA），包含：
- 书籍浏览与搜索
- 购物车管理
- 订单结算
- 用户登录

**技术栈：**
- **React 18** - 核心框架
- **Vite** - 构建工具
- **React Router** - 路由管理
- **Ant Design (antd)** - UI 组件库
- **Context API** - 状态管理

---

## 🗂️ 项目结构

```
bookstore-me1/
├── index.html              # 应用入口 HTML
├── package.json            # 项目依赖配置
├── vite.config.js          # Vite 构建配置
├── README.md               # 本文件
└── src/
    ├── main.jsx            # 应用入口，渲染根组件
    ├── App.jsx             # 根组件，配置路由和全局 Provider
    ├── style.css           # 全局样式
    ├── data/
    │   └── Data.json       # 模拟数据（书籍、用户信息）
    ├── utils/
    │   └── price.js        # 价格解析工具函数
    ├── context/
    │   ├── CartContext.jsx # 购物车、订单、登录状态管理
    │   └── SearchContext.jsx # 搜索关键词状态管理
    ├── components/
    │   ├── Layout.jsx      # 页面布局（侧边栏+头部+内容区）
    │   └── BookCard.jsx    # 书籍卡片组件
    └── pages/
        ├── BookList.jsx    # 书籍列表页（首页）
        ├── BookDetail.jsx  # 书籍详情页
        ├── Cart.jsx        # 购物车页
        ├── Orders.jsx      # 订单页
        ├── Profile.jsx     # 个人信息页
        └── Login.jsx       # 登录页
```

---

## 🎯 核心概念详解

### 1. 组件划分思想

项目将 UI 划分为**页面组件**和**通用组件**：

| 类型 | 文件 | 职责 |
|------|------|------|
| **页面组件** | `pages/*.jsx` | 对应一个路由，负责页面级业务逻辑 |
| **通用组件** | `components/*.jsx` | 可复用的 UI 单元，如 BookCard |

**示例：**
```jsx
// pages/BookList.jsx - 页面组件
// 负责：数据获取（从 Data.json）、布局、与 Context 交互
function BookList() {
  const { addToCart } = useCart()
  return <BookCard book={book} onAddToCart={addToCart} />
}

// components/BookCard.jsx - 通用组件
// 负责：纯展示，通过 props 接收数据和回调
function BookCard({ book, onAddToCart }) {
  return <Card>...</Card>
}
```

### 2. Context 状态管理

**为什么使用 Context？**

React 中数据通常通过 props 自上而下传递。当多个组件需要共享同一状态时（如购物车数量在 Header 和 Cart 页面都要显示），逐层传递 props 会变得繁琐（prop drilling）。

**本项目使用了两个 Context：**

#### CartContext - 购物车与登录状态
```jsx
// 提供的数据
const value = {
  cart,           // 购物车数组
  orders,         // 订单数组
  isLoggedIn,     // 登录状态
  user,           // 用户信息
  addToCart,      // 添加到购物车
  removeFromCart, // 从购物车移除
  checkout,       // 结算
  buyNow,         // 立即购买
  login,          // 登录
  logout,         // 登出
}
```

**使用方式：**
```jsx
import { useCart } from '../context/CartContext'

function MyComponent() {
  const { cart, addToCart } = useCart()
  // 直接使用，无需通过 props 传递
}
```

#### SearchContext - 搜索关键词
```jsx
// 提供的数据
const value = {
  searchKeyword,    // 当前搜索词
  setSearchKeyword, // 修改搜索词
}
```

**为什么分开两个 Context？**

- **关注点分离**：购物车状态和搜索状态没有直接关系
- **性能优化**：避免不相关的状态变化导致不必要重渲染

### 3. 路由设计(Router)

```jsx
// App.jsx 中的路由配置
<Router>
  <Routes>
    {/* 带布局的页面 */}
    <Route path="/" element={<AppLayout />}>
      <Route index element={<BookList />} />
      <Route path="book/:id" element={<BookDetail />} />
      <Route path="cart" element={<Cart />} />
      <Route path="orders" element={<Orders />} />
      <Route path="profile" element={<Profile />} />
    </Route>
    {/* 独立页面（无侧边栏） */}
    <Route path="/login" element={<Login />} />
  </Routes>
</Router>
```

**关键概念：**
- **嵌套路由**：`/` 下的所有页面共享 `AppLayout` 布局
- **Outlet**：`Layout.jsx` 中使用 `<Outlet />` 渲染子路由内容
- **动态路由**：`/book/:id` 中的 `:id` 是参数，可通过 `useParams()` 获取

### 4. 数据流图解

```
┌─────────────────────────────────────────────────────────────┐
│                        顶层 Provider                         │
│  ┌─────────────────────┐    ┌─────────────────────┐        │
│  │   CartProvider      │    │  SearchProvider     │        │
│  │  (cart, orders,     │    │  (searchKeyword)    │        │
│  │   user, isLoggedIn) │    │                     │        │
│  └──────────┬──────────┘    └──────────┬──────────┘        │
└─────────────┼──────────────────────────┼───────────────────┘
              │                          │
     ┌────────▼────────┐        ┌───────▼────────┐
     │  Layout.jsx     │        │  BookList.jsx  │
     │  - 显示购物车数  │        │  - 使用搜索词   │
     │  - 显示用户信息  │        │    过滤书籍     │
     └────────┬────────┘        └────────────────┘
              │
     ┌────────▼────────┐
     │  各页面组件      │
     │  - BookDetail   │
     │  - Cart         │
     │  - Orders       │
     └─────────────────┘
```

### 5. 工具函数设计

```js
// utils/price.js
// 将价格字符串（如 "$49.90"）转为数字
export function parsePrice(priceStr) {
  return parseFloat(priceStr.replace('$', ''))
}

// 计算购物车总价
export function calculateCartTotal(cartItems) {
  return cartItems.reduce((sum, item) => {
    return sum + parsePrice(item.price) * item.quantity
  }, 0)
}
```

**为什么提取工具函数？**

遵循 **DRY 原则**（Don't Repeat Yourself），避免代码重复：
- 修改价格格式时只需改一处
- 代码更易测试和维护

---

## 📱 页面功能说明

### 1. 首页（BookList）
- **路径**：`/`
- **功能**：
  - 轮播图展示
  - 书籍卡片网格布局
  - 实时搜索过滤（支持书名、作者）

### 2. 书籍详情（BookDetail）
- **路径**：`/book/:id`
- **功能**：
  - 展示书籍详细信息
  - "加入购物车" 和 "立即购买" 操作

### 3. 购物车（Cart）
- **路径**：`/cart`
- **功能**：
  - 展示已选商品列表
  - 删除商品
  - 计算总价并结算

### 4. 订单（Orders）
- **路径**：`/orders`
- **功能**：
  - 展示历史订单
  - 订单详情（商品、总价、时间）

### 5. 个人信息（Profile）
- **路径**：`/profile`
- **功能**：
  - 展示用户头像和表单
  - 模拟保存个人信息

### 6. 登录（Login）
- **路径**：`/login`
- **功能**：
  - 独立页面（无侧边栏）
  - 表单验证
  - 登录后跳转首页

---

## 🔧 关键技术点

### 1. useEffect 使用场景

```jsx
// 场景1：页面加载时从 localStorage 恢复数据
useEffect(() => {
  const savedCart = localStorage.getItem('cart')
  if (savedCart) setCart(JSON.parse(savedCart))
}, []) // 空数组表示只在挂载时执行

// 场景2：状态变化时自动保存到 localStorage
useEffect(() => {
  localStorage.setItem('cart', JSON.stringify(cart))
}, [cart]) // cart 变化时执行
```

### 2. 受控组件

```jsx
// 搜索框是典型受控组件
const [searchKeyword, setSearchKeyword] = useState('')

<Input.Search
  value={searchKeyword}           // 值由 React 状态控制
  onChange={(e) => setSearchKeyword(e.target.value)} // 输入时更新状态
/>
```

### 3. 列表渲染与 Key

```jsx
{data.books.map(book => (
  <Col key={book.id}>  {/* key 帮助 React 识别元素 */}
    <BookCard book={book} />
  </Col>
))}
```

**为什么需要 key？**
- 帮助 React 确定哪些元素变化了
- 避免不必要的重新渲染
- 必须使用唯一标识（不要用 index）

---

## 🚀 如何运行

```bash
# 安装依赖
npm install

# 开发模式
npm run dev
# 访问 http://localhost:3000

# 构建生产版本
npm run build
```

---

## 🎓 学习建议

### Code Review 准备重点

1. **能解释每个文件的作用** - 先看整体结构，再看细节
2. **理解数据流向** - 从 Data.json 到页面显示的路径
3. **掌握 Context 使用** - 什么时候用，怎么用
4. **熟悉 React Router** - 嵌套路由、useParams、useNavigate
5. **了解性能优化** - useEffect 依赖数组、key 的作用

### 常见问题准备

**Q: 为什么用 Context 而不是 Redux？**
> A: 项目规模小，Context 足够且学习成本低。

**Q: 登录状态保存在哪里？**
> A: CartContext 中管理，同时持久化到 localStorage。

**Q: 搜索功能是如何实现的？**
> A: SearchContext 保存关键词，BookList 中 filter 过滤数据。

**Q: 为什么 Login 页面没有侧边栏？**
> A: 它在路由配置中独立于 Layout，不嵌套在 `/` 路由下。

---

## 📚 延伸阅读

- [React 官方文档](https://zh-hans.react.dev/)
- [React Router 文档](https://reactrouter.com/)
- [Ant Design 组件库](https://ant.design/)
- [Vite 配置指南](https://cn.vitejs.dev/)

---

**祝 Code Review 顺利！** 🎉
