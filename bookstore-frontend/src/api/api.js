/**
 * 前后端通信封装 - 基于 Fetch API
 * 所有请求都通过 Vite 代理转发到 http://localhost:8080
 * 后端响应体形如 { code, message, data }，本文件统一拆包
 * 
 * 【安全改进】
 * 用户身份由后端Session管理，前端不再传递username。
 * 这样可以防止用户伪造其他用户的身份进行操作。
 */

const BASE_URL = '/api/v1'

/**
 * 通用请求函数
 * 自动序列化 body、拼装 JSON 头，并拆掉 { code, message, data } 包装
 */
async function request(path, options = {}) {
  const url = `${BASE_URL}${path}`
  const headers = {
    'Content-Type': 'application/json',
    ...(options.headers || {}),
  }
  const config = {
    method: options.method || 'GET',
    headers,
    credentials: 'include',  // 发送Cookie，让Session正常工作
  }
  if (options.body !== undefined) {
    config.body = typeof options.body === 'string'
      ? options.body
      : JSON.stringify(options.body)
  }

  const resp = await fetch(url, config)
  if (!resp.ok) {
    throw new Error(`HTTP ${resp.status}`)
  }
  const json = await resp.json()
  if (json.code !== 200) {
    throw new Error(json.message || '请求失败')
  }
  return json.data
}

// ============= 书籍 =============

export function fetchBooks() {
  return request('/books')
}

export function fetchBookById(id) {
  return request(`/book/${id}`)
}

// ============= 购物车 =============

// 获取当前用户的购物车（无需传username，由后端Session获取）
export function fetchCart() {
  return request('/cart')
}

// 添加商品到购物车（无需传username，由后端Session获取）
export function addToCart(bookId, quantity = 1) {
  return request('/cart', {
    method: 'POST',
    body: { bookId, quantity },
  })
}

// 从购物车移除商品（无需传username，由后端Session获取）
export function removeCartItem(cartItemId) {
  return request(`/cart/${cartItemId}`, { method: 'DELETE' })
}

// 清空购物车（无需传username，由后端Session获取）
export function clearCart() {
  return request('/cart', {
    method: 'DELETE',
  })
}

// ============= 订单 =============

// 获取当前用户的订单（无需传username，由后端Session获取）
export function fetchOrders() {
  return request('/orders')
}

// 搜索用户订单（时间范围+书籍名称，无需传username，由后端Session获取）
export function searchUserOrders(startTime, endTime, bookTitle) {
  const params = new URLSearchParams()
  if (startTime) params.append('startTime', startTime)
  if (endTime) params.append('endTime', endTime)
  if (bookTitle) params.append('bookTitle', bookTitle)
  return request(`/orders/search?${params.toString()}`)
}

// 获取所有订单（管理员功能）
export function fetchAllOrders() {
  return request('/orders/all')
}

// 搜索所有订单（管理员功能）
export function searchAllOrders(startTime, endTime, bookTitle) {
  const params = new URLSearchParams()
  if (startTime) params.append('startTime', startTime)
  if (endTime) params.append('endTime', endTime)
  if (bookTitle) params.append('bookTitle', bookTitle)
  return request(`/orders/searchAll?${params.toString()}`)
}

// 结算订单（无需传username，由后端Session获取）
export function checkoutOrder() {
  return request('/orders/checkout', {
    method: 'POST',
    body: {},
  })
}

// 立即购买（无需传username，由后端Session获取）
export function buyNowOrder(bookId) {
  return request('/orders/buyNow', {
    method: 'POST',
    body: { bookId },
  })
}

// ============= 用户 =============

export function loginUser(username, password) {
  return request('/users/login', {
    method: 'POST',
    body: { username, password },
  })
}

export function logoutUser() {
  return request('/users/logout', {
    method: 'POST',
  })
}

export function registerUser(username, password, email) {
  return request('/users/register', {
    method: 'POST',
    body: { username, password, email },
  })
}

// 获取当前登录用户信息（无需传username，由后端Session获取）
export function fetchCurrentUser() {
  return request('/users/me')
}

export function fetchUser(username) {
  return request(`/users/${encodeURIComponent(username)}`)
}

// ============= 管理员功能 =============

// 获取所有用户列表
export function fetchAllUsers() {
  return request('/users')
}

// 禁用/解禁用户
export function toggleUserDisabled(id, disabled) {
  return request(`/users/${id}/disabled?disabled=${disabled}`, {
    method: 'PUT',
  })
}

// 添加书籍
export function addBook(bookData) {
  return request('/book', {
    method: 'POST',
    body: bookData,
  })
}

// 更新书籍
export function updateBook(id, bookData) {
  return request(`/book/${id}`, {
    method: 'PUT',
    body: bookData,
  })
}

// 删除书籍
export function deleteBook(id) {
  return request(`/book/${id}`, {
    method: 'DELETE',
  })
}

// ============= 统计功能 =============

// 热销榜（管理员功能）
export function fetchHotBooks(startTime, endTime) {
  const params = new URLSearchParams()
  if (startTime) params.append('startTime', startTime)
  if (endTime) params.append('endTime', endTime)
  return request(`/stat/hotBooks?${params.toString()}`)
}

// 消费榜（管理员功能）
export function fetchTopUsers(startTime, endTime) {
  const params = new URLSearchParams()
  if (startTime) params.append('startTime', startTime)
  if (endTime) params.append('endTime', endTime)
  return request(`/stat/topUsers?${params.toString()}`)
}

// 个人统计（无需传username，由后端Session获取）
export function fetchPersonalStat(startTime, endTime) {
  const params = new URLSearchParams()
  if (startTime) params.append('startTime', startTime)
  if (endTime) params.append('endTime', endTime)
  return request(`/stat/personal?${params.toString()}`)
}

// ============= 智能客服助手（作业2 · Function Calling）=============

/**
 * 与后端智能助手对话。
 * 后端会完成整套 Function Calling 流程（拦截工具调用 → 本地模拟执行 → 回传结果 → 生成回复）。
 * 返回：{ reply: 自然语言回复, toolCalls: [函数调用轨迹], rounds: 交互轮数 }
 * @param {string} message 本轮用户问题
 * @param {Array<{role:string, content:string}>} history 历史对话（可选，用于多轮上下文）
 */
export function chatWithAssistant(message, history = []) {
  return request('/ai/chat', {
    method: 'POST',
    body: { message, history },
  })
}

// ============= 导购 Agent（作业3 · ReAct + RAG）=============

/**
 * 与导购 Agent 对话。
 * 后端执行 ReAct 循环（Thought -> Action -> Observation），返回：
 * { reply, finalThought, steps: [{ step, thought, action, actionInput, observation, success, durationMs }], rounds }
 */
export function chatWithGuideAgent(message, history = []) {
  return request('/agent/chat', {
    method: 'POST',
    body: { message, history },
  })
}

/** 直接调试政策 RAG 检索（不经过大模型），返回 Top-K 条款与相似度 */
export function searchStorePolicy(q, topK = 3) {
  return request(`/agent/policy/search?q=${encodeURIComponent(q)}&topK=${topK}`)
}

/** 政策知识库概况：切片数、向量维度、词表大小等 */
export function fetchPolicyStats() {
  return request('/agent/policy/stats')
}

// 获取《退换货与会员政策》原文
export function fetchPolicyDocument() {
  return request('/agent/policy/document')
}