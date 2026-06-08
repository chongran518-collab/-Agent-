import { createStore } from 'vuex'
import type { Commit } from 'vuex'
import axios from 'axios'

interface Message {
  content: string
  sender: 'user' | 'ai'
  time: string
}

interface State {
  chatId: string
  messages: Message[]
  eventSource: EventSource | null
}


export default createStore({
  state: {
    chatId: '',
    messages: [],
    eventSource: null
  } as State,
  mutations: {
    setChatId(state: State, chatId: string) {
      state.chatId = chatId
    },
    addMessage(state: State, message: Message) {
      state.messages.push(message)
    },
    clearMessages(state: State) {
      state.messages = []
    },
    setEventSource(state: State, eventSource: EventSource | null) {
      state.eventSource = eventSource
    }
  },
  actions: {
    generateChatId({ commit }: { commit: Commit }) {
      const randomId = Math.random().toString(36).substring(2, 15)
      commit('setChatId', randomId)
    },
    async sendMessage({ state, commit }: { state: State; commit: Commit }, message: string) {
      const now = new Date()
      const timeString = now.toLocaleTimeString()

      commit('addMessage', {
        content: message,
        sender: 'user',
        time: timeString
      })

      if (state.eventSource) {
        state.eventSource.close()
      }

      const sseUrl = `http://localhost:8123/api/ai/audit_app/chat/sse?message=${encodeURIComponent(message)}&chatId=${state.chatId}`

      const eventSource = new EventSource(sseUrl)

      eventSource.onmessage = (e) => {
        try {
          const data = JSON.parse(e.data)
          if (data.content) {
            commit('addMessage', {
              content: data.content,
              sender: 'ai',
              time: timeString
            })
          }
        } catch (error) {
          console.error('Error parsing SSE message:', error)
        }
      }

      eventSource.onerror = (error) => {
        console.error('SSE error:', error)
      }

      commit('setEventSource', eventSource)
    }
  }
})
