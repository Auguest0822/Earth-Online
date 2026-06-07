const fallbackNews = [
  {
    id: 'fallback-climate',
    title: '全球气候议题继续成为今日地球焦点',
    summary: '多个地区围绕极端天气、能源转型和生态保护发布新进展，气候适应与城市韧性成为关注重点。',
    source: 'GDELT 示例',
    publishedAt: new Date().toISOString(),
    category: '气候',
    region: '全球',
    url: '#',
    image: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&q=80&w=900&h=520',
    importance: 96
  },
  {
    id: 'fallback-science',
    title: '空间探索与新技术突破带动科学新闻热度',
    summary: '科研机构和科技企业围绕航天、人工智能、医学研究等方向持续发布成果，公众讨论热度上升。',
    source: 'NewsAPI 示例',
    publishedAt: new Date(Date.now() - 1000 * 60 * 48).toISOString(),
    category: '科技',
    region: '多地区',
    url: '#',
    image: 'https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&q=80&w=900&h=520',
    importance: 88
  },
  {
    id: 'fallback-health',
    title: '公共健康、教育与城市治理成为社会板块热点',
    summary: '今日社会新闻更多聚焦公共服务、社区生活和跨区域协作，呈现出与个人生活密切相连的地球日常。',
    source: 'Earth Online',
    publishedAt: new Date(Date.now() - 1000 * 60 * 92).toISOString(),
    category: '社会',
    region: '全球',
    url: '#',
    image: 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&q=80&w=900&h=520',
    importance: 82
  },
  {
    id: 'fallback-economy',
    title: '能源、贸易和供应链动态影响全球经济观察',
    summary: '国际市场对能源价格、绿色产业和区域贸易变化保持关注，经济事件继续影响人们的日常选择。',
    source: 'Earth Online',
    publishedAt: new Date(Date.now() - 1000 * 60 * 140).toISOString(),
    category: '经济',
    region: '全球',
    url: '#',
    image: 'https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?auto=format&fit=crop&q=80&w=900&h=520',
    importance: 76
  },
  {
    id: 'fallback-ocean',
    title: '海洋生态保护项目带来新的地球观察样本',
    summary: '沿海地区围绕生物多样性、蓝碳生态和海洋污染治理展开新的协作。',
    source: 'Earth Online',
    publishedAt: new Date(Date.now() - 1000 * 60 * 164).toISOString(),
    category: '气候',
    region: '多地区',
    url: '#',
    image: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&q=80&w=900&h=520',
    importance: 74
  },
  {
    id: 'fallback-health-city',
    title: '公共健康和城市治理继续影响日常生活',
    summary: '医疗资源、社区服务和城市安全议题在多个区域新闻中保持较高讨论度。',
    source: 'Earth Online',
    publishedAt: new Date(Date.now() - 1000 * 60 * 210).toISOString(),
    category: '社会',
    region: '全球',
    url: '#',
    image: 'https://images.unsplash.com/photo-1518005020951-eccb494ad742?auto=format&fit=crop&q=80&w=900&h=520',
    importance: 70
  }
]

const fallbackTrends = [
  { name: '气候韧性', value: 96, type: 'climate' },
  { name: '空间探索', value: 88, type: 'science' },
  { name: '公共健康', value: 82, type: 'society' },
  { name: '能源转型', value: 78, type: 'economy' },
  { name: '城市生活', value: 72, type: 'society' }
]

const readJson = async (url) => {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`News request failed: ${response.status}`)
  }
  return response.json()
}

const loadFromConfiguredBackend = async (category) => {
  const backendUrl = import.meta.env.VITE_NEWS_BACKEND_URL || '/api'

  const params = new URLSearchParams({ category })
  return readJson(`${backendUrl.replace(/\/$/, '')}/news?${params.toString()}`)
}

const buildTrends = (items) => {
  const counts = new Map()

  items.forEach((item) => {
    const words = `${item.title} ${item.summary}`
      .toLowerCase()
      .replace(/[^\w\s-]/g, ' ')
      .split(/\s+/)
      .filter((word) => word.length > 5)

    words.slice(0, 10).forEach((word) => {
      counts.set(word, (counts.get(word) || 0) + 1)
    })
  })

  const generated = Array.from(counts.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([name], index) => ({
      name,
      value: Math.max(58, 96 - index * 7),
      type: 'global'
    }))

  return generated.length ? generated : fallbackTrends
}

export const fetchEarthNews = async ({ category = 'all' } = {}) => {
  try {
    const backendPayload = await loadFromConfiguredBackend(category)
    if (backendPayload?.events?.length) {
      return {
        events: backendPayload.events,
        trends: backendPayload.trends?.length ? backendPayload.trends : buildTrends(backendPayload.events),
        sourceMode: backendPayload.sourceMode || 'backend'
      }
    }
  } catch (error) {
    console.warn('[Earth Online] backend news fallback:', error)
  }

  return {
    events: fallbackNews,
    trends: buildTrends(fallbackNews),
    sourceMode: 'fallback'
  }
}
