<template>
  <div class="word-cloud-card module-card" :class="{ embedded }">
    <div class="module-header">
      <div><h3>关键词云</h3><p v-if="sortedKeywords.length">从记录中发现 {{ sortedKeywords.length }} 个关键词</p></div>
      <span v-if="sortedKeywords.length" class="keyword-total">累计 {{ totalCount }} 次</span>
    </div>
    <div v-if="sortedKeywords.length" class="keyword-content">
      <div class="cloud-container" aria-label="关键词云">
        <button v-for="item in sortedKeywords" :key="item.name" type="button" class="keyword"
          :class="{ featured: item.position <= 3 }" :style="getWordStyle(item)"
          :title="`${item.name}：出现 ${item.value} 次，占比 ${item.percentage}%`">
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
import { computed } from 'vue'

const props = defineProps({
  keywords: { type: Array, default: () => [] },
  embedded: Boolean
})

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
const getWordStyle = item => ({
  '--keyword-size': `${Math.round(16 + item.relativeValue * 16)}px`,
  '--keyword-color': colors[(item.position - 1) % colors.length],
  '--keyword-weight': item.position <= 3 ? 750 : 600
})
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
  display: flex; align-content: center; align-items: center; justify-content: center; flex-wrap: wrap; gap: 14px 18px;
  min-height: 260px; padding: 24px; overflow: hidden;
  background: radial-gradient(circle at 22% 30%, rgba(102,194,138,.13), transparent 34%), radial-gradient(circle at 80% 72%, rgba(129,207,157,.11), transparent 30%), #fbfdfb;
  border: 1px solid #e4eee7; border-radius: 18px;
}
.word-cloud-card .keyword {
  display: inline-flex; align-items: baseline; gap: 6px; padding: 7px 9px; color: var(--keyword-color); background: transparent;
  border: 1px solid transparent; border-radius: 12px; font-size: var(--keyword-size); font-weight: var(--keyword-weight);
  line-height: 1.15; white-space: nowrap; cursor: default; transition: transform .2s ease, color .2s ease, background .2s ease, box-shadow .2s ease;
  &.featured { padding: 10px 14px; background: linear-gradient(135deg, rgba(102,194,138,.2), rgba(225,246,233,.82)); border-color: rgba(74,166,108,.2); box-shadow: 0 7px 18px rgba(63,144,98,.09); }
  &:hover { z-index: 2; color: #174b32; background: #fff; border-color: rgba(49,122,83,.25); box-shadow: 0 10px 24px rgba(45,108,75,.14); transform: translateY(-3px) scale(1.04); }
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
  .word-cloud-card .cloud-container { gap: 10px; min-height: 220px; padding: 16px; }
  .word-cloud-card .keyword { --keyword-size: 17px !important; }
  .word-cloud-card .keyword.featured { font-size: 20px; }
}
</style>
