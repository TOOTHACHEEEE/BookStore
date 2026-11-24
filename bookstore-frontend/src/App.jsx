import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { CartProvider } from './context/CartContext'
import { SearchProvider } from './context/SearchContext'
import AppLayout from './components/Layout'
import BookList from './pages/BookList'
import BookDetail from './pages/BookDetail'
import Cart from './pages/Cart'
import Orders from './pages/Orders'
import Profile from './pages/Profile'
import Login from './pages/Login'
import BookManage from './pages/BookManage'
import UserManage from './pages/UserManage'
import Statistics from './pages/Statistics'
import GuideAgent from './pages/GuideAgent'
import StorePolicy from './pages/StorePolicy'

/**
 * App 组件 - 应用根组件
 * 
 * 使用 Provider 模式将全局状态（购物车、搜索）注入到整个应用：
 * - CartProvider: 管理购物车、订单、登录状态
 * - SearchProvider: 管理搜索关键词
 * 
 * Router 配置所有页面路由，Layout 组件作为公共布局包裹所有页面。
 */
function App() {
  return (
    <CartProvider>
      <SearchProvider>
        <Router>
          <Routes>
            <Route path="/" element={<AppLayout />}>
              <Route index element={<BookList />} />
              <Route path="book/:id" element={<BookDetail />} />
              <Route path="cart" element={<Cart />} />
              <Route path="orders" element={<Orders />} />
              <Route path="profile" element={<Profile />} />
              <Route path="statistics" element={<Statistics />} />
              {/* 作业3：智能导购 Agent */}
              <Route path="guide" element={<GuideAgent />} />
              {/* 退换货与会员政策 */}
              <Route path="policy" element={<StorePolicy />} />
              {/* 管理员路由 */}
              <Route path="admin/books" element={<BookManage />} />
              <Route path="admin/users" element={<UserManage />} />
            </Route>
            <Route path="/login" element={<Login />} />
          </Routes>
        </Router>
      </SearchProvider>
    </CartProvider>
  )
}

export default App
