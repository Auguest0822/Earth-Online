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

const loadFromConfiguredBackend = async (category) => {
  return request('/news', { params: { category } })
}

const buildStats = (items, field, fallbackName) => {
  const counts = new Map()
  items.forEach((item) => {
    const name = item[field]?.trim() || fallbackName
    counts.set(name, (counts.get(name) || 0) + 1)
  })

  const stats = Array.from(counts.entries())
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 6)
  const maxCount = Math.max(...stats.map((item) => item.count), 1)
  return stats.map((item) => ({
    ...item,
    percentage: Math.round((item.count / maxCount) * 100)
  }))
}

const buildFallbackPayload = () => ({
  events: fallbackNews,
  categoryStats: buildStats(fallbackNews, 'category', '全球'),
  regionStats: buildStats(fallbackNews, 'region', '未知地区'),
  trends: buildStats(fallbackNews, 'category', '全球').map((item) => ({
    name: item.name,
    value: item.percentage
  })),
  sourceMode: 'fallback',
  meta: {
    sourceMode: 'fallback',
    sources: ['本地演示数据'],
    updatedAt: new Date().toISOString(),
    cached: false,
    cacheMinutes: 10,
    dataStatus: 'fallback',
    message: '新闻接口暂时不可用，当前显示本地演示数据。'
  }
})

const normalizeDataSource = (value) => ['mock', 'api', 'auto'].includes(value) ? value : 'auto'

export const getNewsDataSource = () => normalizeDataSource(
  localStorage.getItem('earth-online-news-source') || import.meta.env.VITE_NEWS_DATA_SOURCE
)

export const setNewsDataSource = (source) => {
  localStorage.setItem('earth-online-news-source', normalizeDataSource(source))
}

export const fetchEarthNews = async ({ category = 'all', source = getNewsDataSource() } = {}) => {
  const dataSource = normalizeDataSource(source)

  if (dataSource === 'mock') {
    const payload = buildFallbackPayload()
    payload.meta.message = '当前手动选择 Mock 演示数据，不依赖网络和后端服务。'
    return payload
  }

  try {
    const backendPayload = await loadFromConfiguredBackend(category)
    if (backendPayload?.events?.length) {
      return {
        events: backendPayload.events,
        categoryStats: backendPayload.categoryStats?.length
          ? backendPayload.categoryStats
          : buildStats(backendPayload.events, 'category', '全球'),
        regionStats: backendPayload.regionStats?.length
          ? backendPayload.regionStats
          : buildStats(backendPayload.events, 'region', '未知地区'),
        trends: backendPayload.trends || (backendPayload.categoryStats || []).map((item) => ({
          name: item.name,
          value: item.percentage
        })),
        sourceMode: backendPayload.meta?.sourceMode || backendPayload.sourceMode || 'backend',
        meta: backendPayload.meta || {
          sourceMode: backendPayload.sourceMode || 'backend',
          sources: ['新闻接口'],
          updatedAt: new Date().toISOString(),
          cached: false,
          cacheMinutes: 10,
          dataStatus: 'normal'
        }
      }
    }
  } catch (error) {
    console.warn('[Earth Online] backend news fallback:', error)
    if (dataSource === 'api') {
      return {
        ...buildFallbackPayload(),
        meta: {
          ...buildFallbackPayload().meta,
          dataStatus: 'error',
          message: `后端接口连接失败，已临时降级为 Mock 数据：${error.message}`
        }
      }
    }
  }

  return buildFallbackPayload()
}
import { request } from './httpClient'
