<template>
  <div class="container">
    <Card title="表格信息提取器" class="card" :bordered="false" style="font-family: Arial, sans-serif;">
      <Form layout="vertical">
        <Form.Item label="上传表格文件">
          <Upload
            :before-upload="beforeUpload"
            :show-upload-list="true"
            accept=".xlsx,.xls,.csv"
            name="file"
          >
            <Button type="primary">
              <a-icon type="upload" />
              点击上传表格文件
            </Button>
          </Upload>
          <p class="upload-hint">支持的格式：.xlsx, .xls, .csv</p>
        </Form.Item>
        
        <Form.Item label="提取选项">
          <CheckboxGroup v-model:value="extractOptions">
            <Row>
              <Col span="8"><Checkbox value="headers">提取表头</Checkbox></Col>
              <Col span="8"><Checkbox value="summary">生成摘要</Checkbox></Col>
              <Col span="8"><Checkbox value="statistics">统计分析</Checkbox></Col>
              <Col span="8"><Checkbox value="trends">趋势识别</Checkbox></Col>
              <Col span="8"><Checkbox value="anomalies">异常值检测</Checkbox></Col>
            </Row>
          </CheckboxGroup>
        </Form.Item>
        
        <Form.Item>
          <Button type="primary" class="submit-button" @click="extractData" :loading="loading">
            提取表格信息
          </Button>
        </Form.Item>
      </Form>
      
      <!-- 上传的文件预览 -->
      <div v-if="filePreview.length > 0" class="preview-section">
        <h3>表格预览</h3>
        <div class="table-container">
          <Table :columns="previewColumns" :data-source="filePreview" :pagination="false" size="small" />
        </div>
      </div>
      
      <!-- 提取结果 -->
      <div v-if="extractionResults && Object.keys(extractionResults).length > 0" class="results-section">
        <h3>提取结果</h3>
        
        <!-- 表头信息 -->
        <div v-if="extractionResults.headers" class="result-block">
          <h4>表头信息</h4>
          <Tag v-for="(header, index) in extractionResults.headers" :key="index" color="blue" style="margin: 5px;">
            {{ header }}
          </Tag>
        </div>
        
        <!-- 数据摘要 -->
        <div v-if="extractionResults.summary" class="result-block">
          <h4>数据摘要</h4>
          <p>{{ extractionResults.summary }}</p>
        </div>
        
        <!-- 统计分析 -->
        <div v-if="extractionResults.statistics" class="result-block">
          <h4>统计分析</h4>
          <Table :columns="statisticsColumns" :data-source="extractionResults.statistics" size="small" />
        </div>
        
        <!-- 趋势识别 -->
        <div v-if="extractionResults.trends" class="result-block">
          <h4>趋势分析</h4>
          <p>{{ extractionResults.trends }}</p>
        </div>
        
        <!-- 异常值检测 -->
        <div v-if="extractionResults.anomalies" class="result-block">
          <h4>异常值检测</h4>
          <Table :columns="anomalyColumns" :data-source="extractionResults.anomalies" size="small" />
        </div>
        
        <Form.Item>
          <Button type="default" @click="downloadResults">
            <a-icon type="download" />
            下载提取结果
          </Button>
        </Form.Item>
      </div>
    </Card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { Upload, Button, Card, Form, Table, Checkbox, Row, Col, Tag, message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { useHead } from '@vueuse/head';
// @ts-ignore - 忽略API模块的类型检查
import { previewTable, extractTableData, downloadTableResults } from '../api';

// 设置页面标题
useHead({
  title: '表格信息提取器 - 智能体应用平台',
  meta: [
    {
      name: 'description',
      content: '表格信息提取器可以帮助您上传表格文件并自动提取关键信息，生成数据摘要和统计分析'
    }
  ]
});

const router = useRouter();

// 状态管理
const loading = ref(false);
const filePreview = ref<any[]>([]);
const previewColumns = ref<any[]>([]);
const extractOptions = ref<string[]>(['headers', 'summary', 'statistics']);
const extractionResults = ref<any>(null);
const statisticsColumns = ref([
  { title: '列名', dataIndex: 'column', key: 'column' },
  { title: '数据类型', dataIndex: 'type', key: 'type' },
  { title: '平均值', dataIndex: 'average', key: 'average' },
  { title: '最小值', dataIndex: 'min', key: 'min' },
  { title: '最大值', dataIndex: 'max', key: 'max' },
  { title: '非空数量', dataIndex: 'count', key: 'count' }
]);
const anomalyColumns = ref([
  { title: '列名', dataIndex: 'column', key: 'column' },
  { title: '异常值', dataIndex: 'value', key: 'value' },
  { title: '行号', dataIndex: 'row', key: 'row' },
  { title: '异常类型', dataIndex: 'type', key: 'type' },
  { title: '原因', dataIndex: 'reason', key: 'reason' }
]);

// 文件上传前处理
const beforeUpload = async (file: File) => {
  try {
    loading.value = true;
    // 使用封装的API方法上传文件并获取预览数据
    const response = await previewTable(file);
    
    if (response.data && response.data.preview) {
      filePreview.value = response.data.preview.data;
      // 根据预览数据动态生成表格列
      if (response.data.preview.columns && response.data.preview.columns.length > 0) {
        previewColumns.value = response.data.preview.columns.map((col: string, index: number) => ({
          title: col,
          dataIndex: `field${index}`,
          key: `field${index}`,
          ellipsis: true
        }));
      }
      message.success('文件上传成功，已生成预览');
    } else {
      // 当API调用失败时，显示模拟数据作为备用
      showMockPreviewData(file.name);
      message.warning('暂时使用模拟数据预览');
    }
  } catch (error) {
    console.error('文件上传失败:', error);
    // 错误时显示模拟数据
    showMockPreviewData(file.name);
    message.warning('暂时使用模拟数据预览');
  } finally {
    loading.value = false;
  }
  
  return false; // 阻止默认上传行为
};

// 显示模拟预览数据作为备用
const showMockPreviewData = (fileName: string) => {
  // 根据文件名模拟不同的表格数据
  if (fileName.includes('销售') || fileName.includes('业绩') || fileName.includes('sales')) {
    filePreview.value = [
      { field0: '2023-01', field1: '张三', field2: 15000, field3: '北京', field4: '电子产品' },
      { field0: '2023-01', field1: '李四', field2: 18000, field3: '上海', field4: '办公用品' },
      { field0: '2023-02', field1: '张三', field2: 16500, field3: '北京', field4: '电子产品' },
      { field0: '2023-02', field1: '王五', field2: 14000, field3: '广州', field4: '日用品' },
      { field0: '2023-03', field1: '李四', field2: 20000, field3: '上海', field4: '办公用品' }
    ];
    previewColumns.value = [
      { title: '日期', dataIndex: 'field0', key: 'field0' },
      { title: '销售人员', dataIndex: 'field1', key: 'field1' },
      { title: '销售额', dataIndex: 'field2', key: 'field2' },
      { title: '区域', dataIndex: 'field3', key: 'field3' },
      { title: '产品类别', dataIndex: 'field4', key: 'field4' }
    ];
  } else if (fileName.includes('员工') || fileName.includes('人事') || fileName.includes('employee')) {
    filePreview.value = [
      { field0: '张三', field1: 32, field2: '研发部', field3: 15000, field4: '本科' },
      { field0: '李四', field1: 45, field2: '市场部', field3: 18000, field4: '硕士' },
      { field0: '王五', field1: 28, field2: '财务部', field3: 12000, field4: '本科' },
      { field0: '赵六', field1: 36, field2: '销售部', field3: 20000, field4: '大专' },
      { field0: '钱七', field1: 41, field2: '行政部', field3: 16500, field4: '本科' }
    ];
    previewColumns.value = [
      { title: '姓名', dataIndex: 'field0', key: 'field0' },
      { title: '年龄', dataIndex: 'field1', key: 'field1' },
      { title: '部门', dataIndex: 'field2', key: 'field2' },
      { title: '薪资', dataIndex: 'field3', key: 'field3' },
      { title: '学历', dataIndex: 'field4', key: 'field4' }
    ];
  } else {
    // 默认模拟数据
    filePreview.value = [
      { field0: '项目A', field1: '2023-01-01', field2: 50000, field3: '进行中' },
      { field0: '项目B', field1: '2023-02-15', field2: 80000, field3: '已完成' },
      { field0: '项目C', field1: '2023-03-10', field2: 65000, field3: '进行中' },
      { field0: '项目D', field1: '2023-04-20', field2: 120000, field3: '已暂停' },
      { field0: '项目E', field1: '2023-05-05', field2: 95000, field3: '进行中' }
    ];
    previewColumns.value = [
      { title: '项目名称', dataIndex: 'field0', key: 'field0' },
      { title: '开始日期', dataIndex: 'field1', key: 'field1' },
      { title: '预算金额', dataIndex: 'field2', key: 'field2' },
      { title: '状态', dataIndex: 'field3', key: 'field3' }
    ];
  }
};

// 提取表格数据
const extractData = async () => {
  if (filePreview.value.length === 0) {
    message.warning('请先上传表格文件');
    return;
  }
  
  if (extractOptions.value.length === 0) {
    message.warning('请至少选择一项提取选项');
    return;
  }
  
  try {
    loading.value = true;
    // 使用封装的API方法提取表格数据
    const response = await extractTableData(extractOptions.value);
    
    if (response.data && response.data.results) {
      extractionResults.value = response.data.results;
      message.success('表格信息提取成功');
    } else {
      // 当API调用失败时，生成模拟提取结果
      generateMockExtractionResults();
      message.warning('暂时使用模拟提取结果');
    }
  } catch (error) {
    console.error('表格信息提取失败:', error);
    // 错误时生成模拟提取结果
    generateMockExtractionResults();
    message.warning('暂时使用模拟提取结果');
  } finally {
    loading.value = false;
  }
};

// 生成模拟提取结果
const generateMockExtractionResults = () => {
  const results: any = {};
  
  // 根据选择的选项生成相应的模拟结果
  if (extractOptions.value.includes('headers')) {
    results.headers = previewColumns.value.map(col => col.title);
  }
  
  if (extractOptions.value.includes('summary')) {
    results.summary = `表格包含 ${filePreview.value.length} 行数据，${previewColumns.value.length} 列信息。`;
    // 根据预览数据的内容添加更详细的摘要
    if (filePreview.value.length > 0 && filePreview.value[0].field3 === '北京') {
      results.summary += ' 数据主要覆盖北京、上海、广州等地区的销售信息，产品类别包括电子产品、办公用品和日用品。';
    } else if (filePreview.value.length > 0 && filePreview.value[0].field2 === '研发部') {
      results.summary += ' 数据包含不同部门的员工信息，包括研发部、市场部、财务部等，薪资范围在12000-20000之间。';
    }
  }
  
  if (extractOptions.value.includes('statistics')) {
    // 模拟统计分析结果
    results.statistics = [
      {
        column: previewColumns.value[0]?.title || '列1',
        type: '文本',
        average: '-',
        min: '-',
        max: '-',
        count: filePreview.value.length
      },
      {
        column: previewColumns.value[1]?.title || '列2',
        type: '日期',
        average: '-',
        min: '-',
        max: '-',
        count: filePreview.value.length
      }
    ];
    // 查找数值列进行数值统计
    const numericColumnIndex = 2; // 假设第三列是数值列
    if (previewColumns.value.length > numericColumnIndex && filePreview.value.length > 0) {
      const numericColumn = previewColumns.value[numericColumnIndex];
      const values = filePreview.value
        .map(row => Number(row[`field${numericColumnIndex}`]))
        .filter(val => !isNaN(val));
      
      if (values.length > 0) {
        results.statistics.push({
          column: numericColumn.title,
          type: '数值',
          average: (values.reduce((a, b) => a + b, 0) / values.length).toFixed(2),
          min: Math.min(...values),
          max: Math.max(...values),
          count: values.length
        });
      }
    }
  }
  
  if (extractOptions.value.includes('trends')) {
    results.trends = '根据数据分析，数据呈现稳定增长趋势。';
  }
  
  if (extractOptions.value.includes('anomalies')) {
    // 模拟异常值检测结果
    results.anomalies = [
      {
        column: previewColumns.value[2]?.title || '数值列',
        value: '80000',
        row: 2,
        type: '高值异常',
        reason: '该值显著高于平均值'
      }
    ];
  }
  
  extractionResults.value = results;
};

// 下载提取结果
const downloadResults = async () => {
  if (!extractionResults.value) {
    message.warning('没有可下载的提取结果');
    return;
  }
  
  try {
    // 使用封装的API方法下载结果
    const response = await downloadTableResults(extractionResults.value);
    
    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', '表格提取结果.xlsx');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    
    message.success('结果下载成功');
  } catch (error) {
    console.error('结果下载失败:', error);
    // 错误时使用备用的下载方法
    try {
      const jsonStr = JSON.stringify(extractionResults.value, null, 2);
      const blob = new Blob([jsonStr], { type: 'application/json' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `表格提取结果_${new Date().getTime()}.json`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      message.success('结果已作为JSON文件下载');
    } catch (fallbackError) {
      message.error('结果下载失败，请重试');
    }
  }
};
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.card {
  background-color: #fafafa;
  border-radius: 8px;
}

.upload-hint {
  margin-top: 8px;
  color: #888;
  font-size: 12px;
}

.submit-button {
  margin-top: 16px;
}

.preview-section,
.results-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e8e8e8;
}

.table-container {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
}

.result-block {
  margin-bottom: 24px;
  padding: 16px;
  background-color: #fff;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.result-block h4 {
  margin-top: 0;
  margin-bottom: 12px;
  color: #333;
}
</style>