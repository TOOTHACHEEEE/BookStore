import { useState, useEffect } from 'react'
import { Table, Button, Tag, Space, message, Popconfirm } from 'antd'
import { LockOutlined, UnlockOutlined } from '@ant-design/icons'
import { fetchAllUsers, toggleUserDisabled } from '../api/api'

/**
 * 用户管理页面（管理员功能）
 * 
 * 功能：
 * - 查看所有用户列表
 * - 禁用/解禁用户
 */
function UserManage() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    loadUsers()
  }, [])

  const loadUsers = async () => {
    setLoading(true)
    try {
      const data = await fetchAllUsers()
      setUsers(data)
    } catch (err) {
      message.error('加载用户失败: ' + err.message)
    }
    setLoading(false)
  }

  // 禁用/解禁用户
  const handleToggleDisabled = async (id, currentDisabled) => {
    try {
      const newDisabled = !currentDisabled
      await toggleUserDisabled(id, newDisabled)
      message.success(newDisabled ? '用户已禁用' : '用户已解禁')
      loadUsers()
    } catch (err) {
      message.error('操作失败: ' + err.message)
    }
  }

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
    },
    {
      title: '用户名',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role) => (
        <Tag color={role === 'admin' ? 'blue' : 'green'}>
          {role === 'admin' ? '管理员' : '顾客'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'disabled',
      key: 'disabled',
      render: (disabled) => (
        <Tag color={disabled ? 'red' : 'green'}>
          {disabled ? '已禁用' : '正常'}
        </Tag>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_, record) => {
        // 不能禁用管理员
        if (record.role === 'admin') {
          return <span className="user-manage-disabled-text">-</span>
        }
        return (
          <Popconfirm
            title={record.disabled ? '确定解禁该用户吗？' : '确定禁用该用户吗？'}
            onConfirm={() => handleToggleDisabled(record.id, record.disabled)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              danger={!record.disabled}
              icon={record.disabled ? <UnlockOutlined /> : <LockOutlined />}
            >
              {record.disabled ? '解禁' : '禁用'}
            </Button>
          </Popconfirm>
        )
      },
    },
  ]

  return (
    <div className="user-manage-page">
      <h2>用户管理</h2>
      <p className="user-manage-section-title">
        管理员可以禁用普通用户，被禁用的用户将无法登录系统。
      </p>
      
      <Table
        columns={columns}
        dataSource={users}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
      />
    </div>
  )
}

export default UserManage