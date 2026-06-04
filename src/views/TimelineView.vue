<template>
  <div class="timeline-page" v-if="profileStore.userInfo">
    <Timeline :timelines="profileStore.timelines" />
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useProfileStore } from '@/store/profile'
import Timeline from '@/components/Profile/Timeline.vue'

const profileStore = useProfileStore()

onMounted(() => {
  profileStore.fetchProfileData()
})
</script>

<style lang="scss" scoped>
@import '@/assets/styles/variables.scss';

.timeline-page {
  padding: 0;
}
</style>
