<template>
  <div class="word-cloud-card module-card" :class="{ embedded }">
    <div class="module-header">
      <div><h3>关键词云</h3><p v-if="sortedKeywords.length">从记录中发现 {{ sortedKeywords.length }} 个关键词</p></div>
      <span v-if="sortedKeywords.length" class="keyword-total">累计 {{ totalCount }} 次</span>
    </div>
    <div v-if="sortedKeywords.length" class="keyword-content">
      <div ref="cloudRef" class="cloud-container" aria-label="关键词云">
        <button
          v-for="item in cloudWords"
          :key="item.name"
          type="button"
          class="keyword"
          :class="{ featured: item.position <= 3 }"
          :style="{
            left: `${item.left}px`,
            top: `${item.top}px`,
            fontSize: `${item.size}px`,
            color: item.color,
            fontWeight: item.position <= 3 ? 750 : 600
          }"
          :title="`${item.name}：出现 ${item.value} 次，占比 ${item.percentage}%`"
        >
          <span>{{ item.name }}</span>
        </button>
      </div>
      <aside class="keyword-ranking">
        <div class="ranking-title"><strong>热门关键词</strong><span>TOP {{ topKeywords.length }}</span></div>
        <ol>
          <li v-for="item in topKeywords" :key="item.name">
            <span class="rank-number">{{ String(item.position).padStart(2, '0') }}</span>
            <div class="rank-detail">
              <div><strong>{{ item.name }}</strong><span>{{ item.value }} 次</span></div>
              <div class="rank-track"><i :style="{ width: `${item.relativeValue * 100}%` }"></i></div>
            </div>
          </li>
        </ol>
      </aside>
    </div>
    <div v-else class="empty-keywords">
      <span>✦</span><strong>还没有关键词</strong>
      <p>为时间线记录添加关键词后，这里会生成你的年度关键词云。</p>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  keywords: { type: Array, default: () => [] },
  embedded: Boolean
})

const cloudRef = ref(null)
const cloudBox = ref({ width: 0, height: 0 })
let cloudObserver = null

const normalizedKeywords = computed(() => props.keywords
  .map(item => ({ name: String(item?.name || '').trim(), value: Math.max(0, Number(item?.value) || 0) }))
  .filter(item => item.name))
const totalCount = computed(() => normalizedKeywords.value.reduce((sum, item) => sum + item.value, 0))
const sortedKeywords = computed(() => {
  const sorted = [...normalizedKeywords.value].sort((a, b) => b.value - a.value || a.name.localeCompare(b.name, 'zh-CN'))
  const max = sorted[0]?.value || 1
  const min = sorted.at(-1)?.value || 0
  const span = max - min
  return sorted.map((item, index) => {
    const rank = sorted.findIndex(candidate => candidate.value === item.value) + 1
    return {
      ...item,
      rank,
      position: index + 1,
      relativeValue: span ? 0.35 + ((item.value - min) / span) * 0.65 : 1,
      percentage: totalCount.value ? Math.round((item.value / totalCount.value) * 100) : 0
    }
  })
})
const topKeywords = computed(() => sortedKeywords.value.slice(0, 5))
const colors = ['#245c40', '#317a53', '#3f9062', '#4f9d70', '#397357']

const WORD_LIMIT = 24

// 固定种子的伪随机：同一组关键词每次布局都一样，避免组件更新时文字乱跳
const createRandom = seed => {
  let state = seed >>> 0 || 1
  return () => {
    state = (state * 1664525 + 1013904223) >>> 0
    return state / 4294967296
  }
}
const hashString = value => {
  let hash = 2166136261
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index)
    hash = Math.imul(hash, 16777619)
  }
  return hash >>> 0
}

// 估算文字宽度（中文约 1em、其他约 0.56em），用于碰撞检测
const estimateWidth = (text, size) => {
  let units = 0
  for (const char of text) units += /[\u3000-\u9fff\uff00-\uffef]/.test(char) ? 1 : 0.56
  return units * size
}
const overlapArea = (a, b) => {
  const width = Math.min(a.left + a.width, b.left + b.width) - Math.max(a.left, b.left)
  const height = Math.min(a.top + a.height, b.top + b.height) - Math.max(a.top, b.top)
  return width > 0 && height > 0 ? width * height : 0
}

const WORD_GAP = 8
const WORD_PADDING = 10
const MIN_WORD_SIZE = 11

const measureWord = (name, size) => ({
  width: Math.ceil(estimateWidth(name, size)) + WORD_GAP * 2,
  height: Math.ceil(size * 1.5)
})

