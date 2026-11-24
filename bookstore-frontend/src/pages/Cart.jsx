import { useNavigate } from 'react-router-dom'
import {
  Card,
  List,
  Button,
  Typography,
  Image,
  Row,
  Col,
  Empty,
  Statistic,
  Divider,
  message,
} from 'antd'
import { DeleteOutlined, ShoppingOutlined } from '@ant-design/icons'
import { useCart } from '../context/CartContext'
import { parsePrice, calculateCartTotal } from '../utils/price'
import '../styles/components.css'

const { Title, Text } = Typography

/**
 * Cart 组件 - 购物车页面
 * 数据来自 CartContext（通过 fetch 与后端同步）
 */
function Cart() {
  const { cart, removeFromCart, checkout, isLoggedIn } = useCart()
  const navigate = useNavigate()

  const total = calculateCartTotal(cart)

  const handleCheckout = async () => {
    try {
      const success = await checkout()
      if (success) {
        message.success('结算成功！订单已生成')
        navigate('/orders')
      }
    } catch (err) {
      message.error('结算失败：' + err.message)
    }
  }

  const handleRemove = async (cartItemId) => {
    try {
      await removeFromCart(cartItemId)
      message.success('已移除')
    } catch (err) {
      message.error('移除失败：' + err.message)
    }
  }

  if (!isLoggedIn) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description="请先登录后查看购物车"
      >
        <Button type="primary" onClick={() => navigate('/login')}>
          去登录
        </Button>
      </Empty>
    )
  }

  if (cart.length === 0) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description="购物车是空的"
      >
        <Button type="primary" onClick={() => navigate('/')}>
          去逛逛
        </Button>
      </Empty>
    )
  }

  return (
    <>
      <Title level={3}>
        <ShoppingOutlined /> 我的购物车
      </Title>

      <Row gutter={24}>
        <Col xs={24} lg={16}>
          <Card>
            <List
              dataSource={cart}
              renderItem={item => {
                const price = parsePrice(item.price)
                const subtotal = price * item.quantity

                return (
                  <List.Item
                    actions={[
                      <Button
                        type="text"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleRemove(item.cartItemId)}
                      >
                        删除
                      </Button>
                    ]}
                  >
                    <List.Item.Meta
                      avatar={
                        <Image
                          src={item.cover}
                          width={80}
                          height={110}
                          className="cart-item-image"
                          preview={false}
                        />
                      }
                      title={item.title}
                      description={
                        <>
                          <Text type="secondary">{item.author}</Text>
                          <br />
                          <Text>{item.price} × {item.quantity}</Text>
                        </>
                      }
                    />
                    <div>
                      <Text strong type="danger">
                        ${subtotal.toFixed(2)}
                      </Text>
                    </div>
                  </List.Item>
                )
              }}
            />
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card>
            <Statistic
              title="商品总计"
              value={total}
              prefix="$"
              precision={2}
              valueStyle={{ color: '#cf1322' }}
            />
            <Divider />
            <Button
              type="primary"
              size="large"
              block
              onClick={handleCheckout}
            >
              去结算
            </Button>
          </Card>
        </Col>
      </Row>
    </>
  )
}

export default Cart
