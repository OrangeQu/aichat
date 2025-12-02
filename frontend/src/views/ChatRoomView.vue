<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { message as antdMessage } from 'ant-design-vue'

type Speaker = 'user' | 'ai'

interface ChatMessage {
  id: number
  role: Speaker
  content: string
}

interface HistoryRoom {
  id: number
  title: string
  chatMessages?: any[]
}

const route = useRoute()
const apiBase = import.meta.env.VITE_API_BASE || '/api'
const STORAGE_KEY = 'aijzw-chat-cache'

const roomId = ref<number>(generateRoomId())
const messages = ref<ChatMessage[]>([])
const inputValue = ref('')
const historyRooms = ref<HistoryRoom[]>([])
const localCache = ref<Record<number, ChatMessage[]>>({})
const startDisabled = ref(false)
const endDisabled = ref(false)
const sending = ref(false)
const greetingSent = ref(false)
const chatBodyRef = ref<HTMLElement | null>(null)

function generateRoomId() {
  return Math.floor(100000 + Math.random() * 900000)
}

const scrollToBottom = () => {
  nextTick(() => {
    const container = chatBodyRef.value
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

const persistAll = () => {
  const payload = {
    rooms: historyRooms.value,
    cache: localCache.value,
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
}

const dedupeRooms = (rooms: HistoryRoom[]) => {
  const seen = new Set<number>()
  const result: HistoryRoom[] = []
  rooms.forEach((room) => {
    if (!seen.has(room.id)) {
      seen.add(room.id)
      result.push(room)
    }
  })
  return result
}

const loadFromStorage = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return
    const parsed = JSON.parse(raw)
    if (parsed?.rooms) {
      historyRooms.value = parsed.rooms
    }
    if (parsed?.cache) {
      localCache.value = parsed.cache
    }
  } catch (e) {
    console.warn('缓存读取失败', e)
  }
}

const syncCache = () => {
  localCache.value[roomId.value] = [...messages.value]
  persistAll()
}

const pushMessage = (role: Speaker, content: string) => {
  messages.value.push({
    id: Date.now() + Math.random(),
    role,
    content,
  })
  syncCache()
  scrollToBottom()
}

const normalizeMessageFromServer = (item: any): ChatMessage | null => {
  if (!item) return null
  const roleText = item.role || item.messageRole || item.chatMessageRole
  let role: Speaker = roleText === 'assistant' ? 'ai' : 'user'
  if (roleText === 'USER' || roleText === 'user') role = 'user'
  if (roleText === 'ASSISTANT' || roleText === 'assistant') role = 'ai'
  const content = item.content ?? item.message?.content ?? ''
  return {
    id: Date.now() + Math.random(),
    role,
    content: typeof content === 'string' ? content : JSON.stringify(content),
  }
}

const setMessagesForRoom = (list: ChatMessage[]) => {
  messages.value = list
  syncCache()
  const last = list[list.length - 1]
  startDisabled.value = list.length > 0
  greetingSent.value = list.length > 0
  endDisabled.value = last ? last.content.includes('游戏已结束') : false
  scrollToBottom()
}

const fetchHistoryRooms = async () => {
  try {
    const { data } = await axios.get(`${apiBase}/rooms`)
    if (Array.isArray(data)) {
      const merged: HistoryRoom[] = []
      data.forEach((item: any, index: number) => {
        const id = Number(item?.roomId ?? item?.id ?? index + 1)
        const normalizedMessages = Array.isArray(item?.chatMessage)
          ? item.chatMessage
              .map((msg: any) => normalizeMessageFromServer(msg))
              .filter((msg: ChatMessage | null): msg is ChatMessage => Boolean(msg))
          : undefined
        const base = {
          id,
          title: `对话 ${id}`,
          chatMessages: normalizedMessages,
        }
        if (normalizedMessages && normalizedMessages.length) {
          localCache.value[id] = normalizedMessages
          if (roomId.value === id) {
            setMessagesForRoom(normalizedMessages)
          }
        }
        merged.push(base)
      })
      // 合并已有历史（如本地缓存生成的房间）保持唯一
      historyRooms.value = dedupeRooms([...historyRooms.value, ...merged])
      persistAll()
    }
  } catch (error) {
    console.error(error)
    antdMessage.warning('历史对话加载失败，稍后再试')
  }
}

const callChatApi = async (prompt: string) => {
  sending.value = true
  try {
    const { data } = await axios.post(`${apiBase}/${roomId.value}/chat`, null, {
      params: { userPrompt: prompt },
    })
    const reply = typeof data === 'string' ? data : JSON.stringify(data)
    pushMessage('ai', reply)
    startDisabled.value = true
    greetingSent.value = true
    if (reply.includes('游戏已结束')) {
      endDisabled.value = true
    }
  } catch (error) {
    console.error(error)
    antdMessage.error('与 AI 通信失败，请稍后重试')
  } finally {
    sending.value = false
  }
}

const ensureHistoryEntry = () => {
  if (!historyRooms.value.some((room) => room.id === roomId.value)) {
    historyRooms.value.unshift({
      id: roomId.value,
      title: `对话 ${roomId.value}`,
    })
    persistAll()
  }
}

const handleStart = async () => {
  if (startDisabled.value || sending.value) return
  ensureHistoryEntry()
  pushMessage('user', '开始')
  await callChatApi('开始')
}

const handleEnd = async () => {
  if (endDisabled.value || sending.value) return
  pushMessage('user', '结束')
  await callChatApi('结束')
}

const handleSend = async () => {
  const text = inputValue.value.trim()
  if (!text || sending.value) return
  inputValue.value = ''
  pushMessage('user', text)
  await callChatApi(text)
}

const loadHistoryConversation = (room: HistoryRoom) => {
  roomId.value = room.id
  const serverMessages = room.chatMessages
    ?.map((msg: any) => normalizeMessageFromServer(msg))
    .filter((msg: ChatMessage | null): msg is ChatMessage => Boolean(msg))
  const cached = localCache.value[room.id]
  if (serverMessages && serverMessages.length) {
    setMessagesForRoom(serverMessages)
  } else if (cached && cached.length) {
    setMessagesForRoom(cached)
  } else {
    setMessagesForRoom([])
  }
}

const hydrateRoomIdFromRoute = () => {
  const paramId = Number(route.query.roomId)
  if (!Number.isNaN(paramId) && paramId > 0) {
    roomId.value = paramId
  }
}

onMounted(async () => {
  loadFromStorage()
  hydrateRoomIdFromRoute()
  await fetchHistoryRooms()
  const cached = localCache.value[roomId.value]
  if (cached && cached.length) {
    setMessagesForRoom(cached)
  } else {
    setMessagesForRoom([])
  }
})
</script>

<template>
  <div class="chat-page">
    <div class="history-panel">
      <div class="history-header">历史对话</div>
      <div class="history-list">
        <a-empty v-if="!historyRooms.length" description="暂无记录" />
        <a-list
          v-else
          bordered
          :data-source="historyRooms"
          :split="false"
          item-layout="horizontal"
        >
          <template #renderItem="{ item }">
            <a-list-item class="history-item" @click="loadHistoryConversation(item)">
              <div class="history-item-title">{{ item.title }}</div>
              <div class="history-item-sub">房间号：{{ item.id }}</div>
            </a-list-item>
          </template>
        </a-list>
      </div>
    </div>

    <div class="chat-panel">
      <div class="chat-header">
        <div class="chat-title">AI 脑筋急转弯</div>
        <div class="room-pill">房间号：{{ roomId }}</div>
      </div>

      <div class="chat-body" ref="chatBodyRef">
        <div
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <div class="avatar">
            <span>{{ message.role === 'ai' ? 'AI' : '我' }}</span>
          </div>
          <div class="bubble">
            <p>{{ message.content }}</p>
          </div>
        </div>
        <div v-if="!messages.length" class="placeholder">
          <span>点击“开始”或输入后发送，开始一轮新的脑筋急转弯</span>
        </div>
      </div>

      <div class="control-bar">
        <a-space>
          <a-button type="primary" shape="round" :disabled="startDisabled" :loading="sending" @click="handleStart">
            开始
          </a-button>
          <a-button danger shape="round" :disabled="endDisabled || !greetingSent" @click="handleEnd">
            结束
          </a-button>
        </a-space>
      </div>

      <div class="input-bar">
        <a-input
          v-model:value="inputValue"
          placeholder="请输入内容"
          size="large"
          @pressEnter="handleSend"
          :disabled="endDisabled"
        />
        <a-button
          type="primary"
          size="large"
          :loading="sending"
          :disabled="endDisabled"
          @click="handleSend"
        >
          发送
        </a-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 20px;
  padding: 32px;
  min-height: 100vh;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.35), rgba(176, 199, 228, 0.55));
}

