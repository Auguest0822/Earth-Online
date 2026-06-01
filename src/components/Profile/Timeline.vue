<template>
  <div class="timeline-container module-card">
    <div class="module-header">
      <div class="title">
        <h3>我的时间轴</h3>
        <el-icon class="icon"><MagicStick /></el-icon>
      </div>
      <div class="actions">
        <el-button-group>
          <el-button type="success" plain size="small" class="active-btn">
            <el-icon><Operation /></el-icon> 时间轴视图
          </el-button>
          <el-button type="success" plain size="small">
            <el-icon><Calendar /></el-icon> 日历视图
          </el-button>
        </el-button-group>
        <el-select style="width: 100px" size="small" placeholder="全部年份">
          <el-option label="全部年份" value="all" />
          <el-option label="2025" value="2025" />
          <el-option label="2024" value="2024" />
        </el-select>
      </div>
    </div>

    <div class="timeline-wrapper">
      <div class="timeline-line"></div>
      
      <div class="timeline-item" v-for="(item, index) in timelines" :key="index">
        <div class="year-label">
          <span class="year">{{ item.year }}</span>
          <span class="status" :class="{ 'locked': item.locked }">{{ item.status }}</span>
        </div>
        
        <div class="dot" :class="{ 'locked': item.locked }"></div>
        
        <div class="content-card" :class="{ 'locked-card': item.locked }">
          <div class="card-header">
            <h4>{{ item.year }}年<span v-if="item.locked">（未完成）</span></h4>
            <div class="more-action" v-if="!item.locked">
              <span>查看详情</span>
              <el-icon><MoreFilled /></el-icon>
            </div>
            <div class="lock-icon" v-else>
              <el-icon><Lock /></el-icon>
            </div>
          </div>
          
          <div class="card-body" v-if="!item.locked">
            <div class="info-row">
              <span class="label">大事记：</span>
              <span class="value">{{ item.event }}</span>
            </div>
            <div class="info-row">
              <span class="label">关键词：</span>
              <span class="value">
                <span class="keyword" v-for="(kw, i) in item.keywords" :key="i">{{ kw }}</span>
              </span>
            </div>
            <div class="info-row">
              <span class="label">心情：</span>
              <span class="value moods">
                <span v-for="(mood, i) in item.moods" :key="i">{{ mood }}</span>
              </span>
            </div>
            
            <div class="photos">
              <img v-for="(photo, i) in item.photos" :key="i" :src="photo" />
            </div>
          </div>
          <div class="card-body locked-body" v-else>
            <p>{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </div>

    <div class="load-more">
      <el-button type="success" plain round size="small">
        <el-icon><ArrowDown /></el-icon> 查看更多年份
      </el-button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  timelines: Array
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.module-card {
  background: $card-bg;
  border-radius: $radius-base;
  padding: 24px;
  box-shadow: $shadow-base;
}

.module-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;

  .title {
    display: flex;
    align-items: center;
    gap: 8px;
    h3 { margin: 0; font-size: 18px; color: $text-main; }
    .icon { color: $primary-color; font-size: 18px; }
  }

  .actions {
    display: flex;
    gap: 12px;
    align-items: center;
    
    .active-btn {
      background-color: rgba(102, 194, 138, 0.1);
    }
  }
}

.timeline-wrapper {
  position: relative;
  padding-left: 80px;

  .timeline-line {
    position: absolute;
    left: 87px;
    top: 10px;
    bottom: 0;
    width: 2px;
    background-color: rgba(102, 194, 138, 0.3);
  }
}

.timeline-item {
  position: relative;
  margin-bottom: 40px;
  display: flex;
  min-height: 100px;

  .year-label {
    position: absolute;
    left: -80px;
    top: -2px;
    width: 60px;
    text-align: right;
    display: flex;
    flex-direction: column;
    
    .year {
      font-size: 16px;
      font-weight: bold;
      color: $text-main;
    }
    .status {
      font-size: 12px;
      color: $primary-color;
      &.locked { color: $text-secondary; }
    }
  }

  .dot {
    position: absolute;
    left: 2px; /* 87px - 80px - radius? Wait. inner relative */
    /* Wait, the container has padding-left: 80px. line is at 87px relative to wrapper */
    /* item is inside wrapper. */
    left: 3px; 
    top: 4px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    background-color: $primary-color;
    border: 2px solid #fff;
    box-shadow: 0 0 0 2px rgba(102, 194, 138, 0.3);
    z-index: 2;
    
    &.locked {
      background-color: #ddd;
      box-shadow: 0 0 0 2px rgba(221, 221, 221, 0.3);
    }
  }

  .content-card {
    margin-left: 32px;
    flex: 1;
    background: #fdfdfd;
    border: 1px solid $border-color;
    border-radius: 12px;
    padding: 20px;
    transition: all 0.3s;

    &:hover {
      box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    }
    
    &.locked-card {
      background: #fafafa;
      border: 1px dashed #eee;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      
      h4 { 
        margin: 0; font-size: 16px; color: $text-main; 
        span { color: $text-secondary; font-size: 14px; font-weight: normal; }
      }
      
      .more-action {
        font-size: 12px; color: $text-secondary;
        display: flex; align-items: center; gap: 4px; cursor: pointer;
        &:hover { color: $primary-color; }
      }
      .lock-icon {
        color: #ccc; font-size: 20px;
      }
    }

    .card-body {
      .info-row {
        margin-bottom: 10px;
        font-size: 14px;
        .label { color: $text-secondary; width: 70px; display: inline-block; }
        .value { color: $text-main; }
        
        .keyword {
          margin-right: 12px;
          color: $primary-color;
        }
        
        .moods span {
          margin-right: 8px; font-size: 16px;
        }
      }

      .photos {
        display: flex; gap: 12px; margin-top: 16px;
        img {
          width: 80px; height: 60px; object-fit: cover;
          border-radius: 8px;
        }
      }
      
      &.locked-body p {
        color: $text-secondary; font-size: 14px; margin: 0;
      }
    }
  }
}

.load-more {
  text-align: center;
  margin-top: 20px;
  
  .el-button {
    width: 200px;
  }
}
</style>
