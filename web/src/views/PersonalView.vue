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
          <p v-if="profileStore.userInfo.slogan?.trim()">{{ profileStore.userInfo.slogan }}</p>
          <div class="identity-tags">
            <span v-if="profileStore.userInfo.publicId">地球 ID · {{ profileStore.userInfo.publicId }}</span>
            <span v-if="displayInfo.survivalDuration">{{ displayInfo.survivalDuration }}</span>
            <span v-if="profileStore.userInfo.birthDate">{{ profileStore.userInfo.birthDate }}</span>
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
                  <strong>{{ formatFullDate(item.date) }}</strong>
                </div>
                <div class="trace-content">
                  <p>{{ getContentPreview(item.content) }}</p>
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

      </div>

      <aside class="side-column">
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

      </aside>
    </section>

    <el-dialog
      v-model="profileEditorVisible"
      title="编辑个人资料"
      width="820px"
      class="profile-editor-dialog"
      @closed="resetAvatarCrop"
    >
      <el-form label-position="top" @submit.prevent>
        <div class="profile-editor-layout">
          <aside class="avatar-editor">
          <div
            v-if="cropSource"
            ref="cropViewportRef"
            class="avatar-crop-viewport"
            @pointerdown="startCropDrag"
            @pointermove="moveCropImage"
            @pointerup="endCropDrag"
            @pointercancel="endCropDrag"
          >
            <img
              ref="cropImageRef"
              :src="cropSource"
              :style="cropImageStyle"
              alt="头像裁剪预览"
              draggable="false"
              @load="handleCropImageLoad"
            />
            <span class="crop-guide" aria-hidden="true"></span>
          </div>
          <div v-else class="avatar-preview">
            <img :src="avatarPreview" alt="avatar preview" />
          </div>
          <div v-if="cropSource" class="crop-zoom">
            <span>缩小</span>
            <input
              v-model.number="cropZoom"
              type="range"
              min="1"
              max="3"
              step="0.01"
              aria-label="头像缩放"
              @input="clampCropOffset"
            />
            <span>放大</span>
          </div>
          <el-upload
            class="avatar-upload"
            accept="image/jpeg,image/png,image/webp"
            :auto-upload="false"
            :show-file-list="false"
            @change="handleAvatarFile"
          >
            <el-button type="success" plain size="small">{{ cropSource ? '重新选择' : '选择头像' }}</el-button>
          </el-upload>
          <div class="avatar-note">
            <strong>{{ profileStore.userInfo.username }}</strong>
            <span>{{ cropSource ? '拖动图片调整位置，使用滑块缩放' : '支持 JPG、PNG、WebP，最大 5MB' }}</span>
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
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useProfileStore } from '@/stores/profile'
import { uploadAvatar } from '@/api/modules/uploads'
import avatarPlaceholder from '@/assets/picture/default-avatar.png'

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
const cropSource = ref('')
const cropImageRef = ref(null)
const cropViewportRef = ref(null)
const cropZoom = ref(1)
const cropBaseScale = ref(1)
const cropOffset = reactive({ x: 0, y: 0 })
const cropDrag = reactive({ active: false, startX: 0, startY: 0, originX: 0, originY: 0 })
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
const cropImageStyle = computed(() => ({
  width: `${cropImageRef.value?.naturalWidth || 1}px`,
  height: `${cropImageRef.value?.naturalHeight || 1}px`,
  transform: `translate(-50%, -50%) translate(${cropOffset.x}px, ${cropOffset.y}px) scale(${cropBaseScale.value * cropZoom.value})`
}))

const revokeCropSource = () => {
  if (cropSource.value.startsWith('blob:')) URL.revokeObjectURL(cropSource.value)
  cropSource.value = ''
}

const resetAvatarCrop = () => {
  revokeCropSource()
  avatarFile.value = null
  cropZoom.value = 1
  cropBaseScale.value = 1
  cropOffset.x = 0
  cropOffset.y = 0
  cropDrag.active = false
}

const openProfileEditor = () => {
  profileForm.email = profileStore.userInfo?.email || ''
  profileForm.avatar = profileStore.userInfo?.avatar || ''
  profileForm.slogan = profileStore.userInfo?.slogan || ''
  profileForm.birthDate = profileStore.userInfo?.birthDate || ''
  resetAvatarCrop()
  profileEditorVisible.value = true
}

const cropViewportSize = () => cropViewportRef.value?.clientWidth || 220

const clampCropOffset = () => {
  const image = cropImageRef.value
  if (!image?.naturalWidth) return
  const size = cropViewportSize()
  const scale = cropBaseScale.value * cropZoom.value
  const maxX = Math.max(0, (image.naturalWidth * scale - size) / 2)
  const maxY = Math.max(0, (image.naturalHeight * scale - size) / 2)
  cropOffset.x = Math.max(-maxX, Math.min(maxX, cropOffset.x))
  cropOffset.y = Math.max(-maxY, Math.min(maxY, cropOffset.y))
}

