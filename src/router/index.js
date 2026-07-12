import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { layout: 'blank' }
    },
    {
      path: '/',
      name: 'home',
      component: () => import('../views/ProfileView.vue')
    },
    {
      path: '/earth',
      name: 'earth',
      component: () => import('../views/EarthView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/PersonalView.vue')
    },
    {
      path: '/timeline',
      name: 'timeline',
      component: () => import('../views/TimelineView.vue')
    },
    {
      path: '/yearly',
      name: 'yearly',
      component: () => import('../views/YearlyView.vue')
    },
    {
      path: '/capsule',
      name: 'capsule',
      component: () => import('../views/CapsuleView.vue')
    },
    {
      path: '/friends',
      name: 'friends',
      component: () => import('../views/FriendsView.vue')
    }
  ]
})

export default router
