# 项目相关知识点速查 —— 抽象问题应对版

> 以下问题均可能在你明天的 CR 中被问到，每个答案都结合了你项目中的实际代码，方便你记忆和引用。

---

## 1. 虚拟 DOM vs 真实 DOM

**Q：什么是虚拟 DOM？和你的项目有什么关系？**

**A：**
- **真实 DOM**：浏览器中的 DOM 树，操作它非常昂贵（会引发重排重绘）
- **虚拟 DOM**：React 在内存中维护的一个轻量级 JavaScript 对象树，是对真实 DOM 的描述

**工作原理**：
1. 组件状态变化时，React 先生成新的虚拟 DOM 树
2. 用 Diff 算法比较新旧虚拟 DOM 树的差异
3. 只把变化的部分批量更新到真实 DOM，而不是整个页面重绘

**结合项目**：
比如你在首页搜索书籍时，输入关键词后 `filteredBooks` 数组变化，React 会计算哪些 `BookCard` 需要显示/隐藏，只更新这些卡片的 DOM，而不是重新渲染整个页面。

```jsx
// 你的 BookList.jsx 中
const filteredBooks = data.books.filter(book => { ... })
// 这里 filteredBooks 变化，React 会精准更新书籍列表的 DOM
```

---

## 2. useState / useRef / useEffect 用法和原理

### useState

**Q：你的项目里哪里用了 useState？它的原理是什么？**

**A：**
- `useState` 用于在函数组件中保存状态，状态变化会触发组件重新渲染
- **原理**：React 内部用闭包和链表来管理状态，每次渲染时按顺序取出对应的状态值

**结合项目**：
```jsx
// CartContext.jsx
const [cart, setCart] = useState([])
const [isLoggedIn, setIsLoggedIn] = useState(false)
```
点击"加入购物车"时调用 `setCart(newCart)`，React 检测到状态变化，会重新渲染消费了这个 Context 的组件（如 `Layout` 上的购物车徽章数、`Cart` 页面）。

**注意**：`useState` 的更新是异步的，如果在同一个事件里连续调用多次 `setState`，React 18 会自动批处理合并为一次重渲染。

---

### useRef

**Q：useRef 和 useState 有什么区别？你的项目里可以用在哪里？**

**A：**
| | useState | useRef |
|---|---|---|
| 用途 | 保存会触发渲染的数据 | 保存不触发渲染的数据 |
| 返回值 | [value, setter] | { current: value } |
| 典型场景 | 用户输入、列表数据 | DOM 引用、定时器 ID、上一次的状态值 |

**结合项目**：
你项目里没直接用 `useRef`，但如果要给搜索框加"自动聚焦"功能，可以这样用：
```jsx
const inputRef = useRef(null)
useEffect(() => {
  inputRef.current?.focus()
}, [])
// 然后在 <Input ref={inputRef} />
```

另一个场景是保存上一次的搜索关键词用于对比：
```jsx
const prevKeyword = useRef('')
useEffect(() => {
  if (prevKeyword.current !== searchKeyword) {
    // 搜索词真正变化了才发请求
    prevKeyword.current = searchKeyword
  }
}, [searchKeyword])
```

---

### useEffect

**Q：你的项目里哪里用了 useEffect？依赖数组的作用是什么？**

**A：**
`useEffect` 用于处理副作用（side effects），比如数据获取、订阅、手动修改 DOM、localStorage 同步等。

**结合项目 —— 三个经典用法**：

```jsx
// 1. 组件挂载时从 localStorage 恢复数据（只执行一次）
useEffect(() => {
  const savedCart = localStorage.getItem('cart')
  if (savedCart) setCart(JSON.parse(savedCart))
}, [])  // 空数组 = 只在挂载时执行

// 2. cart 变化时自动保存到 localStorage
useEffect(() => {
  localStorage.setItem('cart', JSON.stringify(cart))
}, [cart])  // 依赖 cart，cart 变了就执行

// 3. 登录状态变化时保存
useEffect(() => {
  localStorage.setItem('isLoggedIn', isLoggedIn)
}, [isLoggedIn, user])  // 多个依赖，任一变化都执行
```

**依赖数组规则**：
- `[]`：只在组件挂载和卸载时执行
- `[a, b]`：挂载时执行 + 依赖变化时执行
- 不写依赖数组：每次渲染后都执行（容易死循环，要谨慎）

