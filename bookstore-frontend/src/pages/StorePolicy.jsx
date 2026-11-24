import { useEffect, useMemo, useState } from 'react'
import { Alert, Button, Card, Space, Spin, Tag } from 'antd'
import {
  ArrowLeftOutlined,
  CompassOutlined,
  FileTextOutlined,
  SafetyCertificateOutlined,
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { fetchPolicyDocument } from '../api/api'
import '../styles/store-policy.css'

/**
 * StorePolicy —— 《退换货与会员政策》阅读页。
 *
 * 政策原文由后端 /api/v1/agent/policy/document 提供（与 RAG 知识库同源），
 * 左侧目录跳转，右侧逐章展示，用户无需发问即可自行查阅。
 */

/** 把 Markdown 原文解析成 标题 / 元信息 / 章节块 */
function parsePolicy(markdown) {
  const parsed = { title: '退换货与会员政策', meta: [], intro: '', sections: [] }
  let current = null

  for (const raw of (markdown || '').split('\n')) {
    const line = raw.trim()
    if (!line) continue

    if (line.startsWith('# ')) {
      parsed.title = line.slice(2).trim()
      continue
    }
    if (line.startsWith('## ')) {
      current = {
        id: `policy-section-${parsed.sections.length + 1}`,
        title: line.slice(3).trim(),
        blocks: [],
      }
      parsed.sections.push(current)
      continue
    }
    if (line.startsWith('> ')) {
      const text = line.slice(2).trim()
      if (current) current.blocks.push({ type: 'quote', text })
      else parsed.intro = text
      continue
    }

    const metaMatch = line.match(/^(版本|生效日期|适用范围)[:：](.+)$/)
    if (!current && metaMatch) {
      parsed.meta.push({ label: metaMatch[1], value: metaMatch[2].trim() })
      continue
    }

    if (!current) {
      parsed.intro = parsed.intro ? `${parsed.intro}\n${line}` : line
      continue
    }

    if (/^问[:：]/.test(line)) {
      current.blocks.push({ type: 'question', text: line })
    } else if (/^答[:：]/.test(line)) {
      current.blocks.push({ type: 'answer', text: line })
    } else if (/^\d+\.\s+/.test(line)) {
      current.blocks.push({ type: 'item', text: line })
    } else {
      current.blocks.push({ type: 'paragraph', text: line })
    }
  }
  return parsed
}

/** 行内 **加粗**，不注入 HTML */
function renderInline(text) {
  return text.split(/(\*\*[^*]+\*\*)/g).map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**') && part.length > 4) {
      return <strong key={index}>{part.slice(2, -2)}</strong>
    }
    return <span key={index}>{part}</span>
  })
}

function PolicyBlock({ block }) {
  if (block.type === 'item') {
    const match = block.text.match(/^(\d+)\.\s+(.*)$/)
    return (
      <div className="sp-item">
        <span className="sp-item-index">{match ? match[1] : '·'}</span>
        <p>{renderInline(match ? match[2] : block.text)}</p>
      </div>
    )
  }
  if (block.type === 'question') {
    return <p className="sp-question">{renderInline(block.text)}</p>
  }
  if (block.type === 'answer') {
    return <p className="sp-answer">{renderInline(block.text)}</p>
  }
  if (block.type === 'quote') {
    return <blockquote className="sp-quote">{renderInline(block.text)}</blockquote>
  }
  return <p className="sp-paragraph">{renderInline(block.text)}</p>
}

function StorePolicy() {
  const navigate = useNavigate()
  const [doc, setDoc] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = () => {
    setLoading(true)
    setError('')
    fetchPolicyDocument()
      .then(setDoc)
      .catch((err) => setError(err.message || '政策加载失败'))
      .finally(() => setLoading(false))
  }

  useEffect(() => {
    load()
  }, [])

  const policy = useMemo(() => parsePolicy(doc?.content), [doc])

  const handleJump = (id) => {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  return (
    <div className="sp-page">
      <section className="sp-hero">
        <div className="sp-hero-main">
          <div className="sp-hero-icon"><SafetyCertificateOutlined /></div>
          <div>
            <h1>{policy.title}</h1>
            <div className="sp-hero-meta">
              {policy.meta.map((item) => (
                <Tag key={item.label}>{item.label}：{item.value}</Tag>
              ))}
              {doc?.chars ? <Tag>全文 {doc.chars} 字</Tag> : null}
            </div>
          </div>
        </div>
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/')}>
            返回首页
          </Button>
          <Button type="primary" icon={<CompassOutlined />} onClick={() => navigate('/guide')}>
            咨询书灵
          </Button>
        </Space>
      </section>

      {loading && (
        <div className="sp-loading"><Spin /> 正在加载政策原文…</div>
      )}

      {!loading && error && (
        <Alert
          type="error"
          showIcon
          message="政策加载失败"
          description={`${error}。请确认后端服务已启动。`}
          action={<Button size="small" onClick={load}>重试</Button>}
        />
      )}

      {!loading && !error && (
        <>
          {policy.intro && (
            <div className="sp-intro">
              <FileTextOutlined />
              <p>{renderInline(policy.intro)}</p>
            </div>
          )}

          <div className="sp-layout">
            <aside className="sp-toc">
              <div className="sp-toc-title">政策目录</div>
              {policy.sections.map((section, index) => (
                <button
                  type="button"
                  className="sp-toc-item"
                  key={section.id}
                  onClick={() => handleJump(section.id)}
                >
                  <span className="sp-toc-index">{String(index + 1).padStart(2, '0')}</span>
                  <span>{section.title.replace(/^[一二三四五六七八九十]+、/, '')}</span>
                </button>
              ))}
            </aside>

            <main className="sp-content">
              {policy.sections.map((section) => (
                <Card
                  key={section.id}
                  id={section.id}
                  className="sp-section"
                  variant="borderless"
                  title={section.title}
                >
                  {section.blocks.map((block, index) => (
                    <PolicyBlock block={block} key={`${section.id}-${index}`} />
                  ))}
                </Card>
              ))}
            </main>
          </div>
        </>
      )}
    </div>
  )
}

export default StorePolicy
