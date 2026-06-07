<template>
  <section class="earth-event-feed module-card">
    <div class="module-header">
      <div>
        <p class="eyebrow">地球大事件</p>
        <h3>正在发生</h3>
      </div>
      <span class="source-label">{{ sourceLabel }}</span>
    </div>

    <div v-loading="loading" class="event-list">
      <article v-for="event in events" :key="event.id" class="event-card">
        <a
          :href="event.url"
          target="_blank"
          rel="noreferrer"
          class="event-image"
          :class="{ disabled: !isExternalLink(event.url) }"
          @click="handleOpen($event, event)"
        >
          <img :src="event.image" :alt="event.title" loading="lazy" />
        </a>

        <div class="event-content">
          <div class="event-topline">
            <div class="event-tags">
              <span>{{ event.category }}</span>
              <span>{{ event.region }}</span>
            </div>
            <div class="importance">
              <strong>{{ event.importance }}</strong>
              <span>热度</span>
            </div>
          </div>

          <a
            :href="event.url"
            target="_blank"
            rel="noreferrer"
            class="event-title"
            @click="handleOpen($event, event)"
          >
            {{ event.title }}
          </a>
          <p>{{ event.summary }}</p>

          <div class="event-meta">
            <span>{{ event.source }}</span>
            <span>{{ formatTime(event.publishedAt) }}</span>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'

defineProps({
  events: {
    type: Array,
    default: () => []
  },
  loading: Boolean,
  sourceLabel: {
    type: String,
    default: 'GDELT'
  }
})

const isExternalLink = (url) => /^https?:\/\//.test(url || '')

const handleOpen = (event, item) => {
  if (isExternalLink(item.url)) return

  event.preventDefault()
  ElMessage.info('这条是演示新闻，接入真实新闻源后会跳转到原文页面')
}

const formatTime = (value) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '刚刚'

  const diffMinutes = Math.max(0, Math.floor((Date.now() - date.getTime()) / 60000))
  if (diffMinutes < 60) return `${diffMinutes || 1} 分钟前`
  if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)} 小时前`
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.module-card {
  background: rgba(255, 255, 255, 0.94);
  border-radius: $radius-base;
  padding: 24px;
  box-shadow: $shadow-base;
}

.module-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  .eyebrow {
    margin: 0 0 4px;
    color: $primary-color;
    font-size: 13px;
  }

  h3 {
    margin: 0;
    color: $text-main;
    font-size: 18px;
  }
}

.source-label {
  flex: 0 0 auto;
  padding: 6px 10px;
  border-radius: 999px;
  color: $primary-color;
  background: rgba(102, 194, 138, 0.1);
  font-size: 12px;
}

.event-list {
  min-height: 360px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.event-card {
  min-width: 0;
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 18px;
  align-items: stretch;
  border: 1px solid rgba(235, 238, 245, 0.95);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.84);
  transition: box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 10px 24px rgba(102, 194, 138, 0.12);
  }
}

.event-image {
  display: block;
  width: 100%;
  height: 132px;
  overflow: hidden;
  border-radius: 8px 0 0 8px;
  background: linear-gradient(135deg, rgba(102, 194, 138, 0.18), rgba(160, 198, 255, 0.2));

  &.disabled {
    cursor: default;
  }

  img {
    width: 100%;
    height: 100%;
    display: block;
    object-fit: cover;
  }
}

.event-content {
  min-width: 0;
  padding: 16px 16px 16px 0;
}

.event-topline {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.event-tags {
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  span {
    color: $primary-color;
    font-size: 12px;
  }
}

.event-title {
  display: -webkit-box;
  overflow: hidden;
  color: $text-main;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.45;
  text-decoration: none;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;

  &:hover {
    color: $primary-color;
  }
}

p {
  display: -webkit-box;
  overflow: hidden;
  margin: 8px 0 10px;
  color: $text-regular;
  font-size: 13px;
  line-height: 1.7;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.event-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: $text-secondary;
  font-size: 12px;
}

.importance {
  flex: 0 0 auto;
  width: 52px;
  height: 52px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: $primary-color;
  background: rgba(102, 194, 138, 0.11);

  strong {
    font-size: 17px;
    line-height: 1;
  }

  span {
    margin-top: 4px;
    font-size: 11px;
  }
}

@media (max-width: 920px) {
  .event-card {
    grid-template-columns: 150px minmax(0, 1fr);
  }

  .event-image {
    height: 120px;
  }
}

@media (max-width: 760px) {
  .event-card {
    grid-template-columns: 1fr;
  }

  .event-image {
    height: 180px;
    border-radius: 8px 8px 0 0;
  }

  .event-content {
    padding: 14px;
  }

  .importance {
    display: none;
  }
}
</style>
