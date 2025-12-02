import { createRouter, createWebHistory } from 'vue-router'
import LandingView from '../views/HomeView.vue'
import ChatRoomView from '../views/ChatRoomView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: LandingView,
    },
    {
      path: '/chat',
      name: 'chat',
      component: ChatRoomView,
    },
  ],
})

export default router
