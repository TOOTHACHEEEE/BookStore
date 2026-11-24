import { Card, Button, Typography, Image } from 'antd'
import { ShoppingCartOutlined, EyeOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import '../styles/components.css'

const { Title, Text } = Typography

function BookCard({ book, onAddToCart }) {
  const navigate = useNavigate()

  return (
    <Card
      hoverable
      cover={
        <Image
          src={book.cover}
          alt={book.title}
          height={260}
          className="book-card-image"
          preview={false}
        />
      }
      actions={[
        <Button
          type="text"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/book/${book.id}`)}
          key="view"
        >
          查看详情
        </Button>,
        <Button
          type="text"
          icon={<ShoppingCartOutlined />}
          onClick={() => onAddToCart(book)}
          key="cart"
        >
          加入购物车
        </Button>,
      ]}
    >
      <Card.Meta
        title={<Title level={5} className="book-card-title">{book.title}</Title>}
        description={
          <div>
            <Text type="secondary">{book.author}</Text>
            <br />
            <Text type="danger" strong className="book-card-price">
              {book.price}
            </Text>
          </div>
        }
      />
    </Card>
  )
}

export default BookCard