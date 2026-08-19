<template>
  <div class="timeline-page" v-if="profileStore.userInfo">
    <Timeline :timelines="profileStore.timelines" :target-record-id="targetRecordId" />
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useProfileStore } from '@/stores/profile'
import Timeline from '@/components/Profile/Timeline.vue'

const profileStore = useProfileStore()
const route = useRoute()
const targetRecordId = computed(() => route.query.record ? String(route.query.record) : '')

onMounted(() => {
  profileStore.fetchProfileData()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';

.timeline-page {
  padding: 0;
}
</style>
