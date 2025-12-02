<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const handleStart = () => {
  const roomId = Math.floor(100000 + Math.random() * 900000)
  router.push({ name: 'chat', query: { roomId } })
}
</script>

<template>
  <div class="landing">
    <div class="hero-card">
      <div class="eyebrow">Brain Teaser</div>
      <h1>AI 脑筋急转弯</h1>
      <p class="sub">
        点击开始，一起进入神秘的故事汤面。等待你的发问，发现隐藏的真相。
      </p>
      <a-button type="primary" size="large" shape="round" @click="handleStart">
        开始游戏
      </a-button>
    </div>
  </div>
</template>

<style scoped>
.landing {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 40px 16px;
  color: #f4f7fb;
}

.hero-card {
  text-align: center;
  background: rgba(9, 21, 38, 0.75);
  border-radius: 20px;
  padding: 60px 50px;
  max-width: 540px;
  width: min(90vw, 540px);
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(12px);
}

.eyebrow {
  letter-spacing: 5px;
  text-transform: uppercase;
  color: #8ba8ff;
  font-size: 12px;
  margin-bottom: 12px;
}

h1 {
  font-size: 34px;
  margin: 0;
  color: #ffffff;
  letter-spacing: 1px;
}

.sub {
  margin: 18px 0 26px;
  color: #c6d6ff;
  line-height: 1.7;
}
</style>
