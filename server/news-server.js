import http from 'node:http'
import { readFileSync, existsSync } from 'node:fs'
import { resolve } from 'node:path'

const PORT = Number(process.env.NEWS_SERVER_PORT || 8787)
const CACHE_TTL = 5 * 60 * 1000
const GDELT_DOC_API = 'https://api.gdeltproject.org/api/v2/doc/doc'
const NEWS_API_TOP_HEADLINES = 'https://newsapi.org/v2/top-headlines'

const CATEGORY_QUERIES = {
  all: 'climate OR earthquake OR space OR technology OR health OR economy',
  climate: 'climate OR flood OR wildfire OR heatwave OR biodiversity',
  science: 'space OR astronomy OR research OR discovery OR technology',
  society: 'health OR education OR culture OR migration OR city',
  economy: 'economy OR market OR energy OR supply chain OR trade'
}

const CATEGORY_LABELS = {
  all: '全球',
  climate: '气候',
  science: '科技',
  society: '社会',
  economy: '经济'
}

const CATEGORY_IMAGES = {
  气候: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&q=80&w=900&h=520',
  科技: 'https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?auto=format&fit=crop&q=80&w=900&h=520',
  社会: 'https://images.unsplash.com/photo-1494526585095-c41746248156?auto=format&fit=crop&q=80&w=900&h=520',
  经济: 'https://images.unsplash.com/photo-1473341304170-971dccb5ac1e?auto=format&fit=crop&q=80&w=900&h=520',
  全球: 'https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&q=80&w=900&h=520'
}

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
    image: CATEGORY_IMAGES['气候'],
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
    image: CATEGORY_IMAGES['科技'],
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
    image: CATEGORY_IMAGES['社会'],
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
    image: CATEGORY_IMAGES['经济'],
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

const cache = new Map()

