import { useEffect, useState } from 'react'
import { Row, Col, Typography, Carousel, Empty, Spin, message } from 'antd'
import BookCard from '../components/BookCard'
import { useCart } from '../context/CartContext'
import { useSearch } from '../context/SearchContext'
import { fetchBooks } from '../api/api'
import '../styles/components.css'

const { Title } = Typography

const bannerImages = [
  'https://images.unsplash.com/photo-1512820790803-83ca734da794?w=1200&h=300&fit=crop',
  'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=1200&h=300&fit=crop',
]

/**
 * BookList 组件 - 书籍列表页
 * 数据通过 fetch 从后端 /api/v1/books 获取
 */
function BookList() {
  const { addToCart, isLoggedIn } = useCart()
  const { searchKeyword } = useSearch()

  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let mounted = true
    setLoading(true)
    fetchBooks()
      .then(data => {
        if (mounted) {
          setBooks(Array.isArray(data) ? data : [])
        }
      })
      .catch(err => {
        console.error(err)
        message.error('获取书籍列表失败：' + err.message)
      })
      .finally(() => {
        if (mounted) setLoading(false)
      })
    return () => { mounted = false }
  }, [])

  const filteredBooks = books.filter(book => {
    if (!searchKeyword.trim()) return true
    const keyword = searchKeyword.toLowerCase()
    return (
      book.title.toLowerCase().includes(keyword) ||
      (book.author || '').toLowerCase().includes(keyword)
    )
  })

  const handleAddToCart = async (book) => {
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

  return (
    <>
      <Carousel autoplay effect="fade" className="carousel-container">
        {bannerImages.map((img, index) => (
          <div key={index}>
            <img src={img} alt={`Banner ${index + 1}`} className="banner-image" />
          </div>
        ))}
      </Carousel>

      <Title level={3} className="section-title">
        📚 热门书籍
        {searchKeyword && (
          <span className="search-result-text">
            （搜索结果：{filteredBooks.length} 本）
          </span>
        )}
      </Title>

      {loading ? (
        <div className="loading-container">
          <Spin size="large" tip="加载中..." />
        </div>
      ) : filteredBooks.length > 0 ? (
        <Row gutter={[24, 24]}>
          {filteredBooks.map(book => (
            <Col key={book.id} xs={24} sm={12} md={8} lg={6} xl={6}>
              <BookCard book={book} onAddToCart={handleAddToCart} />
            </Col>
          ))}
        </Row>
      ) : (
        <Empty
          description={
            searchKeyword ? (
              <span>未找到与 "<strong>{searchKeyword}</strong>" 相关的书籍</span>
            ) : (
              <span>暂无书籍</span>
            )
          }
        />
      )}
    </>
  )
}

export default BookList
