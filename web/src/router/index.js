import { createRouter, createWebHistory } from 'vue-router'
import { getAccessToken } from '../api/httpClient'

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
      component: () => import('../views/ProfileView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/earth',
      name: 'earth',
      component: () => import('../views/EarthView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/PersonalView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/timeline',
      name: 'timeline',
      component: () => import('../views/TimelineView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/yearly',
      name: 'yearly',
      component: () => import('../views/YearlyView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/future-letters',
      name: 'future-letters',
      component: () => import('../views/Future_letters.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/friends',
      name: 'friends',
      component: () => import('../views/FriendsView.vue'),
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  const token = getAccessToken()

  if (to.name === 'login' && typeof to.query.redirect === 'string' && to.query.redirect.startsWith('/login')) {
    return { name: 'login', query: to.query.expired === '1' ? { expired: '1' } : {} }
  }

  if (to.meta.requiresAuth && !token) {
    return { name: 'login' }
  }

  if (to.name === 'login' && token) {
    return { name: 'home' }
  }
})

export default router