const loadEnvFile = (fileName) => {
  const filePath = resolve(process.cwd(), fileName)
  if (!existsSync(filePath)) return

  const lines = readFileSync(filePath, 'utf8').split(/\r?\n/)
  lines.forEach((line) => {
    const trimmed = line.trim()
    if (!trimmed || trimmed.startsWith('#')) return

    const separatorIndex = trimmed.indexOf('=')
    if (separatorIndex === -1) return

    const key = trimmed.slice(0, separatorIndex).trim()
    const value = trimmed.slice(separatorIndex + 1).trim().replace(/^['"]|['"]$/g, '')
    if (key && process.env[key] === undefined) {
      process.env[key] = value
    }
  })
}

loadEnvFile('.env')
loadEnvFile('.env.local')

const sendJson = (res, statusCode, payload) => {
  res.writeHead(statusCode, {
    'Content-Type': 'application/json; charset=utf-8',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type'
  })
  res.end(JSON.stringify(payload))
}

const readJson = async (url) => {
  const response = await fetch(url, {
    headers: {
      'User-Agent': 'EarthOnline/1.0'
    }
  })

  if (!response.ok) {
    throw new Error(`News request failed: ${response.status}`)
  }

  return response.json()
}

const trimText = (value, max = 110) => {
  if (!value) return ''
  return value.length > max ? `${value.slice(0, max)}...` : value
}

const hashText = (value) => {
  let hash = 0
  for (let i = 0; i < value.length; i += 1) {
    hash = ((hash << 5) - hash) + value.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash)
}

const normalizeDate = (value) => {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? new Date().toISOString() : date.toISOString()
}

const inferCategory = (title, fallbackCategory = 'all') => {
  const text = title.toLowerCase()
  if (/climate|flood|wildfire|storm|heat|carbon|biodiversity/.test(text)) return '气候'
  if (/space|technology|science|research|ai|nasa|discovery/.test(text)) return '科技'
  if (/market|economy|energy|trade|supply|business/.test(text)) return '经济'
  if (/health|education|culture|city|society|migration/.test(text)) return '社会'
  return CATEGORY_LABELS[fallbackCategory] || '全球'
}

const normalizeGdeltArticles = (articles = [], category) => {
  return articles
    .filter((item) => item.title && item.url)
    .map((item, index) => {
      const inferredCategory = inferCategory(item.title, category)
      return {
        id: `gdelt-${hashText(item.url || item.title)}-${index}`,
        title: item.title,
        summary: trimText(item.seendate ? `${item.domain || '全球媒体'} 在 ${item.seendate} 报道了这一事件。` : item.title),
        source: item.domain || 'GDELT',
        publishedAt: normalizeDate(item.seendate),
        category: inferredCategory,
        region: item.sourcecountry || '全球',
        url: item.url,
        image: item.socialimage || CATEGORY_IMAGES[inferredCategory] || CATEGORY_IMAGES['全球'],
        importance: Math.max(62, 95 - index * 4)
      }
    })
}

const normalizeNewsApiArticles = (articles = [], category) => {
  return articles
    .filter((item) => item.title && item.url)
    .map((item, index) => {
      const inferredCategory = inferCategory(item.title, category)
      return {
        id: `newsapi-${hashText(item.url || item.title)}-${index}`,
        title: item.title,
        summary: trimText(item.description || item.content || item.title),
        source: item.source?.name || 'NewsAPI',
        publishedAt: normalizeDate(item.publishedAt),
        category: inferredCategory,
        region: '全球',
        url: item.url,
        image: item.urlToImage || CATEGORY_IMAGES[inferredCategory] || CATEGORY_IMAGES['全球'],
        importance: Math.max(58, 90 - index * 5)
      }
    })
}

const dedupeNews = (items) => {
  const seen = new Set()
  return items.filter((item) => {
    const key = item.title.toLowerCase().replace(/\W/g, '').slice(0, 80)
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

const buildGdeltUrl = (category) => {
  const query = CATEGORY_QUERIES[category] || CATEGORY_QUERIES.all
  const params = new URLSearchParams({
    query,
    mode: 'ArtList',
    format: 'json',
    maxrecords: '12',
    sort: 'hybridrel',
    timespan: '24h'
  })
  return `${GDELT_DOC_API}?${params.toString()}`
}

const buildNewsApiUrl = (category) => {
  const apiKey = process.env.NEWS_API_KEY
  if (!apiKey) return ''

  const params = new URLSearchParams({
    language: 'en',
    pageSize: '8',
    apiKey
  })

  if (category !== 'all') {
    params.set('q', CATEGORY_QUERIES[category].split(' OR ')[0])
  }

  return `${NEWS_API_TOP_HEADLINES}?${params.toString()}`
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

  return Array.from(counts.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([name], index) => ({
      name,
      value: Math.max(58, 96 - index * 7),
      type: 'global'
    }))
}

const fetchNews = async (category) => {
  const cacheKey = category
  const cached = cache.get(cacheKey)

  if (cached && Date.now() - cached.createdAt < CACHE_TTL) {
    return {
      ...cached.payload,
      cached: true
    }
  }

  const requests = [
    readJson(buildGdeltUrl(category)).then((data) => normalizeGdeltArticles(data.articles, category))
  ]

  const newsApiUrl = buildNewsApiUrl(category)
  if (newsApiUrl) {
    requests.push(readJson(newsApiUrl).then((data) => normalizeNewsApiArticles(data.articles, category)))
  }

  const settled = await Promise.allSettled(requests)
  const events = dedupeNews(settled.flatMap((result) => result.status === 'fulfilled' ? result.value : []))
    .sort((a, b) => b.importance - a.importance)

  const payload = events.length
    ? {
        events,
        trends: buildTrends(events),
        sourceMode: newsApiUrl ? 'gdelt-newsapi' : 'gdelt',
        updatedAt: new Date().toISOString()
      }
    : {
        events: fallbackNews,
        trends: buildTrends(fallbackNews),
        sourceMode: 'fallback',
        updatedAt: new Date().toISOString()
      }

  cache.set(cacheKey, {
    createdAt: Date.now(),
    payload
  })

  return payload
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') {
    sendJson(res, 204, {})
    return
  }

  const url = new URL(req.url, `http://${req.headers.host}`)

  if (req.method === 'GET' && url.pathname === '/api/health') {
    sendJson(res, 200, {
      ok: true,
      service: 'earth-online-news',
      newsApiConfigured: Boolean(process.env.NEWS_API_KEY)
    })
    return
  }

  if (req.method === 'GET' && url.pathname === '/api/news') {
    const category = url.searchParams.get('category') || 'all'

    try {
      sendJson(res, 200, await fetchNews(category))
    } catch (error) {
      console.error('[news-server] request failed:', error)
      sendJson(res, 200, {
        events: fallbackNews,
        trends: buildTrends(fallbackNews),
        sourceMode: 'fallback',
        updatedAt: new Date().toISOString(),
        error: '新闻源暂时不可用，已返回演示数据。'
      })
    }
    return
  }

  sendJson(res, 404, {
    error: 'Not found'
  })
})

server.listen(PORT, '127.0.0.1', () => {
  console.log(`[news-server] http://127.0.0.1:${PORT}`)
  console.log(`[news-server] NEWS_API_KEY ${process.env.NEWS_API_KEY ? 'loaded' : 'not configured, using GDELT only'}`)
})
