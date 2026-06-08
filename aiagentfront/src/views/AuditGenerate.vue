<template>
  <div class="container">
    <div class="main-content">
      <!-- 左侧：审计工作底稿编制 -->
      <div class="left-section">
        <Card title="审计工作底稿编制" class="card" :bordered="false" style="font-family: Arial, sans-serif;">
          <Form layout="vertical">
            <Form.Item label="项目名称">
              <Input.TextArea v-model:value="formState.projectName" placeholder="请输入审计名称..." :rows="1"/>
            </Form.Item>
            <Form.Item label="审计事项">
              <Input.TextArea v-model:value="formState.projectProfile" placeholder="请输入审计事项..." :rows="1"/>
            </Form.Item>
            <Form.Item label="审计人员">
              <Input.TextArea v-model:value="formState.auditPerson" placeholder="请输入审计人员..." :rows="1"/>
            </Form.Item>
            <Form.Item label="审计时间">
              <Input.TextArea v-model:value="formState.date" placeholder="请输入审计时间..." :rows="1"/>
            </Form.Item>
            <Form.Item label="输入审计过程">
              <Input.TextArea v-model:value="textValue" placeholder="请输入审计过程..." :rows="4" @input="handleAuditProcessInput"/>
            </Form.Item>
            <Form.Item label="上传审计底稿模板文件">
              <Upload action="/upload.do" :before-upload="beforeUpload">
                <Button>
                  <a-icon type="upload"/>
                  点击上传
                </Button>
              </Upload>
            </Form.Item>
            <Form.Item>
              <Button type="primary" class="submit-button" @click="submitForm">完成审计工作底稿</Button>
            </Form.Item>
          </Form>
        </Card>
      </div>
      
      <!-- 右侧：AI实时分析 -->
      <div class="right-section">
        <Card title="AI实时分析过程" class="card" :bordered="false" style="font-family: Arial, sans-serif;">
          <div class="ai-analysis-container">
            <div v-if="loading" class="loading">
              <a-spin size="large"/>
              <span>正在分析...</span>
            </div>
            <div v-else-if="analysisResult" class="analysis-result">
              <pre>{{ analysisResult }}</pre>
            </div>
            <div v-else class="analysis-placeholder">
              <p>请在左侧输入审计过程，AI将实时为您分析</p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, toRaw, watch } from 'vue';
import type { UnwrapRef } from 'vue';
import {Upload, Button, Card, Form, Input, Spin} from 'ant-design-vue';
import axios from 'axios';

interface FormState {
  projectName: string;
  projectProfile: string;
  auditPerson: string;
  date: string;
}

const textValue = ref<string>('');
const loading = ref<boolean>(false);
const analysisResult = ref<string>('');

// 添加防抖功能，避免输入时频繁调用API
let debounceTimer: number | null = null;

const handleAuditProcessInput = () => {
  if (debounceTimer) {
    clearTimeout(debounceTimer);
  }
  
  debounceTimer = window.setTimeout(async () => {
    if (textValue.value.trim()) {
      await analyzeAuditProcess();
    } else {
      analysisResult.value = '';
    }
  }, 1000); // 1秒防抖
};

const analyzeAuditProcess = async () => {
  if (!textValue.value.trim()) return;
  
  loading.value = true;
  try {
    const response = await axios.post('/api/ai/audit_app/analyze', {
      auditProcess: textValue.value
    }, {
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    analysisResult.value = response.data;
  } catch (error) {
    console.error('分析失败:', error);
    analysisResult.value = '分析失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};

const beforeUpload = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file); // 'file' 是后端接收字段名，根据实际情况调整

  try {
    const response = await axios.post('/api/ai/audit_app/generateword/file', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    console.log('上传成功:', response.data);
  } catch (error) {
    console.error('上传失败:', error);
  }

  return false; // 返回 false 可以阻止默认上传行为
};

const formState: UnwrapRef<FormState> = reactive({
  projectName: '',
  projectProfile: '',
  auditPerson: '',
  date: ''
});

const submitForm = async () => {
  try {
    const formData = {
      projectName: formState.projectName,
      projectProfile: formState.projectProfile,
      auditPerson: formState.auditPerson,
      date: formState.date,
      auditProcess: textValue.value,
    };
    
    const response = await axios.post('/api/ai/audit_app/generateword', formData, {
      headers: {
        'Content-Type': 'application/json',
      },
      responseType: 'blob' // 确保接收blob类型数据
    });

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', '审计工作底稿.docx'); // 设置文件名
    document.body.appendChild(link);
    link.click();
    
    // 清理
    window.URL.revokeObjectURL(url);
    document.body.removeChild(link);
    
    console.log('提交成功并下载文件');
  } catch (error) {
    console.error('提交失败:', error);
  }
};
const onSubmit = () => {
  console.log('submit!', toRaw(formState));
};
</script>

<style scoped>
/* 添加一些基本的样式 */
.container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  min-height: 100vh;
  margin: 0;
  padding: 20px;
}

.main-content {
  display: flex;
  gap: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.left-section, .right-section {
  flex: 1;
  min-width: 0;
}

.card {
  width: 100%;
  margin: 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  background-color: #fff;
}

:deep(.ant-card-head) {
  font-size: 24px !important;
}

:deep(.ant-card-body) {
  padding: 20px;
}

Form.Item {
  margin-bottom: 20px;
}

Input.TextArea {
  border: 2px solid #40a9ff;
  border-radius: 4px;
}

.submit-button {
  width: 50%;
  display: block;
  margin: 0 auto;
}

/* AI分析区域样式 */
.ai-analysis-container {
  min-height: 400px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  min-height: 300px;
}

/* 确保左右两个Card高度一致 */
.right-section .ant-card {
  min-height: 100%;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  color: #666;
}

.analysis-result {
  width: 100%;
  text-align: left;
}

.analysis-result pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: Arial, sans-serif;
  font-size: 16px;
  line-height: 1.5;
  color: #333;
}

.analysis-placeholder {
  color: #999;
  text-align: center;
  padding: 40px 0;
}

/* 响应式布局 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }
  
  .left-section,
  .right-section {
    width: 100%;
  }
}
</style>
