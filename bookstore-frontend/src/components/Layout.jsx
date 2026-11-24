import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Layout, Menu, Typography, Avatar, Space, Input, Badge, ConfigProvider, theme, Dropdown, Button } from 'antd'
import {
  BookOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
  UserOutlined,
  SearchOutlined,
  LogoutOutlined,
  SettingOutlined,
  TeamOutlined,
  BarChartOutlined,
  CompassOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import { useCart } from '../context/CartContext'
import { useSearch } from '../context/SearchContext'
import AiAssistant from './AiAssistant'
import '../styles/components.css'

const { Sider, Content, Header } = Layout
const { Title } = Typography

/**
 * AppLayout 组件 - 应用主布局
 * 用户信息和购物车均来自 CartContext（已与后端同步）
 */
function AppLayout() {
  const location = useLocation()
  const navigate = useNavigate()

  const { cart, isLoggedIn, user, logout } = useCart()
  const { searchKeyword, setSearchKeyword } = useSearch()

  // 根据用户角色显示不同菜单
  const isAdmin = user && user.role === 'admin'
  
  const menuItems = isAdmin ? [
    // 管理员菜单
    { key: '/', icon: <BookOutlined />, label: '书籍首页' },
    { key: '/guide', icon: <CompassOutlined />, label: '智能导购' },
    { key: '/admin/books', icon: <SettingOutlined />, label: '书籍管理' },
    { key: '/admin/users', icon: <TeamOutlined />, label: '用户管理' },
    { key: '/orders', icon: <ShoppingOutlined />, label: '订单管理' },
    { key: '/statistics', icon: <BarChartOutlined />, label: '统计分析' },
    { key: '/profile', icon: <UserOutlined />, label: '个人信息' },
  ] : [
    // 普通用户菜单
    { key: '/', icon: <BookOutlined />, label: '书籍首页' },
    { key: '/guide', icon: <CompassOutlined />, label: '智能导购' },
    { key: '/cart', icon: <ShoppingCartOutlined />, label: '我的购物车' },
    { key: '/orders', icon: <ShoppingOutlined />, label: '我的订单' },
    { key: '/statistics', icon: <BarChartOutlined />, label: '购书统计' },
    { key: '/profile', icon: <UserOutlined />, label: '个人信息' },
  ]

  const handleMenuClick = ({ key }) => {
    navigate(key)
  }

  const cartCount = cart.reduce((sum, item) => sum + item.quantity, 0)

  const handleSearch = () => {
    if (location.pathname !== '/') {
      navigate('/')
    }
  }

  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: () => {
        logout()
        navigate('/login')
      },
    },
  ]

  return (
    <ConfigProvider
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#1890ff',
          borderRadius: 6,
        },
      }}
    >
      <Layout className="layout-main">
        <Sider className="layout-sider" width={220}>
          <div className="layout-logo-area">
            <BookOutlined className="layout-logo-icon" />
            <Title level={4} className="layout-logo-title">
              Book Store
            </Title>
          </div>

          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            items={menuItems}
            onClick={handleMenuClick}
            className="layout-menu"
          />

          <div className="layout-bottom-area">
            {isLoggedIn ? (
              <Button
                danger
                icon={<LogoutOutlined />}
                block
                onClick={() => {
                  logout()
                  navigate('/login')
                }}
              >
                退出登录
              </Button>
            ) : (
              <Button
                type="primary"
                icon={<UserOutlined />}
                block
                onClick={() => navigate('/login')}
              >
                登录
              </Button>
            )}
          </div>
        </Sider>

        <Layout className="layout-content-area">
          <Header className="layout-header">
            <Input.Search
              placeholder="搜索书籍（书名、作者）..."
              allowClear
              enterButton={<SearchOutlined />}
              className="layout-search"
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onSearch={handleSearch}
            />

            <Space size="middle">
              <Button
                className="layout-policy-btn"
                icon={<SafetyCertificateOutlined />}
                onClick={() => navigate('/policy')}
              >
                退换货与会员政策
              </Button>
              <Badge count={cartCount}>
                <ShoppingCartOutlined
                  className="layout-cart-icon"
                  onClick={() => navigate('/cart')}
                />
              </Badge>

              {isLoggedIn && user ? (
                <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
                  <Space className="layout-user-area">
                    <Avatar src={user.avatar} icon={<UserOutlined />} />
                    <span>Hi, {user.name}</span>
                  </Space>
                </Dropdown>
              ) : (
                <Space className="layout-user-area" onClick={() => navigate('/login')}>
                  <Avatar icon={<UserOutlined />} />
                  <span>未登录</span>
                </Space>
              )}
            </Space>
          </Header>

          <Content className="layout-content">
            <div className="layout-content-inner">
              <Outlet />
            </div>
          </Content>
        </Layout>
      </Layout>

      {/* 作业2：智能客服小助手（悬浮于所有页面右下角） */}
      <AiAssistant />
    </ConfigProvider>
  )
}

export default AppLayout