**清理函数**：如果 Effect 里创建了订阅或定时器，返回一个清理函数：
```jsx
useEffect(() => {
  const timer = setTimeout(() => {...}, 1000)
  return () => clearTimeout(timer)  // 组件卸载或依赖变化前清理
}, [])
```

---

## 3. React Strict Mode 的作用

**Q：Strict Mode 是干什么的？为什么要用它？**

**A：**
`React.StrictMode` 是 React 提供的开发模式检查工具，不会在生产环境产生影响。

**主要作用**：
1. **检测副作用**：故意双调用某些函数（如 render、状态更新函数），帮助发现不纯的副作用
2. **检测过时的 API**：提示使用废弃的生命周期或 API
3. **检测意外的副作用**：比如 `useEffect` 忘记写清理函数导致内存泄漏

**结合项目**：
你的 `main.jsx` 里应该包裹了 `StrictMode`：
```jsx
<StrictMode>
  <App />
</StrictMode>
```
这会导致开发环境下某些 Effect 执行两次，帮你发现比如重复订阅、重复添加事件监听器等问题。如果上线后发现生产环境行为不同，很可能是 Strict Mode 帮你暴露出了副作用问题。

---

## 4. npm run dev vs npm run build，HTML 不一样为什么效果一样

**Q：dev 和 build 生成的 HTML 不一样，为什么效果一样？**

**A：**

| | `npm run dev`（开发） | `npm run build`（生产） |
|---|---|---|
| 工具 | Vite 开发服务器 | Vite 打包构建 |
| HTML | 简单 HTML，JS/CSS 通过 `<script type="module">` 动态注入 | 最终 HTML，引用了带有 hash 的打包后的 JS/CSS 文件 |
| JS 处理 | 原生 ESM，按需编译，不做合并 | Tree Shaking、代码分割、压缩、合并 |
| 速度 | 冷启动快（利用浏览器原生 ESM） | 构建慢但输出最优 |
| 用途 | 开发调试 | 部署上线 |

**为什么效果一样**：
因为 React 应用的本质是"一个挂载到 DOM 节点的 JS 应用"。无论 HTML 怎么变，最终都是：
1. 浏览器加载 HTML
2. HTML 加载 JS 入口文件
3. JS 执行 `ReactDOM.createRoot(document.getElementById('root')).render(<App />)`
4. React 接管页面，动态生成所有内容

所以你的 `index.html` 里可能只有一个空的 `<div id="root"></div>`，真正的页面结构全部由 React 组件（`App.jsx` → `Layout.jsx` → 各页面）渲染出来。

---

## 5. Context API 的作用和用法

**Q：你为什么用 Context？props 传值不行吗？**

**A：**
- **Props 传值**：适合父子层级浅的场景，叫 props drilling
- **Context**：适合跨多层组件共享状态，避免中间层组件被迫传递不用的 props

**结合项目**：
你的搜索框在 `Layout` 组件（顶层布局），书籍列表在 `BookList` 组件（子路由页面）。如果用 props，需要：`App → Layout → Outlet → BookList`，中间 `Outlet` 被迫传一个它不关心的 `searchKeyword`。用 `SearchContext` 后，`Layout` 和 `BookList` 直接订阅同一个 Context，中间组件完全无感知。

```jsx
// SearchContext.jsx
const SearchContext = createContext()

// Layout.jsx 中修改
const { setSearchKeyword } = useSearch()

// BookList.jsx 中读取
const { searchKeyword } = useSearch()
```

**注意**：Context 不是状态管理库，它只解决"跨层级传值"问题。如果状态逻辑复杂，还是需要配合 `useReducer` 或引入 Redux/Zustand。

---

## 6. React Router 核心概念

**Q： Outlet 是什么？useNavigate 和 Link 有什么区别？**

**A：**

### Outlet
`Outlet` 是子路由的渲染占位符。父路由组件包裹 `Outlet`，子路由组件会在那个位置渲染。

```jsx
// App.jsx
<Route path="/" element={<AppLayout />}>
  <Route index element={<BookList />} />
  <Route path="book/:id" element={<BookDetail />} />
</Route>

// Layout.jsx
<Content>
  <Outlet />  {/* BookList 或 BookDetail 会渲染在这里 */}
</Content>
```

这样所有子页面共享同一个 `Layout`（侧边栏 + Header），只有中间内容区变化。

