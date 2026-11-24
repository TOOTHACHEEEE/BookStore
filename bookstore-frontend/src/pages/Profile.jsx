import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Card,
  Form,
  Input,
  Button,
  Typography,
  Avatar,
  message,
  Divider,
  Row,
  Col,
  Empty,
} from 'antd'
import {
  UserOutlined,
  SaveOutlined,
  MailOutlined,
  PhoneOutlined,
  EnvironmentOutlined,
} from '@ant-design/icons'
import { useCart } from '../context/CartContext'
import '../styles/components.css'

const { Title } = Typography

/**
 * Profile 组件 - 个人信息
 * 用户数据来自登录后保存的 user（来自后端登录响应）
 */
function Profile() {
  const [form] = Form.useForm()
  const { isLoggedIn, user } = useCart()
  const navigate = useNavigate()

  useEffect(() => {
    if (user) {
      form.setFieldsValue({
        nickname: user.name,
        email: user.email || `${user.name}@example.com`,
        phone: '138****8888',
        address: '北京市朝阳区XX街道XX号',
      })
    }
  }, [user, form])

  const handleSave = () => {
    message.success('✅ 个人信息已保存！')
  }

  if (!isLoggedIn || !user) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description="请先登录后查看个人信息"
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
        <UserOutlined /> 个人信息
      </Title>

      <Row justify="center">
        <Col xs={24} md={16} lg={12}>
          <Card>
            <div className="profile-avatar-area">
              <Avatar
                size={100}
                src={user.avatar}
                icon={<UserOutlined />}
              />
              <Title level={4} className="profile-name-title">
                {user.name}
              </Title>
            </div>

            <Divider>基本信息</Divider>

            <Form form={form} layout="vertical">
              <Form.Item
                name="nickname"
                label="昵称"
                rules={[{ required: true, message: '请输入昵称' }]}
              >
                <Input prefix={<UserOutlined />} placeholder="请输入昵称" />
              </Form.Item>

              <Form.Item
                name="email"
                label="邮箱"
                rules={[
                  { type: 'email', message: '请输入有效的邮箱地址' },
                  { required: true, message: '请输入邮箱' },
                ]}
              >
                <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
              </Form.Item>

              <Form.Item name="phone" label="手机号码">
                <Input prefix={<PhoneOutlined />} placeholder="请输入手机号码" />
              </Form.Item>

              <Form.Item name="address" label="收货地址">
                <Input.TextArea
                  prefix={<EnvironmentOutlined />}
                  placeholder="请输入收货地址"
                  rows={3}
                />
              </Form.Item>

              <Form.Item>
                <Button
                  type="primary"
                  size="large"
                  block
                  icon={<SaveOutlined />}
                  onClick={handleSave}
                >
                  保存修改
                </Button>
              </Form.Item>
            </Form>
          </Card>
        </Col>
      </Row>
    </>
  )
}

export default Profile
