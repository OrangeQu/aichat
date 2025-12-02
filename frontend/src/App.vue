<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-shell">
    <RouterView />
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: radial-gradient(circle at 25% 20%, rgba(255, 255, 255, 0.35), transparent 35%),
    radial-gradient(circle at 80% 5%, rgba(255, 255, 255, 0.28), transparent 40%),
    linear-gradient(135deg, #d7e6ff, #b8cff1 55%, #9ebbdc);
  color: #1b2b3f;
}
</style>
