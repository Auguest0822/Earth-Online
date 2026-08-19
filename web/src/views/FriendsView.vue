<template>
  <main class="friends-view">
    <aside class="friend-sidebar">
      <header class="friend-sidebar__header">
        <p class="eyebrow">EARTH FRIENDS</p>
        <h2>朋友</h2>
        <p class="friend-count">{{ friends.length }} 位好友</p>
      </header>

      <div class="friend-search">
        <el-input
          v-model="searchKeyword"
          maxlength="10"
          placeholder="根据 10 位地球 ID 搜索"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button class="search-button" :icon="Search" circle @click="handleSearch" />
      </div>
      <p v-if="searchMessage" class="search-message">{{ searchMessage }}</p>

      <section v-if="searchResult" class="search-result" aria-label="搜索结果">
        <div class="search-result__profile">
          <Avatar :user="searchResult" />
          <div>
            <strong>{{ searchResult.username }}</strong>
            <span>ID · {{ searchResult.publicId }}</span>
            <p>{{ searchResult.slogan || '在地球上认真生活中。' }}</p>
          </div>
        </div>
        <el-button
          size="small"
          :disabled="searchResult.relationStatus !== 'NONE' || sendingRequest"
          @click="handleSendRequest"
        >
          {{ friendActionText(searchResult.relationStatus) }}
        </el-button>
      </section>

      <section v-if="receivedRequests.length || loadingReceived" class="received-requests" aria-label="收到的好友申请">
        <div class="received-requests__header"><strong>收到的好友申请</strong><span>{{ receivedRequests.length }}</span></div>
        <article v-for="request in receivedRequests" :key="request.id" class="received-request">
          <Avatar :user="request.user" />
          <div class="received-request__main">
            <strong>{{ request.user.username }}</strong><span>ID · {{ request.user.publicId }}</span>
            <p>{{ request.user.slogan || '邀请你成为好友' }}</p>
            <div class="received-request__actions">
              <el-button size="small" type="primary" :loading="processingRequestId === request.id" @click="handleAcceptRequest(request)">接受</el-button>
              <el-button size="small" :disabled="processingRequestId === request.id" @click="handleRejectRequest(request)">拒绝</el-button>
            </div>
          </div>
        </article>
      </section>

      <section class="friend-list" aria-label="好友列表">
        <button
          v-for="friend in friends"
          :key="friend.id"
          type="button"
          class="friend-item"
          :class="{ active: selectedFriend?.id === friend.id }"
          @click="selectedFriend = friend"
        >
          <Avatar :user="friend" />
          <span class="friend-item__main">
            <span class="friend-item__top">
              <strong>{{ friend.name }}</strong><time>{{ friend.time }}</time>
            </span>
            <span class="friend-public-id">ID · {{ friend.publicId }}</span>
            <span class="friend-item__bottom">
              <span class="friend-item__message">{{ friend.preview }}</span>
              <span v-if="friend.unreadCount" class="friend-item__unread" :aria-label="`${friend.unreadCount} 条未读消息`">
                {{ friend.unreadCount > 99 ? '99+' : friend.unreadCount }}
              </span>
            </span>
          </span>
        </button>
        <div v-if="loadingFriends" class="friend-list__empty"><p>正在加载好友列表…</p></div>
        <div v-else-if="!friends.length" class="friend-list__empty">
          <el-icon><UserFilled /></el-icon>
          <p>还没有好友，试试搜索对方的地球 ID 吧</p>
        </div>
      </section>
    </aside>

    <section class="chat-panel">
      <template v-if="selectedFriend">
        <header class="chat-header">
          <div class="chat-user">
            <Avatar :user="selectedFriend" size="large" />
            <div>
              <h3>{{ selectedFriend.name }}</h3>
              <p><span class="online-dot" :class="{ offline: !chatConnected }"></span> {{ chatConnected ? '实时连接' : '正在重连…' }}</p>
            </div>
          </div>
          <el-button text circle :icon="MoreFilled" title="更多操作" />
        </header>

        <div ref="chatContent" class="chat-content">
          <div v-if="loadingMessages" class="chat-placeholder"><p>正在加载聊天记录…</p></div>
          <div v-else-if="!messages.length" class="chat-placeholder">
            <el-icon><ChatDotRound /></el-icon>
            <p>还没有聊天记录，和好友打个招呼吧。</p>
          </div>
          <div v-else class="message-list">
            <template v-for="(message, index) in messages" :key="message.id">
              <div v-if="shouldShowMessageTime(message, messages[index - 1])" class="message-time-divider">
                {{ formatMessageTime(message.createdAt) }}
              </div>
              <article class="message-row" :class="{ mine: message.mine }">
                <Avatar v-if="!message.mine" :user="message.sender" size="small" />
                <div class="message-body">
                  <div class="message-bubble"><p>{{ message.content }}</p></div>
                  <button
                    v-if="message.mine && message.deliveryStatus === 'failed'"
                    type="button"
                    class="message-retry"
                    @click="retryMessage(message)"
                  >发送失败，点击重试</button>
                  <span v-else-if="message.mine" class="message-read-state">
                    {{ message.deliveryStatus === 'sending' ? '发送中' : (message.readAt ? '已读' : '未读') }}
                  </span>
                </div>
              </article>
            </template>
          </div>
        </div>

        <footer class="chat-editor">
          <div class="editor-tools"><el-icon><ChatDotRound /></el-icon><el-icon><Picture /></el-icon></div>
          <el-input v-model="draft" type="textarea" :rows="3" resize="none" maxlength="500" show-word-limit placeholder="输入想对好友说的话…" @keyup.enter.exact.prevent="handleSendMessage" />
          <div class="editor-footer">
            <span>按 Enter 发送，Shift + Enter 换行。</span>
            <el-button type="primary" :loading="sendingMessage" :disabled="!draft.trim()" @click="handleSendMessage">发送</el-button>
          </div>
        </footer>
      </template>

      <div v-else class="chat-empty">
        <el-icon><UserFilled /></el-icon>
        <h3>选择一位朋友</h3>
        <p>从左侧好友列表中选择一位朋友，开始你们的地球对话。</p>
      </div>
    </section>
  </main>