.history-panel {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(0, 39, 85, 0.08);
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 20px 60px rgba(70, 103, 150, 0.15);
  backdrop-filter: blur(10px);
}

.history-header {
  font-size: 16px;
  font-weight: 700;
  color: #1f2a3d;
  margin-bottom: 12px;
  letter-spacing: 0.5px;
}

.history-list {
  max-height: calc(100vh - 180px);
  overflow: auto;
}

.history-item {
  border-radius: 10px;
  padding: 12px;
  margin-bottom: 10px;
  background: rgba(59, 111, 184, 0.08);
  cursor: pointer;
  transition: all 0.2s ease;
}

.history-item:hover {
  background: rgba(59, 111, 184, 0.14);
  transform: translateY(-1px);
}

.history-item-title {
  color: #1f2a3d;
  font-weight: 600;
}

.history-item-sub {
  color: #5a6f92;
  font-size: 12px;
  margin-top: 2px;
}

.chat-panel {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(0, 39, 85, 0.08);
  border-radius: 16px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 24px 60px rgba(70, 103, 150, 0.2);
  backdrop-filter: blur(8px);
}

.chat-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
}

.chat-title {
  flex: 1;
  font-size: 20px;
  font-weight: 800;
  color: #1f2a3d;
  letter-spacing: 0.8px;
  text-align: center;
}

