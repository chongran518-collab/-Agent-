import axios from 'axios'

// 根据环境变量设置 API 基础 URL
const API_BASE_URL = process.env.NODE_ENV === 'production'
  ? '/api' // 生产环境使用相对路径，适用于前后端部署在同一域名下
  : 'http://localhost:8123/api' // 开发环境指向本地后端服务

// 创建axios实例
const request = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000
})

// 文件上传API
const uploadFile = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const response = await request.post('/ai/audit_app/generateword/file', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  } catch (error) {
    console.error('文件上传失败:', error)
    throw error
  }
}

// 封装SSE连接
export const connectSSE = (url, params, onMessage, onError) => {
  // 构建带参数的URL
  const queryString = Object.keys(params)
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')

  const fullUrl = `${API_BASE_URL}${url}?${queryString}`

  // 创建EventSource
  const eventSource = new EventSource(fullUrl)

  eventSource.onmessage = event => {
    let data = event.data

    // 检查是否是特殊标记
    if (data === '[DONE]') {
      if (onMessage) onMessage('[DONE]')
    } else {
      // 处理普通消息
      if (onMessage) onMessage(data)
    }
  }

  eventSource.onerror = error => {
    if (onError) onError(error)
    eventSource.close()
  }

  // 返回eventSource实例，以便后续可以关闭连接
  return eventSource
}

// AI恋爱大师聊天
export const chatWithAuditApp = (message, chatId) => {
  return connectSSE('/ai/audit_app/chat/sse', { message, chatId })
}

// 带文件的AI恋爱大师聊天
export const chatWithAuditAppWithFile = async (message, chatId, file) => {
  // 先上传文件
  const filePath = await uploadFile(file)
  // 然后发送消息，包含文件路径
  const newMessage = `${message}\n\n[文件上传]：${file.name}`
  return connectSSE('/ai/audit_app/chat/sse/file', { message: newMessage, chatId, filePath })
}

// 带文件的AI超级智能体聊天
export const chatWithManusWithFile = async (message, file) => {
  // 先上传文件
  const filePath = await uploadFile(file)
  // 然后发送消息，包含文件路径
  const newMessage = `${message}\n\n[文件上传]：${file.name}`
  return connectSSE('/ai/manus/chat', { message: newMessage, filePath })
}

// AI超级智能体聊天
export const chatWithManus = (message) => {
  return connectSSE('/ai/manus/chat', { message })
}

// 表格上传API
export const uploadTable = async (file) => {
  const formData = new FormData()
  formData.append('file', file)
  
  try {
    const response = await request.post('/ai/audit_app/upload_table', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    return response.data
  } catch (error) {
    console.error('表格上传失败:', error)
    throw error
  }
}

// SQL查询API
export const executeSql = async (sql) => {
  try {
    const response = await request.post('/ai/audit_app/execute_sql', {
      sql
    })
    return response.data
  } catch (error) {
    console.error('SQL查询失败:', error)
    throw error
  }
}

// 自然语言转SQL API
export const generateSql = async (data) => {
  try {
    const response = await request.post('/ai/audit_app/generate_sql', data, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
    return response.data
  } catch (error) {
    console.error('SQL生成失败:', error)
    throw error
  }
}

export default {
  chatWithAuditApp,
  chatWithAuditAppWithFile,
  chatWithManus,
  chatWithManusWithFile,
  uploadTable,
  executeSql,
  generateSql
}
