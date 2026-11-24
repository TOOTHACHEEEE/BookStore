import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Card, Form, Input, Button, Typography, message, ConfigProvider, theme, Tabs } from 'antd'
import { UserOutlined, LockOutlined, BookOutlined, MailOutlined } from '@ant-design/icons'
import { useCart } from '../context/CartContext'
import { loginUser, registerUser } from '../api/api'
import '../styles/components.css'

const { Title } = Typography

/**
 * Login 组件 - 登录/注册页面
 * 
 * 功能：
 * - 用户登录（用户名 + 密码）
 * - 用户注册（用户名 + 密码 + 重复密码 + 邓箱）
 * 
 * 注册验证：
 * - 用户名不能为空
 * - 两次密码必须相同
 * - 邮箱格式验证
 */
function Login() {
  const navigate = useNavigate()
  const { login } = useCart()
  const [loginForm] = Form.useForm()
  const [registerForm] = Form.useForm()
  const [submitting, setSubmitting] = useState(false)

  // 登录处理
  const handleLogin = async (values) => {
    setSubmitting(true)
    try {
      const data = await loginUser(values.username, values.password)
      login(data)
      message.success('登录成功！')
      navigate('/')
    } catch (err) {
      message.error('登录失败：' + err.message)
    } finally {
      setSubmitting(false)
    }
  }

  // 注册处理
  const handleRegister = async (values) => {
    setSubmitting(true)
    try {
      // 验证两次密码是否相同（前端已通过Form.Item rules验证）
      await registerUser(values.username, values.password, values.email)
      message.success('注册成功！请登录')
      registerForm.resetFields()
      // 自动切换到登录tab
      message.info('请使用刚才注册的账号登录')
    } catch (err) {
      message.error('注册失败：' + err.message)
    } finally {
      setSubmitting(false)
    }
  }

  // 邮箱格式验证
  const validateEmail = (_, value) => {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!value) {
      return Promise.reject(new Error('请输入邮箱'))
    }
    if (!emailRegex.test(value)) {
      return Promise.reject(new Error('邮箱格式不正确'))
    }
    return Promise.resolve()
  }

  // 重复密码验证
  const validateConfirmPassword = ({ getFieldValue }) => ({
    validator(_, value) {
      if (!value) {
        return Promise.reject(new Error('请再次输入密码'))
      }
      if (value !== getFieldValue('password')) {
        return Promise.reject(new Error('两次输入的密码不一致'))
      }
      return Promise.resolve()
    },
  })

  return (
    <ConfigProvider
      theme={{
        algorithm: theme.defaultAlgorithm,
        token: {
          colorPrimary: '#1890ff',
        },
      }}
    >
      <div className="login-page">
        <Card className="login-card">
          <div className="login-logo-area">
            <BookOutlined className="login-icon" />
            <Title level={3} className="login-title">
              Book Store
            </Title>
            <Typography.Text type="secondary">电子书城</Typography.Text>
          </div>

          <Tabs
            defaultActiveKey="login"
            centered
            items={[
              {
                key: 'login',
                label: '登录',
                children: (
                  <Form
                    form={loginForm}
                    name="login"
                    initialValues={{
                      username: 'toothache',
                      password: '123456',
                    }}
                    onFinish={handleLogin}
                    autoComplete="off"
                  >
                    <Form.Item
                      name="username"
                      rules={[{ required: true, message: '请输入用户名' }]}
                    >
                      <Input
                        prefix={<UserOutlined />}
                        placeholder="用户名"
                        size="large"
                      />
                    </Form.Item>

                    <Form.Item
                      name="password"
                      rules={[{ required: true, message: '请输入密码' }]}
                    >
                      <Input.Password
                        prefix={<LockOutlined />}
                        placeholder="密码"
                        size="large"
                      />
                    </Form.Item>

                    <Form.Item>
                      <Button
                        type="primary"
                        htmlType="submit"
                        size="large"
                        block
                        loading={submitting}
                        className="login-btn"
                      >
                        登录
                      </Button>
                    </Form.Item>
                  </Form>
                ),
              },
              {
                key: 'register',
                label: '注册',
                children: (
                  <Form
                    form={registerForm}
                    name="register"
                    onFinish={handleRegister}
                    autoComplete="off"
                  >
                    <Form.Item
                      name="username"
                      rules={[
                        { required: true, message: '请输入用户名' },
                        { min: 3, message: '用户名至少3个字符' },
                      ]}
                    >
                      <Input
                        prefix={<UserOutlined />}
                        placeholder="用户名"
                        size="large"
                      />
                    </Form.Item>

                    <Form.Item
                      name="password"
                      rules={[
                        { required: true, message: '请输入密码' },
                        { min: 6, message: '密码至少6个字符' },
                      ]}
                    >
                      <Input.Password
                        prefix={<LockOutlined />}
                        placeholder="密码"
                        size="large"
                      />
                    </Form.Item>

                    <Form.Item
                      name="confirmPassword"
                      dependencies={['password']}
                      rules={[
                        { required: true, message: '请再次输入密码' },
                        validateConfirmPassword,
                      ]}
                    >
                      <Input.Password
                        prefix={<LockOutlined />}
                        placeholder="重复密码"
                        size="large"
                      />
                    </Form.Item>

                    <Form.Item
                      name="email"
                      rules={[{ required: true }, validateEmail]}
                    >
                      <Input
                        prefix={<MailOutlined />}
                        placeholder="邮箱"
                        size="large"
                      />
                    </Form.Item>

                    <Form.Item>
                      <Button
                        type="primary"
                        htmlType="submit"
                        size="large"
                        block
                        loading={submitting}
                        className="login-btn"
                      >
                        注册
                      </Button>
                    </Form.Item>
                  </Form>
                ),
              },
            ]}
          />

          <div className="login-tip-area">
            <Typography.Text type="secondary" className="login-tip-text">
              演示账号：toothache / 123456（顾客）<br />
              管理员账号：admin / admin123
            </Typography.Text>
          </div>
        </Card>
      </div>
    </ConfigProvider>
  )
}

export default Login
