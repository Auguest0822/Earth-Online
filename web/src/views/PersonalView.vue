<template>
  <div class="personal-view" v-if="profileStore.userInfo">
    <section class="profile-hero">
      <div class="identity">
        <div class="avatar-ring">
          <img :src="avatarSrc" alt="avatar" />
        </div>
        <div class="identity-text">
          <span class="eyebrow">My Earth Archive</span>
        <h2>{{ profileStore.userInfo.username || '地球居民' }}</h2>
          <p>{{ profileStore.userInfo.slogan }}</p>
          <div class="identity-tags">
            <span>{{ displayInfo.survivalDuration }}</span>
            <span>{{ profileStore.userInfo.birthDate }}</span>
          </div>
        </div>
      </div>

      <el-button class="edit-profile-button" type="success" plain @click="openProfileEditor">
        <el-icon><EditPen /></el-icon>
        编辑资料
      </el-button>

    </section>

    <section class="stats-grid">
      <article v-for="item in statCards" :key="item.label" class="stat-card">
        <div class="stat-icon">
          <el-icon><component :is="item.icon" /></el-icon>
        </div>
        <div>
          <strong>{{ item.value }}</strong>
          <span>{{ item.label }}</span>
        </div>
      </article>
    </section>

    <section class="content-grid">
      <div class="main-column">
        <article class="panel recent-panel">
          <div class="panel-head">
            <div>
              <h3>我的轨迹</h3>
              <p>最近记录的几个片段</p>
            </div>
            <el-button type="success" plain size="small" @click="router.push('/timeline')">
              查看时间轴
            </el-button>
          </div>

          <div v-if="recentTimelines.length" class="trace-list">
            <div v-for="item in recentTimelines" :key="item.id" class="trace-item">
              <div class="trace-date">
                <strong>{{ formatDay(item.date) }}</strong>
                <span>{{ formatMonth(item.date) }}</span>
              </div>
              <div class="trace-content">
                <p>{{ item.content }}</p>
                <div class="trace-tags">
                  <span v-for="tag in item.keywords" :key="tag">{{ tag }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="empty-state">
            <div class="empty-state-icon">📝</div>
            <strong>还没有记录</strong>
            <p>记录今天发生的事情，慢慢建立属于你的地球轨迹。</p>
            <el-button type="success" plain @click="router.push('/')">记录此刻</el-button>
          </div>
        </article>

        <article class="panel keyword-panel">
          <div class="panel-head">
            <div>
              <h3>我的关键词</h3>
              <p>这些词构成了最近的你</p>
            </div>
          </div>
          <div v-if="profileStore.keywords.length" class="keyword-cloud">
            <span
              v-for="item in profileStore.keywords"
              :key="item.name"
              :style="{ '--weight': item.value }"
            >
              {{ item.name }}
            </span>
          </div>
          <div v-else class="empty-state keyword-empty-state">
            <div class="empty-state-icon">#</div>
            <strong>还没有关键词</strong>
            <p>记录生活并添加标签后，这里会生成属于你的关键词云。</p>
            <el-button type="success" plain @click="router.push('/')">去写一条记录</el-button>
          </div>
        </article>
      </div>

      <aside class="side-column">
        <article class="panel settings-card">
          <div class="panel-head">
            <div>
              <h3>个人设置</h3>
              <p>维护你的地球档案</p>
            </div>
            <el-icon><Setting /></el-icon>
          </div>
          <div class="setting-list">
            <button v-for="item in settingItems" :key="item.label" @click="handleSettingClick(item.action)">
              <span>
                <el-icon><component :is="item.icon" /></el-icon>
                {{ item.label }}
              </span>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </article>

      </aside>
    </section>

    <el-dialog
      v-model="profileEditorVisible"
      title="编辑个人资料"
      width="760px"
      class="profile-editor-dialog"
    >
      <el-form label-position="top" @submit.prevent>
        <div class="profile-editor-layout">
          <aside class="avatar-editor">
          <div class="avatar-preview">
            <img :src="avatarPreview" alt="avatar preview" />
          </div>
          <el-upload
            class="avatar-upload"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            @change="handleAvatarFile"
          >
            <el-button type="success" plain size="small">选择头像</el-button>
          </el-upload>
          <div class="avatar-note">
            <strong>{{ profileStore.userInfo.username }}</strong>
            <span>头像上传功能将在后续开放</span>
          </div>
          </aside>

          <section class="profile-editor-fields">
          <el-form-item label="用户名">
            <el-input :model-value="profileStore.userInfo.username" disabled />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" type="email" placeholder="请输入常用邮箱" />
          </el-form-item>
          <el-form-item label="生日">
            <el-date-picker
              v-model="profileForm.birthDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择出生日期"
              :disabled-date="disabledFutureDate"
              style="width: 100%"
            />
            <p class="field-help">用于计算“在地球上生存的天数”，请填写真实日期。</p>
          </el-form-item>
        <el-form-item label="个性签名">
          <el-input
            v-model="profileForm.slogan"
            type="textarea"
            maxlength="80"
            show-word-limit
            :autosize="{ minRows: 3, maxRows: 4 }"
            placeholder="写一句现在的你"
          />
        </el-form-item>
          </section>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="profileEditorVisible = false">取消</el-button>
        <el-button type="success" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useProfileStore } from '@/stores/profile'
