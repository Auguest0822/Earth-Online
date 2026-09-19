import { API_BASE_URL } from '../config'
import { ApiError, clearAccessToken, getAccessToken } from '../httpClient'

/**
 * 服务端发送事件（SSE）的一帧解析成 { event, data }。
 *
 * SSE 的帧格式长这样（帧与帧之间用一个空行分隔）：
 *
 *   event: delta
 *   data: {"text":"根据"}
 *
 * 所以我们要做的就是把 event: 和 data: 两行读出来，其余行忽略。
 */
const parseFrame = raw => {
  let event = 'message'
  const dataLines = []
  raw.split(/\r?\n/).forEach(line => {
    if (line.startsWith('event:')) event = line.slice(6).trim()
    else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^ /, ''))
  })
  return { event, data: dataLines.join('\n') }
}

/**
 * 流式提问：AI 每生成一小段，就通过 handlers.delta 回调一次。
 *
 * 为什么不用浏览器原生的 EventSource？
 * 1. EventSource 只能发 GET，带不了请求体（我们的 year + question 是 JSON body）；
 * 2. EventSource 不能自定义请求头，带不了 Authorization；
 * 3. 把 token 放到 URL 查询参数里会写进服务器访问日志和浏览器历史，属于泄密。
 * 所以用 fetch + ReadableStream 手动解析 SSE。
 *
 * @param handlers { meta, delta, done, error } 四个回调，按事件名分发
 * @param signal   AbortSignal，用于组件卸载或用户重新提问时中断
 */
export const streamYearlyMemory = async (year, question, handlers, signal) => {
  const response = await fetch(`${API_BASE_URL}/ai/yearly-memory/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      Authorization: `Bearer ${getAccessToken()}`
    },
    body: JSON.stringify({ year, question }),
    signal
  })

  // 第一个字发出去之前，后端仍然可以返回普通 JSON 错误（参数错、未登录、没有记录……）
  if (!response.ok) {
    const contentType = response.headers.get('content-type') || ''
    const payload = contentType.includes('application/json') ? await response.json() : null
    // 流式接口绕过了 httpClient，登录失效要自己处理，否则用户会卡在没有提示的页面
    if (response.status === 401) {
      clearAccessToken()
      window.location.replace(`/login?expired=1&redirect=${encodeURIComponent(window.location.pathname)}`)
    }
    throw new ApiError(payload?.message || `请求失败（${response.status}）`, response.status, payload)
  }

  const reader = response.body?.getReader()
  if (!reader) throw new ApiError('当前浏览器不支持流式响应', 0, null)

  const decoder = new TextDecoder('utf-8')
  let buffer = ''
  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    // 一个 SSE 帧以空行结束，可能收到不完整的帧，剩下的留在 buffer 里等下一次读取
    let boundary = buffer.search(/\r?\n\r?\n/)
    while (boundary >= 0) {
      const frame = buffer.slice(0, boundary)
      const separatorLength = buffer[boundary] === '\r' ? 4 : 2
      buffer = buffer.slice(boundary + separatorLength)
      if (frame.trim()) {
        const { event, data } = parseFrame(frame)
        if (data && handlers[event]) handlers[event](JSON.parse(data))
      }
      boundary = buffer.search(/\r?\n\r?\n/)
    }
  }
}