// 随机撒点找一个不压住任何已放置词的位置；找不到就返回“压得最少”的那个
// 注意：placed 里的元素必须是 { left, top, width, height }，overlapArea 依赖这四个字段
const findFreeSpot = (name, size, placed, width, height, random, attempts = 400) => {
  const box = measureWord(name, size)
  const maxLeft = Math.max(WORD_PADDING, width - box.width - WORD_PADDING)
  const maxTop = Math.max(WORD_PADDING, height - box.height - WORD_PADDING)
  const candidate = { left: 0, top: 0, width: box.width, height: box.height }
  let best = null

  for (let attempt = 0; attempt < attempts; attempt += 1) {
    candidate.left = WORD_PADDING + random() * Math.max(0, maxLeft - WORD_PADDING)
    candidate.top = WORD_PADDING + random() * Math.max(0, maxTop - WORD_PADDING)
    const overlap = placed.reduce((sum, rect) => sum + overlapArea(candidate, rect), 0)
    if (overlap === 0) {
      return { size, left: candidate.left, top: candidate.top, width: box.width, height: box.height, overlap: 0 }
    }
    if (!best || overlap < best.overlap) {
      best = { size, left: candidate.left, top: candidate.top, width: box.width, height: box.height, overlap }
    }
  }
  return best
}

/**
 * 关键词云布局：
 * - 字号由出现次数决定（次数越多字越大）；
 * - 权重最高的词放正中间，其余词随机散布，并用碰撞检测避免互相压住；
 * - 关键字太多时先整体缩字号，仍放不下的词就不显示（保证“字越大=出现越多”不被破坏）。
 */
const layoutCloud = (words, width, height) => {
  const narrow = width < 420
  const maxSize = narrow ? 30 : 38
  const minSize = narrow ? 14 : 16
  const gap = WORD_GAP
  let items = words.map(item => ({
    ...item,
    size: Math.round(minSize + item.relativeValue * (maxSize - minSize))
  }))

  const area = width * height
  const occupied = items.reduce((sum, item) => {
    const box = measureWord(item.name, item.size)
    return sum + box.width * box.height
  }, 0)
  let shrunk = false
  if (occupied > area * 0.45) {
    shrunk = true
    const scale = Math.max(0.55, Math.sqrt((area * 0.45) / occupied))
    items = items.map(item => ({ ...item, size: Math.max(MIN_WORD_SIZE, Math.round(item.size * scale)) }))
  }

  const random = createRandom(hashString(items.map(item => `${item.name}:${item.value}`).join('|')))
  const placed = []
  const results = []
  const skipped = []

  items.forEach((item, index) => {
    if (index === 0) {
      // 权重最高的词放正中间；框比容器还大时先缩小到放得下
      let size = item.size
      let box = measureWord(item.name, size)
      while (size > MIN_WORD_SIZE && (box.width > width - WORD_PADDING * 2 || box.height > height - WORD_PADDING * 2)) {
        size -= 1
        box = measureWord(item.name, size)
      }
      const spot = {
        size,
        left: Math.max(0, (width - box.width) / 2),
        top: Math.max(0, (height - box.height) / 2),
        width: box.width,
        height: box.height
      }
      placed.push(spot)
      results.push({ item, spot })
      return
    }

    // 放不下就跳过这个词，而不是把它缩小——否则“字越大=出现越多”的语义会被破坏
    const spot = findFreeSpot(item.name, item.size, placed, width, height, random)
    if (spot.overlap === 0) {
      placed.push(spot)
      results.push({ item, spot })
    } else {
      skipped.push(item)
    }
  })

  // 极端拥挤时的兜底：宁可个别词轻微贴边，也不要只剩两三个词
  if (results.length < Math.min(5, items.length)) {
    skipped.forEach(item => {
      const spot = findFreeSpot(item.name, item.size, placed, width, height, random)
      placed.push(spot)
      results.push({ item, spot })
    })
  }

  return {
    shrunk,
    words: results.map(({ item, spot }) => ({
      ...item,
      size: spot.size,
      left: Math.round(spot.left),
      top: Math.round(spot.top),
      color: colors[(item.position - 1) % colors.length]
    }))
  }
}

// 关键词云布局：结果只依赖"关键词 + 容器尺寸"，因此可以放进 computed 里
const cloudWords = computed(() => {
  const { width, height } = cloudBox.value
  const words = sortedKeywords.value.slice(0, WORD_LIMIT)
  if (!width || !height || !words.length) return []
  return layoutCloud(words, width, height).words
})

const measureCloud = () => {
  const element = cloudRef.value
  if (!element) return
  const width = element.clientWidth
  const height = element.clientHeight
  if (width !== cloudBox.value.width || height !== cloudBox.value.height) {
    cloudBox.value = { width, height }
  }
}

// 容器随页面宽度变化（竖屏/横屏、桌面缩放）时重新排布
watch(cloudRef, element => {
  cloudObserver?.disconnect()
  cloudObserver = null
  if (!element) return
  measureCloud()
  if (typeof ResizeObserver !== 'undefined') {
    cloudObserver = new ResizeObserver(measureCloud)
    cloudObserver.observe(element)
  }
})