import { uploadAvatar } from '@/api/modules/uploads'
import avatarPlaceholder from '@/assets/picture/avator.jpg'

const router = useRouter()
const profileStore = useProfileStore()

onMounted(() => {
  profileStore.fetchProfileData()
})

const displayInfo = computed(() => ({
  ...profileStore.userInfo,
  ...(profileStore.userStats || {})
}))

const recentTimelines = computed(() => profileStore.timelines.slice(0, 3))
const futureLetterCount = computed(() => profileStore.futureLetters.length)
const profileEditorVisible = ref(false)
const avatarFile = ref(null)
const profileForm = reactive({
  email: '',
  avatar: '',
  slogan: '',
  birthDate: ''
})

const statCards = computed(() => [
  { label: '生存天数', value: displayInfo.value.survivalDays, icon: 'Sunrise' },
  { label: '记录总数', value: profileStore.timelines.length, icon: 'Notebook' },
  { label: '未来信件', value: futureLetterCount.value, icon: 'MessageBox' },
  { label: '关键词数', value: profileStore.keywords.length, icon: 'PriceTag' }
])

const avatarSrc = computed(() => profileStore.userInfo?.avatar || avatarPlaceholder)
const avatarPreview = computed(() => profileForm.avatar || avatarSrc.value)

const settingItems = [
  { label: '编辑头像与昵称', icon: 'User', action: 'editProfile' },
  { label: '更新个性签名', icon: 'EditPen', action: 'editProfile' },
  { label: '隐私与展示设置', icon: 'Lock' },
  { label: '消息提醒', icon: 'Bell' }
]

const openProfileEditor = () => {
  profileForm.email = profileStore.userInfo?.email || ''
  profileForm.avatar = profileStore.userInfo?.avatar || ''
  profileForm.slogan = profileStore.userInfo?.slogan || ''
  profileForm.birthDate = profileStore.userInfo?.birthDate || ''
  avatarFile.value = null
  profileEditorVisible.value = true
}

const handleSettingClick = (action) => {
  if (action === 'editProfile') {
    openProfileEditor()
  }
}

const handleAvatarFile = (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return

  if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
    ElMessage.warning('请选择 JPG、PNG 或 WebP 图片')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 5MB')
    return
  }

  avatarFile.value = file
  profileForm.avatar = URL.createObjectURL(file)
  return

  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    profileForm.avatar = reader.result
  }
  reader.readAsDataURL(file)
}

const disabledFutureDate = (date) => date.getTime() > Date.now()

