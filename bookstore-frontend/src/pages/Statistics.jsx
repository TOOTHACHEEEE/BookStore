import { useState, useEffect } from 'react'
import { Card, Table, DatePicker, Button, Space, message, Typography, Statistic, Row, Col, Tabs } from 'antd'
import { BarChartOutlined, TrophyOutlined, UserOutlined, BookOutlined } from '@ant-design/icons'
import { useCart } from '../context/CartContext'
import { fetchHotBooks, fetchTopUsers, fetchPersonalStat } from '../api/api'

const { Title } = Typography
const { RangePicker } = DatePicker

/**
 * 统计页面
 * 
 * 功能：
 * - 管理员：热销榜、消费榜
 * - 普通用户：个人统计
 */
function Statistics() {
  const { user } = useCart()
  const [loading, setLoading] = useState(false)
  const [dateRange, setDateRange] = useState(null)
  
  // 管理员数据
  const [hotBooks, setHotBooks] = useState([])
  const [topUsers, setTopUsers] = useState([])
  
  // 用户个人数据
  const [personalStat, setPersonalStat] = useState(null)

  const isAdmin = user && user.role === 'admin'

  useEffect(() => {
    if (user) {
      loadStat()
    }
  }, [user])

  const loadStat = async () => {
    setLoading(true)
    const startTime = dateRange ? dateRange[0].format('YYYY-MM-DD') : null
    const endTime = dateRange ? dateRange[1].format('YYYY-MM-DD') : null
    
    try {
      if (isAdmin) {
        const hotData = await fetchHotBooks(startTime, endTime)
        const topData = await fetchTopUsers(startTime, endTime)
        setHotBooks(hotData)
        setTopUsers(topData)
      } else {
        // 无需传username，由后端从Session获取
        const data = await fetchPersonalStat(startTime, endTime)
        setPersonalStat(data)
      }
    } catch (err) {
      message.error('加载统计数据失败: ' + err.message)
    }
    setLoading(false)
  }

  const handleSearch = () => {
    loadStat()
  }

  const handleReset = () => {
    setDateRange(null)
    loadStat()
  }

  // 热销榜表格列定义
  const hotBooksColumns = [
    { title: '排名', key: 'rank', render: (_, __, index) => index + 1 },
    { title: '书名', dataIndex: 'title', key: 'title' },
    { title: '作者', dataIndex: 'author', key: 'author' },
    { title: '销量（本）', dataIndex: 'totalQuantity', key: 'totalQuantity', sorter: (a, b) => a.totalQuantity - b.totalQuantity },
    { title: '销售金额', dataIndex: 'totalAmount', key: 'totalAmount', render: v => `$${v}` },
  ]

  // 消费榜表格列定义
  const topUsersColumns = [
    { title: '排名', key: 'rank', render: (_, __, index) => index + 1 },
    { title: '用户', dataIndex: 'username', key: 'username' },
    { title: '购书数量（本）', dataIndex: 'totalBooks', key: 'totalBooks' },
    { title: '消费金额', dataIndex: 'totalAmount', key: 'totalAmount', render: v => `$${v}`, sorter: (a, b) => a.totalAmount - b.totalAmount },
  ]

  // 个人统计表格列定义
  const personalColumns = [
    { title: '书名', dataIndex: 'title', key: 'title' },
    { title: '购买数量', dataIndex: 'quantity', key: 'quantity' },
    { title: '花费金额', dataIndex: 'subtotal', key: 'subtotal', render: v => `$${v}` },
  ]

  if (!user) {
    return <Card><Typography.Text>请先登录</Typography.Text></Card>
  }

  return (
    <div className="statistics-page">
      <Title level={3}>
        <BarChartOutlined /> {isAdmin ? '统计分析' : '我的购书统计'}
      </Title>

      {/* 时间范围选择 */}
      <Card className="statistics-search-card">
        <Space>
          <RangePicker 
            value={dateRange}
            onChange={setDateRange}
            placeholder={['开始时间', '结束时间']}
          />
          <Button type="primary" onClick={handleSearch} loading={loading}>
            查询
          </Button>
          <Button onClick={handleReset}>
            重置
          </Button>
        </Space>
      </Card>

      {isAdmin ? (
        // 管理员：热销榜 + 消费榜
        <Tabs
          items={[
            {
              key: 'hotBooks',
              label: <span><TrophyOutlined /> 热销榜</span>,
              children: (
                <Card title="热销书籍排行">
                  <Table 
                    dataSource={hotBooks}
                    columns={hotBooksColumns}
                    rowKey="bookId"
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                  />
                </Card>
              ),
            },
            {
              key: 'topUsers',
              label: <span><UserOutlined /> 消费榜</span>,
              children: (
                <Card title="用户消费排行">
                  <Table 
                    dataSource={topUsers}
                    columns={topUsersColumns}
                    rowKey="username"
                    loading={loading}
                    pagination={{ pageSize: 10 }}
                  />
                </Card>
              ),
            },
          ]}
        />
      ) : (
        // 普通用户：个人统计
        <div>
          <Row gutter={16} className="statistics-row">
            <Col span={8}>
              <Card>
                <Statistic 
                  title="购书总本数" 
                  value={personalStat?.totalBooks || 0} 
                  prefix={<BookOutlined />}
                />
              </Card>
            </Col>
            <Col span={8}>
              <Card>
                <Statistic 
                  title="购书总金额" 
                  value={personalStat?.totalAmount || 0} 
                  prefix="$"
                  precision={2}
                />
              </Card>
            </Col>
          </Row>

          <Card title="书籍购买明细">
            <Table 
              dataSource={personalStat?.books || []}
              columns={personalColumns}
              rowKey="bookId"
              loading={loading}
              pagination={{ pageSize: 10 }}
            />
          </Card>
        </div>
      )}
    </div>
  )
}

export default Statistics