</template>

<script setup>
import { defineComponent, h, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, MoreFilled, Picture, Search, UserFilled } from '@element-plus/icons-vue'
import { acceptFriendRequest, listFriends, listReceivedFriendRequests, rejectFriendRequest, searchFriendByPublicId, sendFriendRequest } from '@/api/modules/friends'
import { listChatMessages, listChatUnreadCounts, markChatMessagesRead, sendChatMessage } from '@/api/modules/chat'
import { connectChatSocket, disconnectChatSocket, isChatSocketConnected, sendChatSocketMessage } from '@/api/chatSocket'

const avatarColors = [
  'linear-gradient(135deg, #92d7b0, #5ea979)',
  'linear-gradient(135deg, #9bbaf2, #7589ce)',
  'linear-gradient(135deg, #ffcf85, #e58a75)',
  'linear-gradient(135deg, #c6a9ec, #8f79c9)'
]

const Avatar = defineComponent({
  props: {
    user: { type: Object, required: true },
    size: { type: String, default: '' }
  },
  setup(props) {
    const className = () => ['friend-avatar', props.size].filter(Boolean).join(' ')
    const background = () => avatarColors[String(props.user.publicId || props.user.id || '').slice(-1) % avatarColors.length]
    const size = () => props.size === 'large' ? 46 : (props.size === 'small' ? 30 : 42)
    return () => h('span', {
      class: className(),
      style: {
        background: background(),
        display: 'grid',
        flex: `0 0 ${size()}px`,
        width: `${size()}px`,
        height: `${size()}px`,
        minWidth: `${size()}px`,
        minHeight: `${size()}px`,
        maxWidth: `${size()}px`,
        maxHeight: `${size()}px`,
        overflow: 'hidden',
        placeItems: 'center',
        border: '2px solid rgba(255, 255, 255, .85)',
        borderRadius: '50%',
        boxShadow: '0 2px 8px rgba(62, 126, 85, .15)',
        lineHeight: '1'
      }
    }, [
      props.user.avatar
        ? h('img', {
          src: props.user.avatar,
          alt: `${props.user.username || props.user.name} 的头像`,
          style: {
            display: 'block',
            width: '100%',
            height: '100%',
            maxWidth: '100%',
            maxHeight: '100%',
            objectFit: 'cover'
          }
        })
        : (props.user.username || props.user.name || '?').slice(0, 1)
    ])
  }
})

