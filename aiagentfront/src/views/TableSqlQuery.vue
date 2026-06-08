<template>
  <div class="container">
    <Card title="数据上传与分析" class="card" :bordered="false" style="font-family: Arial, sans-serif;">
      <div class="upload-section">
        <Form.Item label="上传表格文件">
          <Upload
            :before-upload="beforeUpload"
            :file-list="fileList"
            :max-count="1"
            accept=".xlsx,.xls,.csv"
          >
            <Button type="primary">
              <a-icon type="upload" />
              点击上传数据
            </Button>
          </Upload>
        </Form.Item>
      </div>

      <div class="sql-section" v-if="fileUploaded">
        <Form.Item label="自然语言问题">
          <Input.TextArea
            v-model:value="naturalLanguageQuestion"
            placeholder="请输入您的查询问题，例如：查询2023年销售额大于10000的记录..."
            :rows="3"
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" @click="generateSqlQuery" :loading="generating">
            生成SQL语句
          </Button>
        </Form.Item>
        
        <Form.Item label="SQL查询语句">
          <Input.TextArea
            v-model:value="sqlQuery"
            placeholder="请输入SQL查询语句..."
            :rows="4"
          />
        </Form.Item>
        <Form.Item>
          <Button type="primary" @click="executeSqlQuery" :loading="executing">
            执行查询
          </Button>
        </Form.Item>
      </div>

      <div class="result-section" v-if="queryResult">
        <Card title="分析结果" class="result-card">
          <Table
            :columns="resultColumns"
            :data-source="resultData"
            :pagination="{ pageSize: 10 }"
            bordered
          />
        </Card>
      </div>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { Upload, Button, Card, Form, Input, Table, message } from 'ant-design-vue';
import { uploadTable, executeSql, generateSql } from '../api/index.js';

const fileList = ref<any[]>([]);
const fileUploaded = ref(false);
const uploadedTableName = ref(''); // 保存上传表格的表名
const sqlQuery = ref('');
const naturalLanguageQuestion = ref('');
const executing = ref(false);
const generating = ref(false);
const queryResult = ref(false);
const resultColumns = ref<any[]>([]);
const resultData = ref<any[]>([]);

// 文件上传前处理
const beforeUpload = async (file: File) => {
  try {
    const tableName = await uploadTable(file);
    console.log('表格上传成功:', tableName);
    uploadedTableName.value = tableName;
    message.success('表格上传成功');
    fileList.value = [{
      uid: file.uid,
      name: file.name,
      status: 'done',
    }];
    fileUploaded.value = true;
  } catch (error) {
    console.error('表格上传失败:', error);
    message.error('表格上传失败');
  }

  return false; // 阻止默认上传行为
};

// 生成SQL语句
const generateSqlQuery = async () => {
  if (!naturalLanguageQuestion.value) {
    message.warning('请输入自然语言查询问题');
    return;
  }

  generating.value = true;
  try {
    // 将表名和问题一起发送给后端
    const requestData = {
      question: naturalLanguageQuestion.value,
      tableName: uploadedTableName.value
    };
    const response = await generateSql(requestData);
    console.log('生成SQL成功:', response);
    
    // 将生成的SQL填充到输入框
    sqlQuery.value = response;
    message.success('SQL语句生成成功');
  } catch (error) {
    console.error('生成SQL失败:', error);
    message.error('SQL语句生成失败');
  } finally {
    generating.value = false;
  }
};

// 执行SQL查询
const executeSqlQuery = async () => {
  if (!sqlQuery.value) {
    message.warning('请输入SQL查询语句');
    return;
  }

  executing.value = true;
  try {
    const response = await executeSql(sqlQuery.value);
    console.log('查询响应:', response);
    
    // 检查是否有错误信息
    if (response && response.error) {
      message.error(response.error);
      return;
    }
    
    // 转换响应数据为Table组件需要的格式
    if (response && response.data && response.columns) {
      // 转换列配置
      resultColumns.value = response.columns.map((column: any) => ({
        title: column.title || column.name,
        dataIndex: column.dataIndex || column.name,
        key: column.key || column.name,
        width: 150,
        ellipsis: true
      }));
      
      // 转换数据行
      resultData.value = response.data.map((row: any, index: number) => ({
        ...row,
        key: index
      }));
      
      message.success('查询成功');
      queryResult.value = true;
    } else {
      message.warning('查询结果格式异常');
      console.error('查询结果格式异常:', response);
    }
  } catch (error) {
    console.error('查询失败:', error);
    message.error('SQL查询失败');
  } finally {
    executing.value = false;
  }
};
</script>

<style scoped>
.container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.card {
  margin-bottom: 20px;
}

.upload-section {
  margin-bottom: 30px;
}

.sql-section {
  margin-bottom: 30px;
}

.result-card {
  margin-top: 20px;
}
</style>