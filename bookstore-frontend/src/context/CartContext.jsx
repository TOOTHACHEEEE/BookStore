import { createContext, useContext, useState, useEffect, useCallback } from 'react'
import {
  fetchCart,
  addToCart as apiAddToCart,
  removeCartItem as apiRemoveCartItem,
  fetchOrders,
  checkoutOrder,
  buyNowOrder,
  fetchCurrentUser,
} from '../api/api'

/**
 * CartContext - 管理购物车、订单和用户登录状态
 * 
 * 迭代二改造：
 * - 购物车/订单数据通过 fetch 请求后端，不再使用 localStorage
 * - 仅登录状态（用户名/头像）保存在 localStorage，方便刷新后恢复
 * 
 * 【安全改进】
 * - 用户身份由后端Session管理，前端不再传递username
 * - 刷新页面时尝试从后端获取当前用户信息，验证Session是否有效
 */
const CartContext = createContext()

export function CartProvider({ children }) {
  const [cart, setCart] = useState([])
  const [orders, setOrders] = useState([])

  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [user, setUser] = useState(null)

  // 刷新购物车（从后端拉取，无需传username）
  const refreshCart = useCallback(async () => {
    try {
      const data = await fetchCart()
      setCart(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('刷新购物车失败:', err)
      setCart([])
    }
  }, [])

  // 刷新订单（从后端拉取，无需传username）
  const refreshOrders = useCallback(async () => {
    try {
      const data = await fetchOrders()
      setOrders(Array.isArray(data) ? data : [])
    } catch (err) {
      console.error('刷新订单失败:', err)
      setOrders([])
    }
  }, [])

  // 启动时从 localStorage 恢复登录状态，并验证Session是否有效
  useEffect(() => {
    const savedLogin = localStorage.getItem('isLoggedIn')
    const savedUser = localStorage.getItem('user')
    if (savedLogin === 'true' && savedUser) {
      const u = JSON.parse(savedUser)
      // 尝试从后端获取当前用户，验证Session是否有效
      fetchCurrentUser()
        .then(currentUser => {
          setIsLoggedIn(true)
          setUser(u)
        })
        .catch(() => {
          // Session无效，清除登录状态
          console.log('Session已失效，需要重新登录')
          localStorage.removeItem('isLoggedIn')
          localStorage.removeItem('user')
        })
    }
  }, [])

  // 登录状态变化时同步购物车和订单
  useEffect(() => {
    if (isLoggedIn && user?.name) {
      refreshCart()
      refreshOrders()
    } else {
      setCart([])
      setOrders([])
    }
  }, [isLoggedIn, user, refreshCart, refreshOrders])

  // 登录状态变化时同步 localStorage
  useEffect(() => {
    localStorage.setItem('isLoggedIn', isLoggedIn)
    if (user) {
      localStorage.setItem('user', JSON.stringify(user))
    } else {
      localStorage.removeItem('user')
    }
  }, [isLoggedIn, user])

  // 添加到购物车（无需传username，由后端Session获取）
  const addToCart = async (book) => {
    if (!isLoggedIn || !user?.name) {
      throw new Error('请先登录')
    }
    await apiAddToCart(book.id, 1)
    await refreshCart()
  }

  // 从购物车移除（无需传username，由后端Session获取）
  const removeFromCart = async (cartItemId) => {
    await apiRemoveCartItem(cartItemId)
    await refreshCart()
  }

  // 结算（无需传username，由后端Session获取）
  const checkout = async () => {
    if (!isLoggedIn || !user?.name) {
      throw new Error('请先登录')
    }
    if (cart.length === 0) {
      return false
    }
    await checkoutOrder()
    await Promise.all([refreshCart(), refreshOrders()])
    return true
  }

  // 立即购买（无需传username，由后端Session获取）
  const buyNow = async (book) => {
    if (!isLoggedIn || !user?.name) {
      throw new Error('请先登录')
    }
    await buyNowOrder(book.id)
    await refreshOrders()
    return true
  }

  // 登录
  const login = (userInfo) => {
    setIsLoggedIn(true)
    setUser(userInfo)
    // 保存到 localStorage，确保刷新后恢复登录状态
    localStorage.setItem('isLoggedIn', 'true')
    localStorage.setItem('user', JSON.stringify(userInfo))
  }

  // 登出
  const logout = async () => {
    try {
      await fetch('/api/v1/users/logout', { method: 'POST', credentials: 'include' })
    } catch (e) {
      console.error('登出请求失败:', e)
    }
    setIsLoggedIn(false)
    setUser(null)
    setCart([])
    setOrders([])
    localStorage.removeItem('isLoggedIn')
    localStorage.removeItem('user')
  }

  return (
    <CartContext.Provider value={{
      cart,
      orders,
      addToCart,
      removeFromCart,
      checkout,
      buyNow,
      refreshCart,
      refreshOrders,
      isLoggedIn,
      user,
      login,
      logout,
    }}>
      {children}
    </CartContext.Provider>
  )
}

export function useCart() {
  const context = useContext(CartContext)
  if (!context) {
    throw new Error('useCart 必须在 CartProvider 内部使用')
  }
  return context
}