### useNavigate vs Link
| | Link | useNavigate |
|---|---|---|
| 用法 | JSX 组件 `<Link to="/cart">` | Hook `const navigate = useNavigate(); navigate('/cart')` |
| 场景 | 用户点击跳转 | 代码逻辑中跳转（如表单提交后、购买成功后） |
| 示例 | 菜单导航 | 登录成功后 `navigate('/')`、购买成功后 `navigate('/orders')` |

---

## 7. JSX 的本质是什么

**Q：JSX 是 HTML 吗？浏览器能直接运行吗？**

**A：**
- **JSX 不是 HTML**，它是 JavaScript 的语法扩展（ syntactic sugar）
- 浏览器不能直接运行 JSX，需要 Babel/Vite 在构建时把 JSX 编译成 `React.createElement()` 调用

```jsx
// 你写的 JSX
<h1 className="title">Hello</h1>

// 编译后等价于
React.createElement('h1', { className: 'title' }, 'Hello')
```

最终生成的是一个 JavaScript 对象（React Element），描述了这个节点长什么样。React 再把这个 Element 转换成虚拟 DOM，进而更新真实 DOM。

**为什么用 JSX**：
- 声明式：描述"UI 长什么样"而不是"怎么操作 DOM"
- 可以在 UI 中嵌入任意 JS 表达式（`{cart.length}`、`{book.price}`）

---

## 8. 受控组件 vs 非受控组件

**Q：你的项目里表单是受控的还是非受控的？**

**A：**

| | 受控组件 | 非受控组件 |
|---|---|---|
| 数据存储 | React state | DOM 节点 |
| 获取值 | 从 state 读取 | 从 ref 读取 |
| 典型代码 | `value={state}` + `onChange={setState}` | `defaultValue` + `ref` |

**结合项目**：
你的搜索框是典型的受控组件：
```jsx
<Input.Search
  value={searchKeyword}           // 值由 React 状态控制
  onChange={(e) => setSearchKeyword(e.target.value)}  // 变化同步到状态
/>
```

登录表单用了 Ant Design 的 Form，内部也是受控模式，通过 `form.getFieldsValue()` 获取值。

**受控的好处**：
- 可以实时校验（如邮箱格式）
- 可以联动（如搜索时自动跳转到首页）
- 状态可预测，方便调试

---

## 9. React 中 key 的作用

**Q：为什么你的 map 循环里要加 key？可以用 index 吗？**

**A：**
`key` 是 React Diff 算法的"身份证"，帮助 React 识别哪些元素变了、增了、删了。

**结合项目**：
```jsx
// BookList.jsx
{filteredBooks.map(book => (
  <Col key={book.id} ...>   {/* 用书籍唯一 ID 作为 key */}
    <BookCard book={book} />
  </Col>
))}
```

**为什么不用 index**：
如果用数组 index 作为 key，当列表顺序变化（比如删除第一本），React 会认为只是内容变了而不是顺序变了，可能导致状态错乱（比如输入框内容跟着错位）。

**最佳实践**：用数据的唯一标识（如 `book.id`、`order.id`）作为 key。

---

## 10. Vite 是什么？为什么用它而不是 CRA？

**A：**
Vite 是下一代前端构建工具，你的项目用它替代了传统的 Create React App。

**核心优势**：
1. **开发启动快**：利用浏览器原生 ESM，不需要打包整个应用
2. **热更新快**：HMR（Hot Module Replacement）只更新改动的模块
3. **构建快**：用 esbuild/Rollup 做生产打包，比 Webpack 快很多
4. **配置简单**：`vite.config.js` 几行就搞定

**你的项目配置**：
```js
// vite.config.js
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
})
```

---

## 11. useEffect 依赖项为空数组 [] 时，闭包陷阱问题

**Q：你的 useEffect 里读取了 state，但依赖数组是空的，会有问题吗？**

**A：**
如果 Effect 内部用到了某个 state/props，但没有把它加入依赖数组，会拿到"旧值"（闭包陷阱）。

**结合项目 —— 你这里是对的**：
```jsx
// ✅ 正确：内部只用 localStorage，不依赖组件 state
useEffect(() => {
  const savedCart = localStorage.getItem('cart')
  if (savedCart) setCart(JSON.parse(savedCart))
}, [])
```

但如果写成这样就有问题：
```jsx
// ❌ 错误：内部用了 count，但依赖数组没写 count
useEffect(() => {
  console.log(count)  // 永远打印第一次 render 的 count
}, [])
```

**解决方案**：
- ESLint 的 `react-hooks/exhaustive-deps` 规则会提示你补全依赖
- 如果确实不需要响应某个值的变化，先检查逻辑是否正确

