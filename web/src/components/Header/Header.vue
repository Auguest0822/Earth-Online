<template>
  <div class="header">
    <div class="header-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-profile">
          <img :src="profileStore.userInfo?.avatar || avatarPlaceholder" class="avatar" />
          <div class="user-info">
            <div class="user-line" :title="profileStore.userInfo?.username">
              <span class="name">{{ displayName }}</span>
            </div>
          </div>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="switch">切换账号</el-dropdown-item>
            <el-dropdown-item command="logout">退出登录</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <div class="search-box">
        <el-input
          v-model="searchQuery"
          placeholder="搜索记录、心情或标签..."
          prefix-icon="Search"
          clearable
          @focus="searchFocused = true"
          @blur="closeSearchLater"
        />
        <div v-if="searchFocused && searchQuery.trim()" class="search-results">
          <div v-if="searchLoading" class="search-state">正在搜索…</div>
          <template v-else>
            <button
              v-for="record in searchResults"
              :key="record.id"
              type="button"
              @mousedown.prevent="openRecord(record)"
            >
              <strong>{{ record.title || '这一天的记录' }}</strong>
              <span>{{ formatRecordDate(record.date) }} · {{ record.mood || record.eventType || '生活记录' }}</span>
              <p>{{ record.content }}</p>
              <small v-if="record.keywords?.length">{{ record.keywords.map(item => `#${item}`).join(' ') }}</small>
            </button>
          </template>
          <div v-if="!searchLoading && !searchResults.length" class="search-state">没有找到匹配的记录</div>
        </div>
      </div>
      
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useProfileStore } from '@/stores/profile'
import { useRouter } from 'vue-router'
import { logout } from '@/api/modules/auth'
import avatarPlaceholder from '@/assets/picture/default-avatar.png'
import { searchRecords } from '@/api/modules/records'

const profileStore = useProfileStore()
const router = useRouter()
const searchQuery = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const searchFocused = ref(false)
let searchTimer = null
let searchController = null

const displayName = computed(() => {
  const nickname = profileStore.userInfo?.username || profileStore.userInfo?.nickname || '地球居民'
  return Array.from(nickname).slice(0, 10).join('')
})

const handleCommand = (command) => {
  if (command === 'switch' || command === 'logout') {
    logout()
    profileStore.clearUserSession()
    router.replace('/login')
  }
}

watch(searchQuery, value => {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchController?.abort()
  const query = value.trim()
  if (!query) {
    searchResults.value = []
    searchLoading.value = false
    return
  }
  searchLoading.value = true
  searchTimer = window.setTimeout(async () => {
    searchController = new AbortController()
    try {
      searchResults.value = await searchRecords(query, searchController.signal)
    } catch (error) {
      if (error.name !== 'AbortError') searchResults.value = []
    } finally {
      searchLoading.value = false
    }
  }, 300)
})

const closeSearchLater = () => window.setTimeout(() => { searchFocused.value = false }, 150)
const formatRecordDate = value => new Date(value).toLocaleDateString('zh-CN')
const openRecord = record => {
  searchFocused.value = false
  router.push({ path: '/timeline', query: { record: String(record.id) } })
}

// 页面刷新时 Pinia 会重新初始化。这里重新向后端获取当前登录用户，
// 避免 Header 继续展示旧的本地默认资料。
onMounted(() => {
  profileStore.fetchProfileData().catch((error) => {
    console.warn('[Earth Online] failed to load current user for header:', error)
  })
})

onBeforeUnmount(() => {
  if (searchTimer) window.clearTimeout(searchTimer)
  searchController?.abort()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.header {
  height: $header-height;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 32px;
  background: transparent;
}

.header-right {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 24px;
  width: 100%;

  .search-box {
    position: relative;
    width: 280px;
    :deep(.el-input__wrapper) {
      border-radius: 20px;
      box-shadow: 0 0 0 1px $border-color inset;
      background-color: transparent;
      &:hover, &.is-focus {
        box-shadow: 0 0 0 1px $primary-color inset;
      }
    }

    .search-results {
      position: absolute;
      z-index: 30;
      top: calc(100% + 8px);
      left: 0;
      width: min(430px, 80vw);
      max-height: 420px;
      overflow-y: auto;
      padding: 8px;
      border: 1px solid rgba(102,194,138,.18);
      border-radius: 14px;
      background: #fff;
      box-shadow: 0 14px 34px rgba(45,90,61,.16);

      button { display:block; width:100%; padding:10px 12px; border:0; border-radius:10px; background:transparent; text-align:left; cursor:pointer; }
      button:hover { background:rgba(102,194,138,.1); }
      strong,span,p,small { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
      strong { color:$text-main; font-size:13px; }
      span,small { margin-top:3px; color:$text-secondary; font-size:10px; }
      p { margin:5px 0 0; color:$text-regular; font-size:12px; }
      small { color:$primary-color; }
    }
    .search-state { padding:18px 12px; color:$text-secondary; font-size:12px; text-align:center; }
  }

  .user-profile {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;

    .avatar {
      width: 36px;
      height: 36px;
      object-fit: cover;
      border-radius: 50%;
    }

    .user-info {
      display: flex;
      align-items: center;
      max-width: 180px;
      min-width: 0;

      .user-line {
        display: flex;
        align-items: center;
        gap: 6px;
        min-width: 0;
      }

      .name {
        overflow: hidden;
        font-size: 14px;
        color: $text-main;
        font-weight: 500;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }
  }
}

@media (max-width: 1280px) {
  .header { padding: 0 24px; }
  .header-right { gap: 18px; }
  .header-right .search-box { width: 220px; }
}

@media (max-width: 900px) {
  .header { height: 68px; padding: 0 18px; }
  .header-right { gap: 14px; }
  .header-right .search-box { flex: 1; width: auto; max-width: 340px; }
  .header-right .user-profile { gap: 7px; }
  .header-right .user-profile .user-info { max-width: 96px; }
}

@media (max-width: 640px) {
  .header { height: 60px; padding: 0 12px; }
  .header-right { justify-content: flex-end; gap: 8px; }
  .header-right .search-box { display: none; }
  .header-right .user-profile .user-info,
  .header-right .user-profile > .el-icon { display: none; }
  .header-right .user-profile .avatar { width: 32px; height: 32px; }
}
</style>
