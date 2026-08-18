<template>
  <div class="word-cloud-card module-card" :class="{ embedded }">
    <div class="module-header">
      <div class="title">
        <h3>关键词云</h3>
      </div>
    </div>
    <div class="cloud-container">
      <span 
        v-for="(item, index) in keywords" 
        :key="index"
        class="keyword"
        :style="getWordStyle(item, index)"
      >
        {{ item.name }}
      </span>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  keywords: {
    type: Array,
    default: () => []
  },
  embedded: Boolean
})

// Simple fake word cloud generator
const getWordStyle = (item, index) => {
  const size = 12 + (item.value / 100) * 16 // 12px to 28px
  const opacity = 0.5 + (item.value / 100) * 0.5
  
  // Spread them out roughly
  const top = `${Math.floor(Math.random() * 70)}%`
  const left = `${Math.floor(Math.random() * 70)}%`
  
  const colors = ['#66C28A', '#9ED8B3', '#51A874']

  return {
    fontSize: `${size}px`,
    opacity: opacity,
    color: colors[index % colors.length],
    fontWeight: item.value > 70 ? 'bold' : 'normal',
    position: 'absolute',
    top,
    left,
    transform: `translate(-50%, -50%) rotate(${Math.random() > 0.5 ? 0 : (Math.random() * 20 - 10)}deg)`
  }
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.module-card {
  background: $card-bg;
  border-radius: $radius-base;
  padding: 24px;
  box-shadow: $shadow-base;
  height: 100%;
}

.module-header {
  margin-bottom: 20px;
  .title {
    h3 { margin: 0; font-size: 16px; color: $text-main; }
  }
}

.cloud-container {
  position: relative;
  height: clamp(180px, 22vw, 260px);
  width: 100%;
  overflow: hidden;
  
  .keyword {
    white-space: nowrap;
    transition: all 0.3s;
    &:hover {
      transform: scale(1.1) !important;
      z-index: 10;
      opacity: 1 !important;
    }
  }
}

.word-cloud-card.embedded {
  box-sizing: border-box;
  min-height: clamp(330px, 30vw, 390px);
  padding: clamp(18px, 2vw, 28px);
  box-shadow: none;
  background: #fafafa;
}
</style>
