<template>
  <div :class="['message', { 'user-message': message.sender === 'user' }]">
    <div class="message-content">
      <div class="message-sender">
        {{ message.sender === 'user' ? '你' : 'AI' }}
      </div>
      <div class="message-text">{{ message.content }}</div>
      <div class="message-time">{{ message.time }}</div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from 'vue'
import type { PropType } from 'vue'

interface Message {
  content: string
  sender: 'user' | 'ai'
  time: string
}

export default defineComponent({
  props: {
    message: {
      type: Object as PropType<Message>,
      required: true
    }
  }
})
</script>

<style scoped>
.message {
  margin: 10px 0;
  padding: 10px;
  border-radius: 8px;
  max-width: 80%;
}

.message-content {
  display: flex;
  flex-direction: column;
}

.user-message {
  margin-left: auto;
  background-color: #e3f2fd;
  align-self: flex-end;
}

.message-sender {
  font-weight: bold;
  margin-bottom: 4px;
}

.message-text {
  flex-grow: 1;
}

.message-time {
  font-size: 0.7em;
  color: #888;
  margin-top: 4px;
}
</style>
