import { useEffect, useRef, useState } from 'react'
import { Avatar, Button, Card, Input } from 'antd'
import {
  BookOutlined,
  CompassOutlined,
  DeleteOutlined,
  RobotOutlined,
  SafetyCertificateOutlined,
  SendOutlined,
  ThunderboltOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { chatWithGuideAgent } from '../api/api'
import '../styles/guide-agent.css'

/**
 * GuideAgent —— 书店导购 Agent 页面。
 *
 * 只呈现面向用户的对话：模糊找书、退换货与会员政策咨询。
 * ReAct 执行过程（Thought / Action / Observation）由后端终端日志输出，
 * 前端不展示工具名、推理轨迹或向量检索细节。
 */

const WELCOME_ID = 'ga-welcome'

const WELCOME_MESSAGE =
  '你好，我是拾光书店的导购 Agent「书灵」。\n'
  + '你可以用模糊描述告诉我想要什么书，例如“姓刘的作家、书名两个字、很出名”，也可以问我退换货、拆封、会员折扣等政策问题。\n'
  + '我会先查书库或政策条款，再给你可靠的答复。'

const SUGGESTIONS = [
  {
    icon: <BookOutlined />,
    text: '我想买一本关于微服务的书，另外如果我买了不喜欢，拆了塑封还能退吗？',
  },
  {
    icon: <CompassOutlined />,
    text: '我想要一个姓刘的作家的一本书，这本书好像非常出名，书名是两个字',
  },
  {
    icon: <ThunderboltOutlined />,
    text: '我想看科幻小说，有什么推荐？金卡会员买书有折扣吗？',
  },
  {
    icon: <SafetyCertificateOutlined />,
    text: '我买的书有缺页，已经拆封超过 7 天了，还能换吗？',
  },
]

let msgSeq = 0
const nextId = () => `ga-${Date.now()}-${msgSeq++}`

const createWelcomeMessage = () => ({
  id: WELCOME_ID,
  role: 'assistant',
  welcome: true,
  content: WELCOME_MESSAGE,
})

/** 轻量渲染模型回答：支持换行、无序/有序列表与 **加粗**，不注入 HTML */
function renderRichText(text) {
  if (!text) return null
  return text.split('\n').map((line, lineIndex) => {
    const match = line.match(/^\s*([-*]|\d+\.)\s+(.*)$/)
    const marker = match ? match[1] : null
    const content = match ? match[2] : line
    const parts = content.split(/(\*\*[^*]+\*\*)/g).map((part, partIndex) => {
      if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
        return <strong key={partIndex}>{part.slice(2, -2)}</strong>
      }
      return <span key={partIndex}>{part}</span>
    })
    return (
      <div key={lineIndex} className={marker ? 'ga-text-line ga-text-bullet' : 'ga-text-line'}>
        {marker && <span className="ga-bullet-marker">{/^\d/.test(marker) ? marker : '•'}</span>}
        <span className="ga-text-body">{parts}</span>
      </div>
    )
  })
}

function GuideAgent() {
  const [messages, setMessages] = useState([createWelcomeMessage()])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)

  const chatBodyRef = useRef(null)

  useEffect(() => {
    const element = chatBodyRef.current
    if (element) {
      element.scrollTop = element.scrollHeight
    }
  }, [messages, loading])

  const hasConversation = messages.some((item) => !item.welcome)

  const handleSend = async (preset) => {
    const question = (preset ?? input).trim()
    if (!question || loading) return

    const history = messages
      .filter((item) => !item.welcome && !item.error)
      .filter((item) => item.role === 'user' || (item.role === 'assistant' && item.content))
      .map((item) => ({ role: item.role, content: item.content }))

    const userMessage = { id: nextId(), role: 'user', content: question }
    setMessages((prev) => [...prev, userMessage])
    setInput('')
    setLoading(true)

    try {
      const data = await chatWithGuideAgent(question, history)
      setMessages((prev) => [...prev, {
        id: nextId(),
        role: 'assistant',
        content: data.reply || '（模型未返回文本内容）',
      }])
    } catch (error) {
      setMessages((prev) => [...prev, {
        id: nextId(),
        role: 'assistant',
        error: true,
        content: `抱歉，导购 Agent 暂时无法回复：${error.message || '未知错误'}\n`
          + '请确认后端已启动、DeepSeek API Key 可用，并检查控制台日志。',
      }])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      handleSend()
    }
  }

  const handleReset = () => {
    setMessages([createWelcomeMessage()])
    setInput('')
  }

  return (
    <div className="ga-page">
      <section className="ga-hero">
        <div className="ga-hero-main">
          <div className="ga-hero-icon"><CompassOutlined /></div>
          <div>
            <div className="ga-hero-title-row">
              <h2>书灵 · 智能导购 Agent</h2>
            </div>
          </div>
        </div>
      </section>

      <div className="ga-grid">
        <Card className="ga-chat-card" variant="borderless">
          <div className="ga-chat-head">
            <div className="ga-chat-title">
              <Avatar className="ga-chat-avatar" icon={<RobotOutlined />} />
              <div>
                <div className="ga-chat-name">导购对话</div>
                <div className="ga-chat-sub">
                  <span className="ga-status-dot" /> 在线 · 帮你找书、解答退换货与会员政策
                </div>
              </div>
            </div>
            <Button
              type="text"
              icon={<DeleteOutlined />}
              onClick={handleReset}
              disabled={!hasConversation || loading}
              className="ga-reset-btn"
              title="清空对话"
            />
          </div>

          <div className="ga-chat-body" ref={chatBodyRef}>
            {messages.map((item) => (
              <div className={`ga-msg-row ${item.role}`} key={item.id}>
                {item.role === 'assistant' && (
                  <Avatar className="ga-avatar-bot" icon={<RobotOutlined />} />
                )}
                <div className={`ga-bubble ${item.error ? 'ga-bubble-error' : ''}`}>
                  {renderRichText(item.content)}
                </div>
                {item.role === 'user' && (
                  <Avatar className="ga-avatar-user" icon={<UserOutlined />} />
                )}
              </div>
            ))}

            {loading && (
              <div className="ga-msg-row assistant">
                <Avatar className="ga-avatar-bot" icon={<RobotOutlined />} />
                <div className="ga-bubble ga-typing">
                  <span /><span /><span />
                  <em>正在为你查找和思考…</em>
                </div>
              </div>
            )}
          </div>

          {!hasConversation && (
            <div className="ga-suggestions">
              <div className="ga-suggestions-title">试试这些复杂问题</div>
              {SUGGESTIONS.map((suggestion) => (
                <button
                  type="button"
                  className="ga-suggestion"
                  key={suggestion.text}
                  onClick={() => handleSend(suggestion.text)}
                  disabled={loading}
                >
                  <span className="ga-suggestion-icon">{suggestion.icon}</span>
                  <span>{suggestion.text}</span>
                </button>
              ))}
            </div>
          )}

          <div className="ga-chat-footer">
            <Input.TextArea
              value={input}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="描述你想找的书，或询问退换货 / 会员政策，Enter 发送，Shift+Enter 换行"
              autoSize={{ minRows: 1, maxRows: 4 }}
              disabled={loading}
              className="ga-chat-input"
            />
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={() => handleSend()}
              loading={loading}
              className="ga-send-btn"
            >
              发送
            </Button>
          </div>
        </Card>
      </div>
    </div>
  )
}

export default GuideAgent