const saveProfile = async () => {
  const email = profileForm.email.trim()
  if (!email) {
    ElMessage.warning('邮箱不能为空')
    return
  }

  try {
    const payload = {
      email,
      slogan: profileForm.slogan.trim(),
      birthDate: profileForm.birthDate || null
    }

    if (avatarFile.value) {
      const uploaded = await uploadAvatar(avatarFile.value)
      payload.avatar = uploaded.objectKey
    }

    await profileStore.updateUserInfo(payload)
    avatarFile.value = null
    profileEditorVisible.value = false
    ElMessage.success('个人资料已更新')
    return

    await profileStore.updateUserInfo({
      email,
      avatar: profileForm.avatar.trim() || profileStore.userInfo.avatar,
      slogan: profileForm.slogan.trim(),
      birthDate: profileForm.birthDate || null
    })
    profileEditorVisible.value = false
    ElMessage.success('个人资料已更新')
  } catch (error) {
    const message = error?.message || '保存失败，请重新登录后再试'
    ElMessage.error(message)
  }
}

const getDate = (dateStr) => new Date(dateStr)

const formatDay = (dateStr) => {
  const date = getDate(dateStr)
  return Number.isNaN(date.getTime()) ? '--' : String(date.getDate()).padStart(2, '0')
}

const formatMonth = (dateStr) => {
  const date = getDate(dateStr)
  if (Number.isNaN(date.getTime())) return '--'
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}`
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.personal-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding-bottom: 8px;
}

.profile-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 24px;
  padding: 28px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(240, 250, 244, 0.92)),
    radial-gradient(circle at 18px 18px, rgba(102, 194, 138, 0.14) 1px, transparent 1.5px);
  background-size: auto, 22px 22px;
  border: 1px solid rgba(102, 194, 138, 0.16);
  border-radius: $radius-base;
  box-shadow: $shadow-base;
}

.edit-profile-button {
  align-self: start;
  white-space: nowrap;
}

.identity {
  display: flex;
  align-items: center;
  gap: 24px;
  min-width: 0;
}

.avatar-ring {
  flex: 0 0 auto;
  width: 108px;
  height: 108px;
  padding: 5px;
  background: linear-gradient(135deg, $primary-color, #d8efe0);
  border-radius: 50%;
  box-shadow: 0 12px 28px rgba(102, 194, 138, 0.2);

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border: 4px solid #fff;
    border-radius: 50%;
  }
}

.identity-text {
  min-width: 0;

  h2 {
    margin: 6px 0 10px;
    color: $text-main;
    font-size: 28px;
    line-height: 1.2;
  }

  p {
    max-width: 560px;
    margin: 0;
    color: $text-regular;
    font-size: 14px;
    line-height: 1.7;
  }
}

.eyebrow {
  color: #3f9b68;
  font-size: 12px;
  font-weight: 700;
}

.identity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;

  span {
    padding: 7px 12px;
    color: #3f9b68;
    background: rgba(102, 194, 138, 0.12);
    border: 1px solid rgba(102, 194, 138, 0.18);
    border-radius: 999px;
    font-size: 12px;
    font-weight: 600;
  }
}

.panel,
.stat-card {
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(102, 194, 138, 0.12);
  border-radius: $radius-base;
  box-shadow: $shadow-base;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;

  strong,
  span {
    display: block;
  }

  strong {
    margin-bottom: 4px;
    color: $text-main;
    font-size: 24px;
  }

  span {
    color: $text-secondary;
    font-size: 13px;
  }
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  color: #3f9b68;
  background: rgba(102, 194, 138, 0.13);
  border-radius: 12px;
  font-size: 21px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 24px;
}

.main-column,
.side-column {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
}

.panel {
  padding: 24px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;

  h3 {
    margin: 0 0 6px;
    color: $text-main;
    font-size: 18px;
  }

  p {
    margin: 0;
    color: $text-secondary;
    font-size: 13px;
  }

  > .el-icon {
    color: $primary-color;
    font-size: 22px;
  }
}

.trace-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.trace-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: #fbfdfb;
  border: 1px solid rgba(102, 194, 138, 0.12);
  border-radius: 14px;
}

.trace-date {
  width: 62px;
  text-align: center;

  strong {
    display: block;
    color: $primary-color;
    font-size: 24px;
    line-height: 1;
  }

  span {
    color: $text-secondary;
    font-size: 12px;
  }
}

.trace-content {
  flex: 1;
  min-width: 0;

  p {
    margin: 0 0 10px;
    color: $text-main;
    font-size: 14px;
    line-height: 1.7;
  }
}

.trace-tags,
.keyword-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.trace-tags span,
.keyword-cloud span {
  color: #3f9b68;
  background: rgba(102, 194, 138, 0.12);
  border-radius: 999px;
}

.trace-tags span {
  padding: 4px 10px;
  font-size: 12px;
}

.keyword-cloud {
  align-items: center;
  min-height: 120px;

  span {
    padding: 8px 13px;
    font-size: calc(12px + var(--weight) * 0.04px);
    font-weight: 600;
  }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 190px;
  padding: 20px;
  text-align: center;
  border: 1px dashed rgba(102, 194, 138, 0.3);
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(246, 252, 248, 0.9), rgba(255, 255, 255, 0.8));

  .empty-state-icon {
    display: grid;
    place-items: center;
    width: 46px;
    height: 46px;
    margin-bottom: 10px;
    color: #3f9b68;
    background: rgba(102, 194, 138, 0.13);
    border-radius: 14px;
    font-size: 22px;
    font-weight: 700;
  }

  strong {
    color: $text-main;
    font-size: 16px;
  }

  p {
    max-width: 360px;
    margin: 8px 0 14px;
    color: $text-secondary;
    font-size: 13px;
    line-height: 1.7;
  }
}

.keyword-empty-state {
  min-height: 150px;
}

.setting-list {
  display: flex;
  flex-direction: column;
  gap: 10px;

  button {
    display: flex;
    align-items: center;
    justify-content: space-between;
    min-height: 44px;
    padding: 0 12px;
    color: $text-regular;
    background: #fbfdfb;
    border: 1px solid rgba(102, 194, 138, 0.12);
    border-radius: 12px;
    cursor: pointer;
    transition: color 0.2s ease, border-color 0.2s ease, background 0.2s ease;

    &:hover {
      color: #3f9b68;
      background: rgba(102, 194, 138, 0.08);
      border-color: rgba(102, 194, 138, 0.24);
    }

    span {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }
}

.avatar-editor {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  min-height: 100%;
  padding: 14px 28px 14px 4px;
  border-right: 1px solid rgba(102, 194, 138, 0.18);
}

.profile-editor-layout {
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 28px;
  align-items: stretch;
}

.profile-editor-fields {
  min-width: 0;
}

.avatar-note {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 14px;
  text-align: center;

  strong {
    color: $text-main;
    font-size: 16px;
  }

  span,
  .field-help {
    color: $text-secondary;
    font-size: 12px;
    line-height: 1.5;
  }
}

.field-help {
  margin: 7px 0 0;
  color: $text-secondary;
  font-size: 12px;
  line-height: 1.5;
}

.avatar-preview {
  width: 96px;
  height: 96px;
  padding: 4px;
  background: linear-gradient(135deg, $primary-color, #d8efe0);
  border-radius: 50%;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border: 3px solid #fff;
    border-radius: 50%;
  }
}

.avatar-upload {
  margin-top: 14px;
}

.avatar-actions {
  display: none;
  /* avatar upload is temporarily disabled */
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}

:deep(.profile-editor-dialog) {
  .el-dialog__body {
    padding-top: 8px;
  }
}

.achievement-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;

  div {
    display: flex;
    align-items: center;
    gap: 8px;
    min-height: 42px;
    padding: 0 12px;
    color: $text-secondary;
    background: #f7f8f7;
    border-radius: 12px;

    &.active {
      color: #3f9b68;
      background: rgba(102, 194, 138, 0.12);
      font-weight: 600;
    }
  }
}

@media (max-width: 1200px) {
  .profile-hero,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .profile-hero {
    padding: 22px;
  }

  .identity {
    align-items: flex-start;
    flex-direction: column;
  }

  .stats-grid,
  .achievement-list {
    grid-template-columns: 1fr;
  }

  .avatar-editor {
    padding: 0 0 18px;
    border-right: 0;
    border-bottom: 1px solid rgba(102, 194, 138, 0.18);
  }

  .profile-editor-layout {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .avatar-actions {
    width: 100%;
  }
}
</style>
