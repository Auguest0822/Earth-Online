<template>
  <main class="friends-view">
    <aside class="friend-sidebar">
      <header class="friend-sidebar__header">
        <p class="eyebrow">EARTH FRIENDS</p>
        <h2>朋友</h2>
        <p class="friend-count">{{ filteredFriends.length }} 位好友</p>
      </header>

      <div class="friend-search">
        <el-input v-model="searchKeyword" placeholder="根据 ID 搜索好友" clearable @keyup.enter="handleSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button class="search-button" :icon="Search" circle @click="handleSearch" />
      </div>
      <p v-if="searchMessage" class="search-message">{{ searchMessage }}</p>

      <section class="friend-list" aria-label="好友列表">
        <button v-for="friend in filteredFriends" :key="friend.id" type="button" class="friend-item"
          :class="{ active: selectedFriend?.id === friend.id }" @click="selectedFriend = friend">
          <span class="friend-avatar" :style="{ background: friend.color }">{{ friend.name.slice(0, 1) }}</span>
          <span class="friend-item__main">
            <span class="friend-item__top"><strong>{{ friend.name }}</strong><time>{{ friend.time }}</time></span>
            <span class="friend-public-id">ID · {{ friend.publicId }}</span>
            <span class="friend-item__message">{{ friend.preview }}</span>
          </span>
          <i v-if="friend.unread" class="unread-dot">{{ friend.unread }}</i>
        </button>
        <div v-if="!filteredFriends.length" class="friend-list__empty">
          <el-icon><UserFilled /></el-icon><p>没有找到这位朋友</p>
        </div>
      </section>
    </aside>

    <section class="chat-panel">
      <template v-if="selectedFriend">
        <header class="chat-header">
          <div class="chat-user">
            <span class="friend-avatar large" :style="{ background: selectedFriend.color }">{{ selectedFriend.name.slice(0, 1) }}</span>
            <div><h3>{{ selectedFriend.name }}</h3><p><span class="online-dot"></span> 正在地球上生活</p></div>
          </div>
          <el-button text circle :icon="MoreFilled" title="更多操作" />
        </header>

        <div class="chat-content">
          <p class="chat-tip">你们已经成为朋友，开始聊聊彼此的地球吧。</p>
          <div v-for="message in selectedFriend.messages" :key="message.id" class="message-row" :class="message.from">
            <span v-if="message.from === 'friend'" class="friend-avatar small" :style="{ background: selectedFriend.color }">{{ selectedFriend.name.slice(0, 1) }}</span>
            <div><div class="message-bubble">{{ message.text }}</div><time>{{ message.time }}</time></div>
          </div>
        </div>

        <footer class="chat-editor">
          <div class="editor-tools"><el-icon><ChatDotRound /></el-icon><el-icon><Picture /></el-icon></div>
          <el-input v-model="draft" type="textarea" :rows="3" resize="none" maxlength="500" show-word-limit placeholder="写下想对朋友说的话…" />
          <div class="editor-footer"><span>聊天消息功能将在后端完成后启用</span><el-button type="primary" :disabled="!draft.trim()" @click="showChatNotice">发送</el-button></div>
        </footer>
      </template>
      <div v-else class="chat-empty"><el-icon><UserFilled /></el-icon><h3>选择一位朋友</h3><p>从左侧好友列表中选择一位朋友，开始你们的地球对话。</p></div>
    </section>
  </main>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, MoreFilled, Picture, Search, UserFilled } from '@element-plus/icons-vue'

