/**
 * 临时验证脚本（可删除）：确认 WebSocket/STOMP 能真正把聊天消息推给接收方。
 * 用法：node output/chat-ws-verify.mjs   （后端需已在 8080 运行，MySQL 可用）
 * 注意：脚本会注册 wsv* 开头的临时测试账号并互加好友；
 *       验证结束后可用 SQL 删除：
 *       delete cm from chat_messages cm join users u on (cm.sender_id=u.id or cm.receiver_id=u.id) where u.username like 'wsv%';
 *       delete fr from friend_requests fr join users u on (fr.sender_id=u.id or fr.receiver_id=u.id) where u.username like 'wsv%';
 *       delete from users where username like 'wsv%';
 */
const BASE = process.env.CHAT_TEST_BASE || 'http://localhost:8080'
const WS_URL = process.env.CHAT_TEST_WS || 'ws://localhost:8080/ws/chat'
const STAMP = Date.now().toString().slice(-6)

const api = async (path, { method = 'GET', body, token } = {}) => {
  const response = await fetch(`${BASE}${path}`, {
    method,
    headers: {
      Accept: 'application/json',
      ...(body ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: body ? JSON.stringify(body) : undefined
  })
  const text = await response.text()
  let data = null
  try {
    data = text ? JSON.parse(text) : null
  } catch {
    data = text
  }
  return { status: response.status, data }
}

const parseFrame = raw => {
  const separator = raw.indexOf('\n\n')
  const headLines = (separator === -1 ? raw : raw.slice(0, separator)).split('\n')
  const command = headLines.shift()
  const headers = {}
  headLines.forEach(line => {
    const index = line.indexOf(':')
    if (index > 0) headers[line.slice(0, index)] = line.slice(index + 1)
  })
  return { command, headers, body: separator === -1 ? '' : raw.slice(separator + 2) }
}

const openStompSession = (label, token, destinations) => {
  const received = []
  const errors = []
  const queued = []
  const state = { connected: false, failed: null }

  const ws = new WebSocket(WS_URL)
  let buffer = ''

  const handleFrame = frame => {
    if (frame.command === 'CONNECTED') {
      state.connected = true
      destinations.forEach((destination, index) => {
        ws.send(`SUBSCRIBE\nid:sub-${index}\ndestination:${destination}\n\n\u0000`)
      })
      return
    }
    if (frame.command === 'MESSAGE') {
      let payload = frame.body
      try {
        payload = JSON.parse(frame.body)
      } catch {
        /* 保留原始文本 */
      }
      received.push({ destination: frame.headers.destination, payload })
      return
    }
    if (frame.command === 'ERROR') {
      errors.push(frame.headers.message || frame.body)
      console.error(`[${label}] STOMP ERROR:`, frame.headers.message || frame.body)
    }
  }

  const ready = new Promise((resolve, reject) => {
    const timer = setTimeout(() => reject(new Error(`[${label}] STOMP 连接超时`)), 8000)
    ws.onopen = () => {
      ws.send(`CONNECT\naccept-version:1.2\nheart-beat:0,0\nAuthorization:Bearer ${token}\n\n\u0000`)
    }
    ws.onmessage = event => {
      buffer += typeof event.data === 'string' ? event.data : ''
      let end = buffer.indexOf('\u0000')
      while (end !== -1) {
        const raw = buffer.slice(0, end)
        buffer = buffer.slice(end + 1)
        if (raw.trim()) handleFrame(parseFrame(raw))
        end = buffer.indexOf('\u0000')
      }
      if (state.connected) {
        clearTimeout(timer)
        resolve()
      }
    }
    ws.onerror = () => {
      clearTimeout(timer)
      reject(new Error(`[${label}] WebSocket 握手失败（检查 Origin 白名单与后端端口）`))
    }
    ws.onclose = event => {
      if (!state.connected) {
        clearTimeout(timer)
        reject(new Error(`[${label}] WebSocket 关闭，code=${event.code}`))
      }
    }
  })

  return {
    label,
    ready,
    received,
    errors,
    send: (destination, body) => ws.send(`SEND\ndestination:${destination}\ncontent-type:application/json\n\n${JSON.stringify(body)}\u0000`),
    waitFor: (predicate, timeoutMs = 5000) => new Promise((resolve, reject) => {
      const found = received.find(predicate)
      if (found) return resolve(found)
      const started = Date.now()
      const timer = setInterval(() => {
        const match = received.find(predicate)
        if (match) {
          clearInterval(timer)
          resolve(match)
        } else if (Date.now() - started > timeoutMs) {
          clearInterval(timer)
          reject(new Error(`[${label}] ${timeoutMs}ms 内没有等到期望的推送帧`))
        }
      }, 50)
    }),
    close: () => ws.close()
  }
}

const registerOrLogin = async (username, password, email) => {
  const registered = await api('/api/auth/register', {
    method: 'POST',
    body: { username, password, email }
  })
  if (registered.status === 200 || registered.status === 201) {
    const login = await api('/api/auth/login', { method: 'POST', body: { username, password } })
    return { user: login.data.user, token: login.data.accessToken }
  }
  const login = await api('/api/auth/login', { method: 'POST', body: { username, password } })
  if (login.status !== 200) {
    throw new Error(`无法登录 ${username}: ${login.status} ${JSON.stringify(login.data)}`)
  }
  return { user: login.data.user, token: login.data.accessToken }
}

const main = async () => {
  const failures = []
  const userA = `wsv${STAMP}a`
  const userB = `wsv${STAMP}b`
  const password = 'WsCheck12345'

  const a = await registerOrLogin(userA, password, `a${STAMP}@x.io`)
  const b = await registerOrLogin(userB, password, `b${STAMP}@x.io`)
  console.log(`账号 A=${a.user.username}/${a.user.publicId}  B=${b.user.username}/${b.user.publicId}`)

  const request = await api('/api/friends/requests', {
    method: 'POST',
    token: a.token,
    body: { publicId: b.user.publicId }
  })
  if (request.status !== 200 && request.status !== 201) {
    throw new Error(`好友申请失败: ${request.status} ${JSON.stringify(request.data)}`)
  }
  const pending = await api('/api/friends/requests/received', { token: b.token })
  const target = (pending.data || []).find(item => item.user?.publicId === a.user.publicId)
  if (!target) throw new Error('B 没有收到好友申请')
  const accepted = await api(`/api/friends/requests/${target.id}/accept`, { method: 'POST', token: b.token })
  if (accepted.status !== 200) throw new Error(`接受好友失败: ${accepted.status}`)
  console.log('已成为好友，开始 WebSocket 实时推送验证')

  const queue = '/user/queue/chat/messages'
  const sessionA = openStompSession('A', a.token, [queue, '/user/queue/chat/read-receipts'])
  const sessionB = openStompSession('B', b.token, [queue, '/user/queue/chat/read-receipts'])
  await Promise.all([sessionA.ready, sessionB.ready])
  console.log('两端 STOMP CONNECTED 并订阅 /user/queue/chat/messages')

  const content = `实时推送验证 ${STAMP}`
  const sent = await api('/api/chat/messages', {
    method: 'POST',
    token: a.token,
    body: { recipientPublicId: b.user.publicId, content, clientMessageId: `verify-${STAMP}`, messageType: 'TEXT', imageObjectKey: null }
  })
  if (sent.status !== 200) throw new Error(`REST 发送失败: ${sent.status} ${JSON.stringify(sent.data)}`)
  console.log('A 已通过 REST 发送消息，等待 B 的 WebSocket 推送…')

  try {
    const pushed = await sessionB.waitFor(item => item.payload?.content === content, 5000)
    const mine = pushed.payload?.mine
    if (mine !== false) failures.push(`B 收到的 mine 字段应为 false，实际 ${mine}`)
    console.log('✅ B 通过 WebSocket 收到消息:', JSON.stringify({ id: pushed.payload.id, content: pushed.payload.content, mine }))
  } catch (error) {
    failures.push(error.message)
    console.error('❌ B 没有收到实时推送 —— 实时通道仍然不通')
  }

  // 已读回执同样走私人队列，一并验证。
  const readResponse = await api(`/api/chat/messages/read?friendPublicId=${a.user.publicId}`, { method: 'POST', token: b.token })
  if (readResponse.status === 200 && readResponse.data?.lastReadMessageId) {
    try {
      const receipt = await sessionA.waitFor(
        item => item.destination?.includes('read-receipts') && item.payload?.readerPublicId === b.user.publicId,
        5000
      )
      console.log('✅ A 通过 WebSocket 收到已读回执:', JSON.stringify(receipt.payload))
    } catch (error) {
      failures.push(error.message)
      console.error('❌ 已读回执没有实时推送到 A')
    }
  }

  const minePushedBack = sessionA.received.some(item => item.payload?.content === content && item.payload?.mine === undefined)
  if (minePushedBack) failures.push('A 收到了不该由 REST 发送方接收的重复帧')

  // STOMP SEND 路径（/app/chat/messages）依赖 JWT 绑定出的 Principal，一并验证双向推送。
  const reply = `WS 发送验证 ${STAMP}`
  sessionB.send('/app/chat/messages', {
    recipientPublicId: a.user.publicId,
    content: reply,
    clientMessageId: `verify-ws-${STAMP}`,
    messageType: 'TEXT',
    imageObjectKey: null
  })
  try {
    const toA = await sessionA.waitFor(item => item.payload?.content === reply, 5000)
    if (toA.payload?.mine !== false) failures.push(`A 收到的 mine 字段应为 false，实际 ${toA.payload?.mine}`)
    const echo = await sessionB.waitFor(item => item.payload?.content === reply, 5000)
    if (echo.payload?.mine !== true) failures.push(`B 自己回显的 mine 字段应为 true，实际 ${echo.payload?.mine}`)
    console.log('✅ /app/chat/messages 路径：A 收到消息、B 收到自己的回显（Principal 绑定正确）')
  } catch (error) {
    failures.push(error.message)
    console.error('❌ STOMP SEND 路径失败（Principal 可能未绑定）')
  }

  sessionA.close()
  sessionB.close()

  console.log('---')
  if (failures.length) {
    console.error('验证失败：')
    failures.forEach(item => console.error(' -', item))
    console.log(JSON.stringify({ userA, userB }))
    process.exit(1)
  }
  console.log('验证通过：WebSocket 实时推送与已读回执均正常工作')
  console.log(JSON.stringify({ userA, userB }))
}

main().catch(error => {
  console.error('验证脚本异常:', error.message)
  process.exit(2)
})