const friends = ref([])
const searchKeyword = ref('')
const searchMessage = ref('')
const searchResult = ref(null)
const selectedFriend = ref(null)
const draft = ref('')
const loadingFriends = ref(false)
const sendingRequest = ref(false)
const receivedRequests = ref([])
const loadingReceived = ref(false)
const processingRequestId = ref(null)
const messages = ref([])
const loadingMessages = ref(false)
const sendingMessage = ref(false)
const chatConnected = ref(false)
const chatContent = ref(null)
const messageTimeouts = new Map()
const receivedMessageIds = new Set()
const MESSAGE_TIMEOUT_MS = 10000

const formatFriendsSince = value => {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? ''
    : new Intl.DateTimeFormat('zh-CN', { month: 'numeric', day: 'numeric' }).format(date)
}

const mapFriend = friend => ({
  id: friend.friendshipId,
  name: friend.username,
  publicId: friend.publicId,
  avatar: friend.avatar,
  preview: friend.slogan || '已经成为好友，开始你们的地球对话吧。',
  time: formatFriendsSince(friend.friendsSince),
  unreadCount: 0
})

const friendActionText = status => ({
  NONE: '添加好友',
  PENDING_SENT: '等待确认',
  PENDING_RECEIVED: '对方已申请',
  FRIENDS: '已是好友'
}[status] || '添加好友')

const loadFriends = async () => {
  loadingFriends.value = true
  try {
    const [result, unreadResult] = await Promise.all([listFriends(), listChatUnreadCounts()])
    const list = Array.isArray(result) ? result : (result?.content || result?.items || [])
    const unreadCounts = new Map(
      (Array.isArray(unreadResult) ? unreadResult : []).map(item => [item.friendPublicId, Number(item.unreadCount) || 0])
    )
    friends.value = list.map(friend => ({
      ...mapFriend(friend),
      unreadCount: unreadCounts.get(friend.publicId) || 0
    }))
    if (!selectedFriend.value || !friends.value.some(friend => friend.id === selectedFriend.value.id)) {
      selectedFriend.value = friends.value[0] || null
    }
  } catch (error) {
    ElMessage.error(error.message || '好友列表加载失败')
  } finally {
    loadingFriends.value = false
  }
}

const loadReceivedRequests = async () => {
  loadingReceived.value = true
  try {
    const result = await listReceivedFriendRequests()
    receivedRequests.value = Array.isArray(result) ? result : (result?.content || result?.items || result?.value || [])
  } catch (error) {
    ElMessage.error(error.message || '收到的好友申请加载失败')
  } finally {
    loadingReceived.value = false
  }
}

const handleAcceptRequest = async request => {
  processingRequestId.value = request.id
  try {
    await acceptFriendRequest(request.id)
    receivedRequests.value = receivedRequests.value.filter(item => item.id !== request.id)
    await loadFriends()
    ElMessage.success('已成为好友')
  } catch (error) {
    ElMessage.error(error.message || '接受好友申请失败')
  } finally {
    processingRequestId.value = null
  }
}

const handleRejectRequest = async request => {
  processingRequestId.value = request.id
  try {
    await rejectFriendRequest(request.id)
    receivedRequests.value = receivedRequests.value.filter(item => item.id !== request.id)
    ElMessage.success('已拒绝该好友申请')
  } catch (error) {
    ElMessage.error(error.message || '拒绝好友申请失败')
  } finally {
    processingRequestId.value = null
  }
}

const formatMessageTime = value => {
  if (!value) return ''
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : new Intl.DateTimeFormat('zh-CN', { hour: '2-digit', minute: '2-digit' }).format(date)
}

const shouldShowMessageTime = (message, previousMessage) => {
  if (!previousMessage) return true
  const currentTime = new Date(message.createdAt).getTime()
  const previousTime = new Date(previousMessage.createdAt).getTime()
  // Invalid dates should still show a separator rather than hiding useful context.
  if (Number.isNaN(currentTime) || Number.isNaN(previousTime)) return true
  return currentTime - previousTime >= 5 * 60 * 1000
}

const scrollMessagesToBottom = async () => {
  await nextTick()
  if (chatContent.value) chatContent.value.scrollTop = chatContent.value.scrollHeight
}