---

## 12. React 18 的并发特性（Concurrent Features）

**Q：React 18 有什么新特性？你的项目用到了吗？**

**A：**
React 18 引入了并发渲染，主要特性：

1. **自动批处理（Automatic Batching）**：
   以前只有在 React 事件处理函数中的 setState 才会批处理，现在即使在 `setTimeout`、Promise 中也会自动批处理，减少重渲染次数。
   ```jsx
   // React 18 中这只会触发一次重渲染
   setTimeout(() => {
     setCount(c => c + 1)
     setFlag(f => !f)
   }, 1000)
   ```

2. **Concurrent Rendering**：
   React 可以中断渲染，优先响应用户交互（如输入），后台继续计算复杂更新。

3. **Suspense 改进**：更好地配合数据获取。

**结合项目**：
你的 `main.jsx` 使用了 `createRoot` 而不是 `ReactDOM.render`，这就是 React 18 的入口：
```jsx
// main.jsx
ReactDOM.createRoot(document.getElementById('root')).render(<App />)
```

---

## 13. CSS 在你的项目中是怎么处理的

**Q：你项目里样式怎么写的？行内 style、CSS 文件、CSS-in-JS 的区别？**

**A：**
你的项目主要用了三种方式：

1. **行内 style（最常用）**：
   ```jsx
   <div style={{ padding: '24px', background: '#fff' }}>
   ```
   优点：动态计算方便，作用域天然隔离。缺点：无法写伪类/媒体查询。

2. **Ant Design 组件自带样式**：
   AntD 内部用 CSS-in-JS（Styled Components 类似方案）生成样式，你通过 props 控制（如 `type="primary"`、`size="large"`）。

3. **ConfigProvider 主题定制**：
   ```jsx
   <ConfigProvider theme={{ token: { colorPrimary: '#1890ff' } }}>
   ```
   统一修改 AntD 的主题色、圆角等设计 token。

**为什么没写单独的 .css 文件**：
对于这个规模的项目，行内 style 足够。如果项目变大，可以引入 CSS Modules 或 Tailwind CSS 来更好地管理样式。

---

## 14. localStorage 和 React 状态同步的注意事项

**Q：为什么 localStorage 变化了，React 组件不会自动更新？**

**A：**
`localStorage` 是浏览器 API，和 React 的响应式系统完全独立。直接 `localStorage.setItem()` 不会触发任何组件重渲染。

**正确做法（你项目里就是这样做的）**：
1. **以 React state 为唯一数据源**：用户操作先改 state
2. **用 useEffect 做同步**：state 变化后，Effect 里把值写回 localStorage
3. **初始化时恢复**：另一个 Effect 从 localStorage 读值，设置到 state

```jsx
// 你的 CartContext.jsx 中的标准模式
const [cart, setCart] = useState([])

useEffect(() => {
  // 读：挂载时恢复
  const saved = localStorage.getItem('cart')
  if (saved) setCart(JSON.parse(saved))
}, [])

useEffect(() => {
  // 写：state 变化时保存
  localStorage.setItem('cart', JSON.stringify(cart))
}, [cart])
```

**如果要在多个标签页间同步**：需要监听 `window.addEventListener('storage', ...)` 事件。

---

## 15. 组件的 Props 和 State 有什么区别

**Q：props 和 state 的区别？BookCard 接收的 book 是 props 还是 state？**

**A：**

| | Props | State |
|---|---|---|
| 定义 | 父组件传进来的 | 组件自己管理的 |
| 可修改性 | 只读（不可直接修改） | 可修改（通过 setter） |
| 作用 | 让组件可复用、可配置 | 保存组件内部会变化的数据 |
| 类比 | 函数参数 | 函数内部变量 |

**结合项目**：
```jsx
// BookCard.jsx
function BookCard({ book, onAddToCart }) {  // book 和 onAddToCart 都是 props
```
`book` 是 props（父组件 `BookList` 传进来的数据），`onAddToCart` 也是 props（父组件传进来的回调函数）。

**一个组件可以同时有 props 和 state**：
```jsx
function Example({ title }) {        // title 是 props
  const [count, setCount] = useState(0)  // count 是 state
}
```

---

## 16. 事件处理中的 this 和闭包问题

**Q：为什么你的 onClick 里可以直接写箭头函数？**

**A：**
React 事件处理函数推荐用箭头函数或普通函数，避免 `this` 指向问题。

