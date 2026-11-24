import { useState, useEffect } from 'react'
import { Card, List, Typography, Tag, Empty, Button, DatePicker, Input, Space, message, Table } from 'antd'
import { ShoppingOutlined, SearchOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useCart } from '../context/CartContext'
import { fetchOrders, searchUserOrders, fetchAllOrders, searchAllOrders } from '../api/api'
import '../styles/components.css'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

/**
 * Orders 组件 - 订单列表页（支持搜索过滤）
 * 
 * 功能：
 * - 普通用户：查看自己的订单，可按时间范围和书籍名称搜索
 * - 管理员：查看所有订单，可按时间范围和书籍名称搜索
 * 
 * 【安全改进】
 * 用户名由后端从Session获取，前端不再传递
 */
function Orders() {
  const { isLoggedIn, user } = useCart()
  const navigate = useNavigate()
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [dateRange, setDateRange] = useState(null)

  // 判断用户角色
  const isAdmin = user && user.role === 'admin'

  // 加载订单
  useEffect(() => {
    if (isLoggedIn && user) {
      loadOrders()
    }
  }, [isLoggedIn, user])

  const loadOrders = async () => {
    setLoading(true)
    try {
      // 无需传username，由后端从Session获取
      const data = isAdmin ? await fetchAllOrders() : await fetchOrders()
      setOrders(data)
    } catch (err) {
      message.error('加载订单失败: ' + err.message)
    }
    setLoading(false)
  }

  // 搜索订单
  const handleSearch = async () => {
    setLoading(true)
    try {
      const startTime = dateRange ? dateRange[0].format('YYYY-MM-DD') : null
      const endTime = dateRange ? dateRange[1].format('YYYY-MM-DD') : null
      
      // 无需传username，由后端从Session获取
      const data = isAdmin 
        ? await searchAllOrders(startTime, endTime, searchText)
        : await searchUserOrders(startTime, endTime, searchText)
      setOrders(data)
      message.success('搜索完成')
    } catch (err) {
      message.error('搜索失败: ' + err.message)
    }
    setLoading(false)
  }

  // 重置搜索
  const handleReset = () => {
    setSearchText('')
    setDateRange(null)
    loadOrders()
  }

  if (!isLoggedIn) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description="请先登录后查看订单"
      >
        <Button type="primary" onClick={() => navigate('/login')}>
          去登录
        </Button>
      </Empty>
    )
  }

  return (
    <>
      <Title level={3}>
        <ShoppingOutlined /> {isAdmin ? '订单管理' : '我的订单'}
      </Title>

      {/* 搜索过滤 */}
      <Card className="orders-search-card">
        <Space wrap>
          <RangePicker 
            value={dateRange}
            onChange={setDateRange}
            placeholder={['开始时间', '结束时间']}
          />
          <Input 
            placeholder="书籍名称"
            value={searchText}
            onChange={e => setSearchText(e.target.value)}
            className="manage-input-small"
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch} loading={loading}>
            搜索
          </Button>
          <Button onClick={handleReset}>
            重置
          </Button>
        </Space>
      </Card>

      {/* 订单列表 */}
      {orders.length === 0 ? (
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="暂无订单"
        >
          <Button type="primary" onClick={() => navigate('/')}>
            去购物
          </Button>
        </Empty>
      ) : (
        <List
          dataSource={orders}
          loading={loading}
          renderItem={(order) => (
            <Card key={order.id} className="orders-card">
              <div className="orders-header">
                <Text strong>订单号：{order.id}</Text>
                <Tag color="blue">{order.status}</Tag>
              </div>
              
              <Text type="secondary">
                用户：{order.username} | 时间：{order.orderTime}
              </Text>
              
              <div className="orders-detail-area">
                <Text>订单详情：</Text>
                {order.items && order.items.map((item, idx) => (
                  <div key={idx} className="orders-item-row">
                    <Text>{item.title} - {item.author}</Text>
                    <Text type="secondary" className="orders-item-secondary">
                      {item.price} × {item.quantity}
                    </Text>
                  </div>
                ))}
              </div>
              
              <div className="orders-total-area">
                <Text strong className="orders-total-text">总金额：{order.total}</Text>
              </div>
            </Card>
          )}
        />
      )}
    </>
  )
}

export default Orders