const isSameMessageList = (current, next) => (
  current.length === next.length
  && current.every((message, index) => String(message.id) === String(next[index]?.id))
)

const isNearMessagesBottom = () => {
  if (!chatContent.value) return true
  const { scrollHeight, scrollTop, clientHeight } = chatContent.value
  return scrollHeight - scrollTop - clientHeight < 80
}

const loadMessages = async (friend, { silent = false } = {}) => {
  if (!friend?.publicId) {
    messages.value = []
    return
  }
  if (!silent) loadingMessages.value = true
  const friendPublicId = friend.publicId
  const shouldStickToBottom = !silent || isNearMessagesBottom()
  try {
    const result = await listChatMessages(friend.publicId)
    // Prevent a slower request for a previously selected friend from replacing
    // the active conversation after the user switches to someone else.
    if (selectedFriend.value?.publicId === friendPublicId) {
      const serverMessages = Array.isArray(result) ? result : (result?.content || result?.items || [])
      const serverClientIds = new Set(serverMessages.map(message => message.clientMessageId).filter(Boolean))
      const localPending = messages.value.filter(message => (
        message.clientMessageId && !serverClientIds.has(message.clientMessageId)
      ))
      const nextMessages = [...serverMessages, ...localPending]
      if (!isSameMessageList(messages.value, nextMessages)) {
        messages.value = nextMessages
        if (shouldStickToBottom) await scrollMessagesToBottom()
      }
    }
  } catch (error) {
    // 新增聊天模块后若浏览器仍连接到旧的后端进程，先维持空会话界面，避免点击好友时反复报错。
    if (error.status === 404) {
      return
    }
    ElMessage.error(error.message || '聊天记录加载失败')
  } finally {
    if (!silent) loadingMessages.value = false
  }
}

const appendMessage = async message => {
  if (!message || messages.value.some(item => String(item.id) === String(message.id))) return
  messages.value.push(message)
  await scrollMessagesToBottom()
}

const handleIncomingMessage = message => {
  if (!message?.mine && receivedMessageIds.has(String(message.id))) return
  if (!message?.mine) receivedMessageIds.add(String(message.id))
  const friendPublicId = message?.mine ? message?.receiver?.publicId : message?.sender?.publicId
  const friend = friends.value.find(item => item.publicId === friendPublicId)
  if (friend) {
    friend.preview = message.content
    friend.time = formatMessageTime(message.createdAt)
    if (!message.mine && selectedFriend.value?.publicId !== friendPublicId) {
      friend.unreadCount += 1
    }
  }
  if (selectedFriend.value?.publicId === friendPublicId) {
    if (message.mine) {
      const pendingIndex = messages.value.findIndex(item => (
        item.clientMessageId && item.clientMessageId === message.clientMessageId
      ))
      if (message.clientMessageId) clearMessageTimeout(message.clientMessageId)
      if (pendingIndex >= 0) messages.value.splice(pendingIndex, 1, { ...message, deliveryStatus: 'sent' })
      else appendMessage(message)
    } else {
      appendMessage(message)
    }
  }
  if (!message.mine && selectedFriend.value?.publicId === friendPublicId) {
    markChatMessagesRead(friendPublicId).catch(error => {
      console.error('标记聊天消息已读失败', error)
    })
  }
}

const clearMessageTimeout = clientMessageId => {
  const timeout = messageTimeouts.get(clientMessageId)
  if (timeout) window.clearTimeout(timeout)
  messageTimeouts.delete(clientMessageId)
}

const armMessageTimeout = clientMessageId => {
  clearMessageTimeout(clientMessageId)
  messageTimeouts.set(clientMessageId, window.setTimeout(() => {
    const message = messages.value.find(item => item.clientMessageId === clientMessageId)
    if (message?.deliveryStatus === 'sending') message.deliveryStatus = 'failed'
    messageTimeouts.delete(clientMessageId)
  }, MESSAGE_TIMEOUT_MS))
}

const createClientMessageId = () => (
  globalThis.crypto?.randomUUID?.()
  || `${Date.now()}-${Math.random().toString(16).slice(2)}`
)