onBeforeUnmount(() => {
  cloudObserver?.disconnect()
  cloudObserver = null
  window.removeEventListener('resize', measureCloud)
})

if (typeof window !== 'undefined') window.addEventListener('resize', measureCloud)
</script>

<style lang="scss">
@use '@/styles/variables' as *;

.word-cloud-card.module-card { box-sizing: border-box; height: 100%; padding: 24px; background: $card-bg; border-radius: $radius-base; box-shadow: $shadow-base; }
.word-cloud-card .module-header {
  display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 22px;
  h3 { margin: 0; color: $text-main; font-size: 20px; }
  p { margin: 7px 0 0; color: #66766e; font-size: 13px; }
}
.word-cloud-card .keyword-total { flex: 0 0 auto; padding: 7px 12px; color: #317a53; background: #eaf7ef; border: 1px solid #d4eddd; border-radius: 999px; font-size: 12px; font-weight: 650; }
.word-cloud-card .keyword-content { display: grid; grid-template-columns: minmax(0, 1.8fr) minmax(230px, .7fr); gap: 24px; min-height: 270px; }
.word-cloud-card .cloud-container {
  position: relative; overflow: hidden;
  min-height: 300px; padding: 0;
  background: radial-gradient(circle at 22% 30%, rgba(102,194,138,.13), transparent 34%), radial-gradient(circle at 80% 72%, rgba(129,207,157,.11), transparent 30%), #fbfdfb;
  border: 1px solid #e4eee7; border-radius: 18px;
}
.word-cloud-card .keyword {
  position: absolute; display: inline-flex; align-items: center; justify-content: center; padding: 2px 8px; background: transparent;
  border: 1px solid transparent; border-radius: 12px;
  line-height: 1.2; white-space: nowrap; cursor: default; transition: transform .2s ease, color .2s ease, background .2s ease, box-shadow .2s ease;
  &.featured { background: linear-gradient(135deg, rgba(102,194,138,.2), rgba(225,246,233,.82)); border-color: rgba(74,166,108,.2); box-shadow: 0 7px 18px rgba(63,144,98,.09); }
  &:hover { z-index: 2; color: #174b32 !important; background: #fff; border-color: rgba(49,122,83,.25); box-shadow: 0 10px 24px rgba(45,108,75,.14); transform: translateY(-3px) scale(1.04); }
}
.word-cloud-card .keyword-ranking { padding: 20px; background: linear-gradient(180deg, #f5fbf7, #fbfdfb); border: 1px solid #dfede4; border-radius: 18px; }
.word-cloud-card .ranking-title {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px;
  strong { color: #2d4638; font-size: 15px; }
  span { color: #56906d; font-size: 11px; font-weight: 700; letter-spacing: .06em; }
}
.word-cloud-card .keyword-ranking ol { display: grid; gap: 17px; margin: 0; padding: 0; list-style: none; }
.word-cloud-card .keyword-ranking li { display: flex; align-items: center; gap: 10px; }
.word-cloud-card .rank-number { width: 34px; color: #70a184; font-size: 12px; font-weight: 750; }
.word-cloud-card .rank-detail {
  flex: 1; min-width: 0;
  > div:first-child { display: flex; justify-content: space-between; gap: 10px; margin-bottom: 7px; }
  strong { overflow: hidden; color: #31473a; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }
  span { flex: 0 0 auto; color: #688075; font-size: 12px; }
}
.word-cloud-card .rank-track { height: 5px; overflow: hidden; background: #deebe2; border-radius: 999px; i { display: block; height: 100%; background: linear-gradient(90deg, #58b77b, #8bd5a5); border-radius: inherit; } }
.word-cloud-card .empty-keywords {
  display: flex; min-height: 260px; align-items: center; justify-content: center; flex-direction: column; color: #809087;
  background: #fbfdfb; border: 1px dashed #d9e9df; border-radius: 18px;
  > span { margin-bottom: 10px; color: #65bd85; font-size: 30px; }
  strong { color: #405448; font-size: 16px; }
  p { margin: 8px 20px 0; font-size: 13px; text-align: center; }
}
.word-cloud-card.embedded { min-height: clamp(360px, 32vw, 430px); padding: clamp(20px, 2vw, 30px); background: #fafafa; box-shadow: none; }
@media (max-width: 900px) { .word-cloud-card .keyword-content { grid-template-columns: 1fr; } .word-cloud-card .keyword-ranking { display: none; } }
@media (max-width: 560px) {
  .word-cloud-card .module-header { align-items: flex-start; }
  .word-cloud-card .keyword-total { display: none; }
  .word-cloud-card .cloud-container { min-height: 260px; }
  .word-cloud-card.embedded { min-height: 300px; }
}
</style>