const handleCropImageLoad = () => {
  const image = cropImageRef.value
  if (!image?.naturalWidth) return
  const size = cropViewportSize()
  cropBaseScale.value = Math.max(size / image.naturalWidth, size / image.naturalHeight)
  cropZoom.value = 1
  cropOffset.x = 0
  cropOffset.y = 0
}

const startCropDrag = (event) => {
  cropDrag.active = true
  cropDrag.startX = event.clientX
  cropDrag.startY = event.clientY
  cropDrag.originX = cropOffset.x
  cropDrag.originY = cropOffset.y
  event.currentTarget.setPointerCapture?.(event.pointerId)
}

const moveCropImage = (event) => {
  if (!cropDrag.active) return
  cropOffset.x = cropDrag.originX + event.clientX - cropDrag.startX
  cropOffset.y = cropDrag.originY + event.clientY - cropDrag.startY
  clampCropOffset()
}

const endCropDrag = () => {
  cropDrag.active = false
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

  revokeCropSource()
  avatarFile.value = file
  cropSource.value = URL.createObjectURL(file)
}

const createCroppedAvatar = () => new Promise((resolve, reject) => {
  const image = cropImageRef.value
  if (!image?.naturalWidth) {
    reject(new Error('头像图片尚未加载完成'))
    return
  }

  const viewportSize = cropViewportSize()
  const scale = cropBaseScale.value * cropZoom.value
  const sourceSize = viewportSize / scale
  const sourceX = (image.naturalWidth - sourceSize) / 2 - cropOffset.x / scale
  const sourceY = (image.naturalHeight - sourceSize) / 2 - cropOffset.y / scale
  const canvas = document.createElement('canvas')
  canvas.width = 512
  canvas.height = 512
  const context = canvas.getContext('2d')
  context.drawImage(image, sourceX, sourceY, sourceSize, sourceSize, 0, 0, 512, 512)
  canvas.toBlob((blob) => {
    if (!blob) {
      reject(new Error('头像裁剪失败'))
      return
    }
    resolve(new File([blob], 'avatar-cropped.jpg', { type: 'image/jpeg' }))
  }, 'image/jpeg', 0.9)
})

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

    if (avatarFile.value && cropSource.value) {
      const croppedAvatar = await createCroppedAvatar()
      const uploaded = await uploadAvatar(croppedAvatar)
      payload.avatar = uploaded.objectKey
    }

    await profileStore.updateUserInfo(payload)
    profileEditorVisible.value = false
    ElMessage.success('个人资料已更新')
  } catch (error) {
    const message = error?.message || '保存失败，请重新登录后再试'
    ElMessage.error(message)
  }
}

onBeforeUnmount(revokeCropSource)

const getDate = (dateStr) => new Date(dateStr)

const formatFullDate = (dateStr) => {
  const date = getDate(dateStr)
  if (Number.isNaN(date.getTime())) return '--'
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

const getContentPreview = (content, maxLength = 96) => {
  const normalized = String(content || '').replace(/\s+/g, ' ').trim()
  return normalized.length > maxLength ? `${normalized.slice(0, maxLength)}…` : normalized
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
  flex: 0 0 120px;
  padding-top: 4px;
  text-align: left;

  strong {
    display: block;
    color: $primary-color;
    font-size: 14px;
    font-weight: 700;
    line-height: 1.5;
    white-space: nowrap;
  }
}

.trace-content {
  flex: 1;
  min-width: 0;

  p {
    display: -webkit-box;
    overflow: hidden;
    margin: 0 0 10px;
    color: $text-main;
    font-size: 14px;
    line-height: 1.7;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
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
  grid-template-columns: 250px minmax(0, 1fr);
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

.avatar-crop-viewport {
  position: relative;
  width: 220px;
  max-width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  border: 4px solid #fff;
  border-radius: 50%;
  background: #e8f4ec;
  box-shadow: 0 0 0 3px rgba(102, 194, 138, 0.42), 0 10px 24px rgba(62, 126, 85, 0.16);
  cursor: grab;
  touch-action: none;
  user-select: none;

  &:active { cursor: grabbing; }

  img {
    position: absolute;
    top: 50%;
    left: 50%;
    max-width: none;
    pointer-events: none;
    transform-origin: center;
    user-select: none;
  }
}

.crop-guide {
  position: absolute;
  inset: 0;
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 50%;
  pointer-events: none;
}

.crop-zoom {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  width: 220px;
  max-width: 100%;
  margin-top: 16px;
  color: $text-secondary;
  font-size: 11px;

  input {
    width: 100%;
    accent-color: $primary-color;
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
  width: min(820px, calc(100vw - 32px));

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
  .personal-view { gap: 16px; }

  .profile-hero {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .edit-profile-button { width: 100%; }

  .panel { padding: 18px; }

  .trace-item { flex-direction: column; gap: 8px; }

  .trace-date { flex-basis: auto; }

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

  .avatar-crop-viewport,
  .crop-zoom { width: min(220px, 72vw); }

  .avatar-actions {
    width: 100%;
  }
}
</style>