const deliverMessage = async message => {
  message.deliveryStatus = 'sending'
  if (isChatSocketConnected()) {
    sendChatSocketMessage(message.receiver.publicId, message.content, message.clientMessageId)
    armMessageTimeout(message.clientMessageId)
    return
  }
  try {
    const savedMessage = await sendChatMessage(
      message.receiver.publicId,
      message.content,
      message.clientMessageId
    )
    handleIncomingMessage(savedMessage)
  } catch (error) {
    message.deliveryStatus = 'failed'
    throw error
  }
}

const retryMessage = async message => {
  try {
    await deliverMessage(message)
  } catch (error) {
    ElMessage.error(error.message || '消息重发失败')
  }
}

const handleReadReceipt = receipt => {
  if (!receipt?.readerPublicId || !receipt.lastReadMessageId) return
  if (selectedFriend.value?.publicId !== receipt.readerPublicId) return
  messages.value = messages.value.map(message => (
    message.mine
    && Number.isFinite(Number(message.id))
    && Number(message.id) <= Number(receipt.lastReadMessageId)
      ? { ...message, readAt: receipt.readAt }
      : message
  ))
}

const handleSendMessage = async () => {
  const content = draft.value.trim()
  if (!content || !selectedFriend.value || sendingMessage.value) return
  sendingMessage.value = true
  try {
    const clientMessageId = createClientMessageId()
    const pendingMessage = {
      id: `pending-${clientMessageId}`,
      clientMessageId,
      sender: null,
      receiver: { publicId: selectedFriend.value.publicId },
      content,
      createdAt: new Date().toISOString(),
      mine: true,
      deliveryStatus: 'sending'
    }
    await appendMessage(pendingMessage)
    await deliverMessage(pendingMessage)
    draft.value = ''
  } catch (error) {
    ElMessage.error(error.message || '消息发送失败')
  } finally {
    sendingMessage.value = false
  }
}

const refreshUnreadCounts = async () => {
  const result = await listChatUnreadCounts()
  const unreadCounts = new Map(
    (Array.isArray(result) ? result : []).map(item => [item.friendPublicId, Number(item.unreadCount) || 0])
  )
  friends.value.forEach(friend => {
    friend.unreadCount = selectedFriend.value?.publicId === friend.publicId
      ? 0
      : (unreadCounts.get(friend.publicId) || 0)
  })
}

const handleChatReconnect = async () => {
  try {
    await Promise.all([
      loadMessages(selectedFriend.value, { silent: true }),
      refreshUnreadCounts()
    ])
  } catch (error) {
    console.error('聊天重连同步失败', error)
  }
}

const handleSearch = async () => {
  const publicId = searchKeyword.value.trim()
  searchMessage.value = ''
  searchResult.value = null
  if (!publicId) return
  if (!/^\d{10}$/.test(publicId)) {
    searchMessage.value = '请输入完整的 10 位地球 ID'
    return
  }
  try {
    searchResult.value = await searchFriendByPublicId(publicId)
    searchMessage.value = '已找到这位地球居民'
  } catch (error) {
    searchMessage.value = error.status === 404 ? '没有找到该地球 ID 对应的用户' : (error.message || '搜索失败')
  }
}

const handleSendRequest = async () => {
  if (!searchResult.value || searchResult.value.relationStatus !== 'NONE') return
  sendingRequest.value = true
  try {
    await sendFriendRequest(searchResult.value.publicId)
    searchResult.value = { ...searchResult.value, relationStatus: 'PENDING_SENT' }
    ElMessage.success('好友申请已发出，等待对方确认')
  } catch (error) {
    ElMessage.error(error.message || '好友申请发送失败')
  } finally {
    sendingRequest.value = false
  }
}

