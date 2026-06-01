<template>
  <div class="placeholder-view">
    <div class="card">
      <el-icon class="icon"><Calendar /></el-icon>
      <h2>年度记录</h2>
      <p>年度记录页面（待开发）</p>
    </div>
  </div>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.placeholder-view {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  
  .card {
    text-align: center;
    .icon { font-size: 64px; color: #66C28A; }
    h2 { color: #333; margin: 16px 0 8px; }
    p { color: #999; }
  }
}
</style>
