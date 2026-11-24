import { useState, useEffect } from 'react'
import { Table, Button, Modal, Form, Input, InputNumber, message, Space, Popconfirm } from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { fetchBooks, addBook, updateBook, deleteBook } from '../api/api'
import { parsePrice } from '../utils/price'

/**
 * 书籍管理页面（管理员功能）
 * 
 * 功能：
 * - 查看所有书籍列表（书名、作者、封面、ISBN、库存）
 * - 搜索书籍（按书名）
 * - 添加新书籍
 * - 编辑书籍信息
 * - 删除书籍
 */
function BookManage() {
  const [books, setBooks] = useState([])
  const [loading, setLoading] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [modalVisible, setModalVisible] = useState(false)
  const [editingBook, setEditingBook] = useState(null)
  const [form] = Form.useForm()

  // 加载书籍列表
  useEffect(() => {
    loadBooks()
  }, [])

  const loadBooks = async () => {
    setLoading(true)
    try {
      const data = await fetchBooks()
      setBooks(data)
    } catch (err) {
      message.error('加载书籍失败: ' + err.message)
    }
    setLoading(false)
  }

  // 搜索过滤
  const filteredBooks = books.filter(book =>
    book.title.toLowerCase().includes(searchText.toLowerCase())
  )

  // 打开添加/编辑弹窗
  const openModal = (book = null) => {
    setEditingBook(book)
    if (book) {
      form.setFieldsValue({
        title: book.title,
        author: book.author,
        price: parsePrice(book.price),
        isbn: book.isbn,
        cover: book.cover,
        description: book.description,
        publisher: book.publisher,
        publishDate: book.publishDate,
        pages: book.pages,
        stock: book.stock,
      })
    } else {
      form.resetFields()
    }
    setModalVisible(true)
  }

  // 提交表单
  const handleSubmit = async (values) => {
    try {
      if (editingBook) {
        await updateBook(editingBook.id, values)
        message.success('更新成功')
      } else {
        await addBook(values)
        message.success('添加成功')
      }
      setModalVisible(false)
      loadBooks()
    } catch (err) {
      message.error('操作失败: ' + err.message)
    }
  }

  // 删除书籍
  const handleDelete = async (id) => {
    try {
      await deleteBook(id)
      message.success('删除成功')
      loadBooks()
    } catch (err) {
      message.error('删除失败: ' + err.message)
    }
  }

  const columns = [
    {
      title: '封面',
      dataIndex: 'cover',
      key: 'cover',
      width: 80,
      render: (cover) => (
        <img src={cover} alt="封面" className="book-manage-cover-img" />
      ),
    },
    {
      title: '书名',
      dataIndex: 'title',
      key: 'title',
    },
    {
      title: '作者',
      dataIndex: 'author',
      key: 'author',
    },
    {
      title: 'ISBN',
      dataIndex: 'isbn',
      key: 'isbn',
    },
    {
      title: '价格',
      dataIndex: 'price',
      key: 'price',
    },
    {
      title: '库存',
      dataIndex: 'stock',
      key: 'stock',
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => openModal(record)}
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除这本书吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div className="manage-page">
      <h2>书籍管理</h2>
      
      <Space className="manage-space">
        <Input.Search
          placeholder="按书名搜索"
          value={searchText}
          onChange={(e) => setSearchText(e.target.value)}
          className="manage-input"
        />
        <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()}>
          添加书籍
        </Button>
      </Space>

      <Table
        columns={columns}
        dataSource={filteredBooks}
        rowKey="id"
        loading={loading}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingBook ? '编辑书籍' : '添加书籍'}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        footer={null}
        width={600}
      >
        <Form form={form} onFinish={handleSubmit} layout="vertical">
          <Form.Item
            name="title"
            label="书名"
            rules={[{ required: true, message: '请输入书名' }]}
          >
            <Input />
          </Form.Item>
          <Form.Item name="author" label="作者">
            <Input />
          </Form.Item>
          <Form.Item
            name="price"
            label="价格"
            rules={[{ required: true, message: '请输入价格' }]}
          >
            <InputNumber min={0} precision={2} className="manage-input-full" />
          </Form.Item>
          <Form.Item name="isbn" label="ISBN">
            <Input />
          </Form.Item>
          <Form.Item name="cover" label="封面URL">
            <Input />
          </Form.Item>
          <Form.Item name="description" label="简介">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="publisher" label="出版社">
            <Input />
          </Form.Item>
          <Form.Item name="publishDate" label="出版日期">
            <Input />
          </Form.Item>
          <Form.Item name="pages" label="页数">
            <InputNumber min={0} />
          </Form.Item>
          <Form.Item name="stock" label="库存" initialValue={100}>
            <InputNumber min={0} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                {editingBook ? '更新' : '添加'}
              </Button>
              <Button onClick={() => setModalVisible(false)}>取消</Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default BookManage