watch(selectedFriend, async friend => {
  // 打开会话即清除本次页面会话期间累计的未读提示。
  if (friend) friend.unreadCount = 0
  messages.value = []
  loadMessages(friend)
  if (friend) {
    try {
      await markChatMessagesRead(friend.publicId)
    } catch (error) {
      console.error('标记聊天消息已读失败', error)
    }
  }
})
onMounted(async () => {
  connectChatSocket({
    onMessage: handleIncomingMessage,
    onReadReceipt: handleReadReceipt,
    onConnectionChange: connected => { chatConnected.value = connected },
    onReconnect: handleChatReconnect
  })
  await Promise.all([loadFriends(), loadReceivedRequests()])
})
onBeforeUnmount(() => {
  messageTimeouts.forEach(timeout => window.clearTimeout(timeout))
  messageTimeouts.clear()
  disconnectChatSocket()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.friends-view { display:grid; grid-template-columns:minmax(280px,340px) minmax(0,1fr); min-height:calc(100vh - 116px); overflow:hidden; border:1px solid rgba(102,194,138,.16); border-radius:24px; background:rgba(255,255,255,.88); box-shadow:$shadow-base; }
.friend-sidebar { display:flex; min-height:0; flex-direction:column; border-right:1px solid rgba(102,194,138,.16); background:rgba(249,253,249,.9); }
.friend-sidebar__header { padding:26px 22px 16px; }
.eyebrow { margin:0 0 6px; color:$primary-color; font-size:12px; font-weight:700; letter-spacing:.08em; }
h2,h3,p { margin-top:0; }
h2 { margin-bottom:5px; color:$text-main; font-size:24px; }
.friend-count { margin:0; color:$text-secondary; font-size:13px; }
.friend-search { display:flex; gap:8px; padding:0 18px 8px; }
.friend-search :deep(.el-input__wrapper) { border-radius:12px; box-shadow:0 0 0 1px rgba(102,194,138,.2) inset; }
.search-button { color:$primary-color; border-color:rgba(102,194,138,.35); }
.search-message { min-height:18px; margin:0 20px 8px; color:$primary-color; font-size:12px; }
.search-result { margin:0 14px 10px; padding:11px; border:1px solid rgba(102,194,138,.28); border-radius:14px; background:#fff; }
.search-result__profile { display:flex; gap:9px; align-items:center; }
.search-result__profile > div { min-width:0; flex:1; }
.search-result__profile strong,.search-result__profile span { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.search-result__profile strong { color:$text-main; font-size:14px; }
.search-result__profile span,.search-result__profile p { margin:3px 0 0; color:$text-secondary; font-size:11px; }
.search-result > .el-button { width:100%; margin-top:10px; }
.received-requests { max-height:260px; overflow-y:auto; margin:0 10px 8px; padding:10px; border:1px solid rgba(102,194,138,.22); border-radius:14px; background:rgba(255,255,255,.72); }
.received-requests__header { display:flex; align-items:center; justify-content:space-between; margin-bottom:8px; color:$text-main; font-size:13px; }
.received-requests__header span { display:grid; width:20px; height:20px; place-items:center; border-radius:50%; color:#fff; background:#ef6b6b; font-size:11px; }
.received-request { display:flex; gap:8px; padding:9px 0; border-top:1px solid rgba(102,194,138,.12); }
.received-request:first-of-type { border-top:0; }
.received-request__main { min-width:0; flex:1; }
.received-request__main strong,.received-request__main span { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.received-request__main strong { color:$text-main; font-size:13px; }.received-request__main span,.received-request__main p { margin:2px 0 0; color:$text-secondary; font-size:11px; }.received-request__main p { overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.received-request__actions { display:flex; gap:6px; margin-top:7px; }.received-request__actions .el-button { flex:1; margin:0; }
.friend-list { min-height:0; flex:1; overflow-y:auto; padding:4px 10px 16px; }
.friend-item { position:relative; display:flex; width:100%; gap:11px; padding:12px 10px; border:0; border-radius:14px; color:inherit; background:transparent; text-align:left; cursor:pointer; transition:.2s ease; }
.friend-item:hover,.friend-item.active { background:rgba(102,194,138,.14); }
.friend-avatar { display:grid; flex:0 0 42px; width:42px; height:42px; place-items:center; overflow:hidden; border:2px solid rgba(255,255,255,.85); border-radius:50%; color:#fff; font-weight:700; box-shadow:0 2px 8px rgba(62,126,85,.15); }
.friend-avatar img { width:100%; height:100%; object-fit:cover; }
.friend-avatar.large { flex-basis:46px; width:46px; height:46px; }
.friend-item__main { min-width:0; flex:1; }
.friend-item__top { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.friend-item strong { overflow:hidden; color:$text-main; font-size:14px; text-overflow:ellipsis; white-space:nowrap; }
.friend-item time,.friend-item__message { color:$text-secondary; font-size:12px; }
.friend-item__bottom { display:flex; min-width:0; align-items:center; gap:8px; margin-top:5px; }
.friend-item__message { display:block; min-width:0; flex:1; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.friend-item__unread { display:grid; min-width:19px; height:19px; flex:0 0 auto; padding:0 5px; place-items:center; border-radius:10px; color:#fff; background:#ef6b6b; font-size:10px; font-weight:700; line-height:1; }
.friend-public-id { display:block; margin-top:3px; color:$text-secondary; font-size:11px; font-variant-numeric:tabular-nums; }
.friend-list__empty { display:grid; padding:52px 16px; place-items:center; color:$text-secondary; text-align:center; }
.friend-list__empty .el-icon { color:$primary-color; font-size:36px; }
.chat-panel { display:flex; min-width:0; min-height:0; flex-direction:column; background:#fff; }
.chat-header { display:flex; min-height:74px; align-items:center; justify-content:space-between; padding:12px 26px; border-bottom:1px solid rgba(102,194,138,.14); }
.chat-user { display:flex; align-items:center; gap:12px; }
.chat-user h3 { margin-bottom:4px; color:$text-main; font-size:17px; }
.chat-user p { margin:0; color:$text-secondary; font-size:12px; }
.online-dot { display:inline-block; width:7px; height:7px; border-radius:50%; background:$primary-color; }
.online-dot.offline { background:#d9a441; }
.chat-content { display:flex; flex:1; min-height:260px; overflow-y:auto; padding:26px; background:linear-gradient(180deg,rgba(247,252,247,.72),#fff 35%); }
.chat-tip { position:absolute; align-self:start; padding:7px 12px; border-radius:999px; color:$text-secondary; background:rgba(102,194,138,.12); font-size:12px; }
.chat-placeholder { display:grid; width:100%; align-self:center; place-items:center; color:$text-secondary; text-align:center; }
.chat-placeholder .el-icon { color:$primary-color; font-size:44px; }
.message-list { display:flex; width:100%; align-self:flex-end; flex-direction:column; gap:12px; }
.message-time-divider { align-self:center; padding:3px 10px; border-radius:999px; color:$text-secondary; background:rgba(245,248,245,.94); font-size:12px; line-height:1.4; }
.message-row { display:flex; max-width:76%; align-items:flex-end; gap:8px; }.message-row.mine { align-self:flex-end; flex-direction:row-reverse; }
.message-body { display:flex; min-width:0; flex-direction:column; align-items:flex-start; }.message-row.mine .message-body { align-items:flex-end; }
.message-bubble { padding:10px 13px; border-radius:14px 14px 14px 3px; color:$text-main; background:#fff; box-shadow:0 2px 10px rgba(62,126,85,.08); }.message-row.mine .message-bubble { border-radius:14px 14px 3px 14px; color:#fff; background:$primary-color; }
.message-bubble p { margin:0; line-height:1.6; white-space:pre-wrap; word-break:break-word; }
.message-read-state { margin-top:4px; color:$text-secondary; font-size:10px; }
.message-retry { margin-top:4px; padding:0; border:0; color:#e45b5b; background:transparent; font-size:10px; cursor:pointer; }
.chat-editor { padding:12px 22px 16px; border-top:1px solid rgba(102,194,138,.14); }
.editor-tools { display:flex; gap:16px; margin:0 0 9px 4px; color:$text-regular; font-size:20px; }
.chat-editor :deep(.el-textarea__inner) { border-radius:12px; }
.editor-footer { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-top:10px; color:$text-secondary; font-size:12px; }
.chat-empty { display:grid; height:100%; padding:20px; place-content:center; place-items:center; color:$text-secondary; text-align:center; }
.chat-empty .el-icon { margin-bottom:12px; color:$primary-color; font-size:56px; }
.chat-empty h3 { margin-bottom:8px; color:$text-main; }
.chat-empty p { max-width:300px; margin-bottom:0; line-height:1.7; }
@media (max-width:760px) { .friends-view { grid-template-columns:1fr; min-height:auto; }.friend-sidebar { max-height:440px; border-right:0; border-bottom:1px solid rgba(102,194,138,.16); }.chat-panel { min-height:520px; } }
</style>