```jsx
// ✅ 你的写法
<Button onClick={() => onAddToCart(book)}>加入购物车</Button>

// ✅ 或者提前绑定
const handleAdd = () => onAddToCart(book)
<Button onClick={handleAdd}>加入购物车</Button>
```

**为什么不能直接传带参数的函数调用**：
```jsx
// ❌ 错误：组件渲染时就会执行，不是点击时才执行
<Button onClick={onAddToCart(book)} />

// ✅ 正确：传入函数引用或箭头函数
<Button onClick={() => onAddToCart(book)} />
```

---

## 17. React 组件渲染流程

**Q：从输入 URL 到页面显示，React 做了什么？**

**A：**
1. **浏览器加载 HTML** → 执行 `main.jsx`
2. **创建 Root** → `ReactDOM.createRoot(document.getElementById('root'))`
3. **首次渲染** → 执行 `App()` → 执行 `Layout()` → 根据路由匹配子组件（如 `BookList`）
4. **生成虚拟 DOM** → 所有 JSX 编译成 `React.createElement`，组成虚拟 DOM 树
5. **Diff + 提交** → React 比较新旧虚拟 DOM（首次没有旧树，全部创建），生成真实 DOM 并插入页面
6. **执行副作用** → 执行各组件的 `useEffect`
7. **交互更新** → 用户点击"加入购物车" → `setCart()` → React 重新渲染相关组件 → Diff 计算变化 → 更新真实 DOM

---

## 18. 为什么函数组件里不能写 if (条件) { useState(...) }

**Q：Hooks 的调用规则是什么？**

**A：**
React Hooks 有两条铁律：
1. **只在最顶层调用**，不要在循环、条件、嵌套函数里调用
2. **只在 React 函数组件或自定义 Hook 中调用**

**原因**：React 靠 Hooks 调用的顺序来匹配对应的状态。如果第一次渲染调了 3 个 Hook，第二次渲染只调了 2 个（因为条件分支跳过了），React 就会错乱。

```jsx
// ❌ 错误
if (isLoggedIn) {
  const [data, setData] = useState(null)
}

// ✅ 正确
const [data, setData] = useState(null)
// 然后在 JSX 里条件渲染
{isLoggedIn && <div>{data}</div>}
```

---

## 19. React.memo 和 useMemo / useCallback 是做什么的

**Q：你的项目里需要优化渲染性能吗？**

**A：**
- **`React.memo`**：包裹组件，props 没变时不重渲染
- **`useMemo`**：缓存计算结果，依赖没变时不重新计算
- **`useCallback`**：缓存函数引用，避免子组件因收到新函数引用而重渲染

**结合项目**：
你项目规模不大，React 的默认渲染性能已经足够。但如果 `BookList` 里的书籍数量变成上千本，可以考虑：
```jsx
// 缓存过滤结果
const filteredBooks = useMemo(() => {
  return data.books.filter(book => ...)
}, [searchKeyword])

// 缓存回调，避免 BookCard 不必要的重渲染
const handleAddToCart = useCallback((book) => {
  addToCart(book)
}, [addToCart])
```

**不要过度优化**：小项目里滥用这些反而增加代码复杂度，且收益不明显。

---

## 20. npm 包管理相关

**Q：package.json 里的 dependencies 和 devDependencies 有什么区别？**

**A：**

| | dependencies | devDependencies |
|---|---|---|
| 用途 | 项目运行时必须 | 开发时需要，运行不需要 |
| 例子 | react、react-dom、antd | vite、@vitejs/plugin-react |
| 生产构建 | 会被打包进产物 | 不会被打包 |

**结合项目**：
```json
"dependencies": {
  "react": "^18.2.0",         // 运行必须
  "antd": "^5.29.3",          // 运行必须
  "react-router-dom": "^6.20.0"  // 运行必须
},
"devDependencies": {
  "vite": "^5.0.0",           // 构建工具，运行不需要
  "@vitejs/plugin-react": "^4.2.0"
}
```

**为什么作业要求不要提交 node_modules**：
因为 `node_modules` 体积巨大（几百 MB），但可以通过 `package.json` + `npm install` 完全重建。提交压缩包时排除它能显著减小体积。

---

> 最后提醒：CR 时如果被问到没准备到的问题，可以先说"这个问题我在做项目时确实遇到过/思考过"，然后结合你项目里的某个具体场景来讲，即使答案不完美，展示思考过程也能拿到分数。加油！