// 当前为 UI 演示数据；好友关系、聊天消息接口完成后会替换为后端数据。
const friends = ref([
  { id: 'earth-10001', name: '星野', color: 'linear-gradient(135deg, #92d7b0, #5ea979)', preview: '今天也要好好生活呀。', time: '10:28', unread: 2, messages: [{ id: 1, from: 'friend', text: '今天看到一片很好看的云，想和你分享。', time: '10:26' }, { id: 2, from: 'self', text: '我也刚记录了今天的天空！', time: '10:27' }] },
  { id: 'earth-10002', name: '小满', color: 'linear-gradient(135deg, #ffcf85, #e58a75)', preview: '新的未来信写好了吗？', time: '昨天', unread: 0, messages: [{ id: 1, from: 'friend', text: '新的未来信写好了吗？', time: '昨天 20:10' }] },
  { id: 'earth-10003', name: '南风', color: 'linear-gradient(135deg, #9bbaf2, #7589ce)', preview: '周末一起去散步吧。', time: '星期日', unread: 0, messages: [{ id: 1, from: 'friend', text: '周末一起去散步吧。', time: '星期日 16:44' }] }
])
friends.value.forEach((friend, index) => {
  friend.publicId = ['4829317506', '6152849037', '7931604825'][index]
})
const searchKeyword = ref('')
const searchMessage = ref('')
const selectedFriend = ref(friends.value[0])
const draft = ref('')
const filteredFriends = computed(() => {
  const keyword = searchKeyword.value.trim().toLowerCase()
  if (!keyword) return friends.value
  return friends.value.filter(friend => friend.publicId.includes(keyword))
})
const handleSearch = () => {
  if (!searchKeyword.value.trim()) return (searchMessage.value = '')
  searchMessage.value = filteredFriends.value.length ? `已找到 ${filteredFriends.value.length} 位匹配的朋友` : '当前好友列表中没有该 ID'
}
const showChatNotice = () => ElMessage.info('聊天消息后端尚未接入：这条消息暂不会发送或保存。')
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.friends-view { display:grid; grid-template-columns:minmax(260px,320px) minmax(0,1fr); min-height:calc(100vh - 116px); overflow:hidden; border:1px solid rgba(102,194,138,.16); border-radius:24px; background:rgba(255,255,255,.88); box-shadow:$shadow-base; }
.friend-sidebar { display:flex; min-height:0; flex-direction:column; border-right:1px solid rgba(102,194,138,.16); background:rgba(249,253,249,.9); }
.friend-sidebar__header { padding:26px 22px 16px; }.eyebrow { margin:0 0 6px; color:$primary-color; font-size:12px; font-weight:700; letter-spacing:.08em; } h2,h3,p { margin-top:0; } h2 { margin-bottom:5px; color:$text-main; font-size:24px; }.friend-count { margin:0; color:$text-secondary; font-size:13px; }
.friend-search { display:flex; gap:8px; padding:0 18px 12px; }.friend-search :deep(.el-input__wrapper) { border-radius:12px; box-shadow:0 0 0 1px rgba(102,194,138,.2) inset; }.search-button { color:$primary-color; border-color:rgba(102,194,138,.35); }.search-message { min-height:18px; margin:0 20px 10px; color:$primary-color; font-size:12px; }
.friend-list { min-height:0; overflow-y:auto; padding:4px 10px 16px; }.friend-item { position:relative; display:flex; width:100%; gap:11px; padding:12px 10px; border:0; border-radius:14px; color:inherit; background:transparent; text-align:left; cursor:pointer; transition:.2s ease; }.friend-item:hover,.friend-item.active { background:rgba(102,194,138,.14); }.friend-avatar { display:grid; flex:0 0 42px; width:42px; height:42px; place-items:center; border:2px solid rgba(255,255,255,.85); border-radius:50%; color:#fff; font-weight:700; box-shadow:0 2px 8px rgba(62,126,85,.15); }.friend-avatar.large { flex-basis:46px; width:46px; height:46px; }.friend-avatar.small { flex-basis:32px; width:32px; height:32px; font-size:13px; }.friend-item__main { min-width:0; flex:1; }.friend-item__top { display:flex; align-items:center; justify-content:space-between; gap:8px; }.friend-item strong { overflow:hidden; color:$text-main; font-size:14px; text-overflow:ellipsis; white-space:nowrap; }.friend-item time,.friend-item__message { color:$text-secondary; font-size:12px; }.friend-item__message { display:block; overflow:hidden; margin-top:5px; text-overflow:ellipsis; white-space:nowrap; }.unread-dot { display:grid; min-width:17px; height:17px; place-items:center; border-radius:50%; color:#fff; background:#ec7b73; font-size:10px; font-style:normal; }.friend-list__empty { display:grid; padding:52px 0; place-items:center; color:$text-secondary; text-align:center; }.friend-list__empty .el-icon { color:$primary-color; font-size:36px; }
.chat-panel { display:flex; min-width:0; min-height:0; flex-direction:column; background:#fff; }.chat-header { display:flex; min-height:74px; align-items:center; justify-content:space-between; padding:12px 26px; border-bottom:1px solid rgba(102,194,138,.14); }.chat-user { display:flex; align-items:center; gap:12px; }.chat-user h3 { margin-bottom:4px; color:$text-main; font-size:17px; }.chat-user p { margin:0; color:$text-secondary; font-size:12px; }.online-dot { display:inline-block; width:7px; height:7px; border-radius:50%; background:$primary-color; }
.chat-content { flex:1; overflow-y:auto; padding:26px clamp(22px,4vw,64px); background:linear-gradient(180deg,rgba(247,252,247,.72),#fff 35%); }.chat-tip { width:fit-content; margin:0 auto 26px; padding:7px 12px; border-radius:999px; color:$text-secondary; background:rgba(102,194,138,.12); font-size:12px; }.message-row { display:flex; max-width:78%; gap:9px; margin:14px 0; align-items:flex-start; }.message-row.self { flex-direction:row-reverse; margin-left:auto; }.message-bubble { padding:11px 14px; border-radius:4px 15px 15px; color:$text-main; background:#f1f6f2; line-height:1.6; }.message-row.self .message-bubble { border-radius:15px 4px 15px 15px; background:#dff3e5; }.message-row time { display:block; margin-top:4px; color:$text-secondary; font-size:11px; }.message-row.self time { text-align:right; }
.chat-editor { padding:12px 22px 16px; border-top:1px solid rgba(102,194,138,.14); }.editor-tools { display:flex; gap:16px; margin:0 0 9px 4px; color:$text-regular; font-size:20px; }.chat-editor :deep(.el-textarea__inner) { border-radius:12px; box-shadow:0 0 0 1px rgba(102,194,138,.17) inset; }.editor-footer { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-top:10px; color:$text-secondary; font-size:12px; }
.chat-empty { display:grid; height:100%; padding:20px; place-content:center; place-items:center; color:$text-secondary; text-align:center; }.chat-empty .el-icon { margin-bottom:12px; color:$primary-color; font-size:56px; }.chat-empty h3 { margin-bottom:8px; color:$text-main; }.chat-empty p { max-width:300px; margin-bottom:0; line-height:1.7; }
.friend-public-id { display:block; margin-top:3px; color:$text-secondary; font-size:11px; font-variant-numeric:tabular-nums; }
@media (max-width:760px) { .friends-view { grid-template-columns:1fr; min-height:auto; }.friend-sidebar { max-height:370px; border-right:0; border-bottom:1px solid rgba(102,194,138,.16); }.chat-panel { min-height:580px; } }
</style>
