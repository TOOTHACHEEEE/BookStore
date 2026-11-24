/**
 * 价格解析工具函数
 * 将字符串价格（如 "$49.90"）转换为数字（如 49.90）
 * @param {string} priceStr - 带 $ 符号的价格字符串
 * @returns {number} - 解析后的数字价格
 */
export function parsePrice(priceStr) {
  if (!priceStr) return 0
  return parseFloat(priceStr.replace('$', ''))
}

/**
 * 计算购物车总价
 * @param {Array} cartItems - 购物车商品数组，每项包含 price 和 quantity
 * @returns {number} - 总价
 */
export function calculateCartTotal(cartItems) {
  return cartItems.reduce((sum, item) => {
    return sum + parsePrice(item.price) * item.quantity
  }, 0)
}