.room-pill {
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(59, 111, 184, 0.1);
  color: #1f2a3d;
  font-size: 13px;
  letter-spacing: 0.5px;
}

.chat-body {
  flex: 1;
  background: rgba(244, 248, 255, 0.9);
  border: 1px solid rgba(0, 39, 85, 0.06);
  border-radius: 12px;
  padding: 16px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-row {
  display: flex;
  align-items: flex-end;
  gap: 10px;
}

.message-row.ai {
  flex-direction: row;
}

.message-row.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 38px;
  height: 38px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #3b6fb8, #5cc4ff);
  color: #ffffff;
  font-weight: 800;
  font-size: 14px;
  box-shadow: 0 10px 25px rgba(59, 111, 184, 0.25);
}

.message-row.user .avatar {
  background: linear-gradient(135deg, #ffad5c, #ff7c6b);
  box-shadow: 0 10px 25px rgba(255, 138, 114, 0.25);
}

.bubble {
  max-width: 75%;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(59, 111, 184, 0.08);
  color: #1f2a3d;
  border: 1px solid rgba(59, 111, 184, 0.12);
  line-height: 1.6;
}

.message-row.user .bubble {
  background: rgba(255, 173, 92, 0.16);
  border-color: rgba(255, 173, 92, 0.22);
}

.bubble p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.placeholder {
  color: #5a6f92;
  text-align: center;
  padding: 24px 0;
}

.control-bar {
  display: flex;
  justify-content: flex-start;
  gap: 12px;
  margin: 16px 0 10px;
}

.input-bar {
  display: grid;
  grid-template-columns: 1fr 120px;
  gap: 12px;
  align-items: center;
}

@media (max-width: 960px) {
  .chat-page {
    grid-template-columns: 1fr;
    padding: 18px;
  }

  .history-panel {
    order: 2;
  }

  .input-bar {
    grid-template-columns: 1fr;
  }
}
</style>
