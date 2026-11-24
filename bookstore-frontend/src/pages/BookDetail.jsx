import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Row, Col, Button, Typography, Tag, Descriptions, Divider, message, Spin } from 'antd'
import {
  ArrowLeftOutlined,
  ShoppingCartOutlined,
  CreditCardOutlined,
  UserOutlined,
  BookOutlined,
  CalendarOutlined,
  FileTextOutlined,
} from '@ant-design/icons'
import { useCart } from '../context/CartContext'
import { fetchBookById } from '../api/api'
import '../styles/components.css'

const { Title, Text, Paragraph } = Typography

function BookDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { addToCart, buyNow, isLoggedIn } = useCart()

  const [book, setBook] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true
    setLoading(true)
    fetchBookById(id)
      .then(data => {
        if (mounted) setBook(data)
      })
      .catch(err => {
        console.error(err)
        message.error('获取书籍详情失败：' + err.message)
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => { mounted = false }
  }, [id])

  const handleAddToCart = async () => {
    if (!isLoggedIn) {
      message.warning('请先登录后再加入购物车')
      return
    }
    try {
      await addToCart(book)
      message.success('✅ 已加入购物车！')
    } catch (err) {
      message.error('加入购物车失败：' + err.message)
    }
  }

  const handleBuyNow = async () => {
    if (!isLoggedIn) {
      message.warning('请先登录后再购买')
      return
    }
    try {
      await buyNow(book)
      message.success('🎉 购买成功！订单已生成')
      navigate('/orders')
    } catch (err) {
      message.error('购买失败：' + err.message)
    }
  }

  if (loading) {
    return (
      <div className="loading-container-large">
        <Spin size="large" tip="加载中..." />
      </div>
    )
  }

  if (!book) {
    return (
      <div className="not-found-container">
        <Title level={3}>未找到该书籍</Title>
        <Button type="primary" onClick={() => navigate('/')}>
          返回首页
        </Button>
      </div>
    )
  }

  return (
    <>
      <Button
        icon={<ArrowLeftOutlined />}
        onClick={() => navigate('/')}
        className="back-button"
      >
        返回书籍列表
      </Button>

      <Card>
        <Row gutter={48}>
          <Col xs={24} md={8}>
            <img
              src={book.cover}
              alt={book.title}
              className="book-cover"
            />
          </Col>

          <Col xs={24} md={16}>
            <Title level={2}>{book.title}</Title>

            <Tag color="blue">热销</Tag>
            <Tag color="green">正版</Tag>

            <Title level={2} type="danger" className="price-title">
              {book.price}
            </Title>

            <Descriptions column={2} size="middle">
              <Descriptions.Item label={<span><UserOutlined /> 作者</span>}>
                {book.author}
              </Descriptions.Item>
              <Descriptions.Item label={<span><BookOutlined /> 出版社</span>}>
                {book.publisher}
              </Descriptions.Item>
              <Descriptions.Item label={<span><CalendarOutlined /> 出版日期</span>}>
                {book.publishDate}
              </Descriptions.Item>
              <Descriptions.Item label={<span><FileTextOutlined /> 页数</span>}>
                {book.pages}
              </Descriptions.Item>
              <Descriptions.Item label="ISBN">
                {book.isbn}
              </Descriptions.Item>
              <Descriptions.Item label="库存">
                {book.stock}
              </Descriptions.Item>
            </Descriptions>

            <Divider />

            <Paragraph>
              <Text strong>内容简介：</Text>
              <br />
              {book.description}
            </Paragraph>

            <Divider />

            <Button
              type="primary"
              size="large"
              icon={<ShoppingCartOutlined />}
              onClick={handleAddToCart}
              className="add-cart-btn"
            >
              加入购物车
            </Button>

            <Button
              type="primary"
              danger
              size="large"
              icon={<CreditCardOutlined />}
              onClick={handleBuyNow}
            >
              立即购买
            </Button>
          </Col>
        </Row>
      </Card>
    </>
  )
}

export default BookDetail
