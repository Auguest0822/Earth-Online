<template>
  <div class="profile-view" v-if="profileStore.userInfo">
    <!-- Top Role Panel -->
    <RolePanel :userInfo="profileStore.userInfo" />

    <div class="main-content">
      <!-- Left Column: Timeline -->
      <div class="left-column">
        <Timeline :timelines="profileStore.timelines" />
      </div>

      <!-- Right Column: Data Analysis, Time Capsule, Achievement -->
      <div class="right-column">
        <DataAnalysis 
          :eventsData="profileStore.eventsData" 
          :moodTrend="profileStore.moodTrend" 
        />
        
        <div class="mid-row">
          <WordCloudCard :keywords="profileStore.keywords" />
          <TimeCapsule :capsules="profileStore.capsules" />
        </div>

        <Achievement :achievements="profileStore.achievements" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useProfileStore } from '@/store/profile'
import RolePanel from '@/components/Profile/RolePanel.vue'
import Timeline from '@/components/Profile/Timeline.vue'
import DataAnalysis from '@/components/Profile/DataAnalysis.vue'
import WordCloudCard from '@/components/Profile/WordCloudCard.vue'
import TimeCapsule from '@/components/Profile/TimeCapsule.vue'
import Achievement from '@/components/Profile/Achievement.vue'

const profileStore = useProfileStore()

onMounted(() => {
  profileStore.fetchProfileData()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.profile-view {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.main-content {
  display: flex;
  gap: 24px;
  
  .left-column {
    flex: 0 0 calc(65% - 12px);
    display: flex;
    flex-direction: column;
  }

  .right-column {
    flex: 0 0 calc(35% - 12px);
    display: flex;
    flex-direction: column;
    gap: 24px;
  }
}

.mid-row {
  display: flex;
  gap: 24px;
  
  > * {
    flex: 1;
    min-width: 0;
  }
}

@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
    
    .left-column, .right-column {
      flex: 1 1 auto;
    }
  }
}
</style>
