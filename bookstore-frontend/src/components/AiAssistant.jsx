import { useState, useRef, useEffect } from 'react'
import { Button, Input, Avatar, Tooltip } from 'antd'
import {
  RobotOutlined,
  CloseOutlined,
  SendOutlined,
  UserOutlined,
  MinusOutlined,
} from '@ant-design/icons'
import { chatWithAssistant } from '../api/api'
import '../styles/assistant.css'

/**
 * AiAssistant —— 电子书店「智能客服小助手」悬浮对话组件（作业2）
 *
 * 功能：
 *  - 悬浮机器人按钮，点击展开对话面板；
 *  - 用户输入自然语言问题（如"帮我查一下 ISBN 为 xxx 的书还有多少本，别家卖多少钱"）；
 *  - 调用后端 /api/v1/ai/chat，后端触发大模型 Function Calling；
 *  - 面板中【打印】被拦截的函数调用轨迹（函数名 + 参数 + 模拟执行结果 + 是否成功 + 耗时）；
 *  - 最终把模型生成的自然语言回复以气泡形式展示。
 */

// 快捷提问示例，方便一键演示 Function Calling
const SUGGESTIONS = [
  '帮我查一下 ISBN 为 978-7-5366-9293-0 的书还有多少本，顺便看看别家卖多少钱',
  'ISBN 978-7-000-00000-3 这本书本店库存还有吗？',
  '查一下 ISBN 12345 的书还有多少本',
]

let msgSeq = 0
const nextId = () => `m${Date.now()}_${msgSeq++}`

function AiAssistant() {
  const [open, setOpen] = useState(false)
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [messages, setMessages] = useState([
    {
      id: nextId(),
      role: 'assistant',
      content:
        '你好，我是本书店的智能客服小助手「书童」🤖\n我可以帮你查询图书的实时库存，以及比比竞争对手的价格。\n试试点击下面的示例问题，或直接输入你的问题吧！',
    },
  ])
  const listRef = useRef(null)

  // 消息变化时自动滚动到底部
  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight
    }
  }, [messages, loading, open])

  const handleSend = async (text) => {
    const question = (text ?? input).trim()
    if (!question || loading) return

    // 组装历史（仅取纯文本对话，作为多轮上下文）
    const history = messages
      .filter((m) => m.role === 'user' || (m.role === 'assistant' && m.content))
      .map((m) => ({ role: m.role, content: m.content }))

    setMessages((prev) => [...prev, { id: nextId(), role: 'user', content: question }])
    setInput('')
    setLoading(true)

    try {
      const data = await chatWithAssistant(question, history)
      setMessages((prev) => [
        ...prev,
        {
          id: nextId(),
          role: 'assistant',
          content: data.reply || '（模型未返回文本内容）',
          toolCalls: data.toolCalls || [],
          rounds: data.rounds,
        },
      ])
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          id: nextId(),
          role: 'assistant',
          content: `⚠️ 抱歉，助手暂时无法回复：${err.message || '未知错误'}\n请确认后端已启动且已配置 DeepSeek API Key。`,
          error: true,
        },
      ])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <>
      {/* 悬浮机器人按钮 */}
      {!open && (
        <Tooltip title="智能客服小助手" placement="left">
          <div className="ai-fab" onClick={() => setOpen(true)}>
            <RobotOutlined />
            <span className="ai-fab-pulse" />
          </div>
        </Tooltip>
      )}

      {/* 对话面板 */}
      {open && (
        <div className="ai-panel">
          <div className="ai-panel-header">
            <div className="ai-panel-title">
              <Avatar className="ai-header-avatar" icon={<RobotOutlined />} />
              <div>
                <div className="ai-header-name">书童 · 智能客服助手</div>
                <div className="ai-header-status">
                  <span className="ai-dot" /> 在线 · 支持库存查询 / 竞对比价
                </div>
              </div>
            </div>
            <div className="ai-panel-actions">
              <Button
                type="text"
                size="small"
                icon={<MinusOutlined />}
                onClick={() => setOpen(false)}
                className="ai-header-btn"
              />
              <Button
                type="text"
                size="small"
                icon={<CloseOutlined />}
                onClick={() => setOpen(false)}
                className="ai-header-btn"
              />
            </div>
          </div>

          <div className="ai-panel-body" ref={listRef}>
            {messages.map((m) => (
              <div className={`ai-msg-row ${m.role}`} key={m.id}>
                {m.role === 'assistant' && (
                  <Avatar className="ai-avatar-bot" icon={<RobotOutlined />} />
                )}
                <div className={`ai-bubble ${m.role} ${m.error ? 'error' : ''}`}>
                  {m.content}
                </div>
                {m.role === 'user' && (
                  <Avatar className="ai-avatar-user" icon={<UserOutlined />} />
                )}
              </div>
            ))}

            {loading && (
              <div className="ai-msg-row assistant">
                <Avatar className="ai-avatar-bot" icon={<RobotOutlined />} />
                <div className="ai-bubble assistant ai-typing">
                  <span />
                  <span />
                  <span />
                </div>
              </div>
            )}
          </div>

          {/* 快捷提问 */}
          {messages.length <= 1 && (
            <div className="ai-suggestions">
              {SUGGESTIONS.map((s, i) => (
                <div className="ai-suggestion" key={i} onClick={() => handleSend(s)}>
                  {s}
                </div>
              ))}
            </div>
          )}

          <div className="ai-panel-footer">
            <Input.TextArea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="输入你的问题，Enter 发送 / Shift+Enter 换行"
              autoSize={{ minRows: 1, maxRows: 4 }}
              disabled={loading}
              className="ai-input"
            />
            <Button
              type="primary"
              icon={<SendOutlined />}
              onClick={() => handleSend()}
              loading={loading}
              className="ai-send-btn"
            />
          </div>
        </div>
      )}
    </>
  )
}

export default AiAssistant
