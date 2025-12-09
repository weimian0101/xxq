<template>
  <a-card title="我的学生" :loading="loading">
    <template #extra>
      <a-button @click="fetchData">刷新</a-button>
    </template>

    <a-table 
      :dataSource="students" 
      rowKey="studentId" 
      :pagination="pagination"
      @change="handleTableChange"
    >
      <a-table-column title="学生ID" dataIndex="studentId" width="120" />
      <a-table-column title="课题ID" dataIndex="topicId" width="120" />
      <a-table-column title="课题标题" dataIndex="topicTitle" />
      <a-table-column title="选题状态" dataIndex="selectionStatus" width="120">
        <template #customRender="{ text }">
          <a-tag :color="getSelectionStatusColor(text)">{{ getSelectionStatusText(text) }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="选择时间" dataIndex="createdAt" width="180">
        <template #customRender="{ text }">
          {{ text ? new Date(text).toLocaleString() : '-' }}
        </template>
      </a-table-column>
      <a-table-column title="操作" width="200">
        <template #customRender="{ record }">
          <a-space>
            <a-button size="small" @click="handleViewStudentTasks(record.studentId)">查看任务</a-button>
            <a-button size="small" @click="handleViewTopic(record.topicId)">查看课题</a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </a-card>

  <!-- 学生任务对话框 -->
  <a-modal 
    v-model:open="openStudentTasks" 
    title="学生阶段任务" 
    :footer="null"
    :width="900"
  >
    <a-table 
      :dataSource="studentTasks" 
      rowKey="id" 
      :pagination="false"
      :loading="tasksLoading"
    >
      <a-table-column title="阶段" dataIndex="stageId" />
      <a-table-column title="状态" dataIndex="status">
        <template #customRender="{ text }">
          <a-tag :color="getTaskStatusColor(text)">{{ getTaskStatusText(text) }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="内容" dataIndex="content" :ellipsis="true" />
      <a-table-column title="更新时间" dataIndex="updatedAt" width="180">
        <template #customRender="{ text }">
          {{ text ? new Date(text).toLocaleString() : '-' }}
        </template>
      </a-table-column>
      <a-table-column title="操作" width="150">
        <template #customRender="{ record }">
          <a-space>
            <a-button size="small" @click="handleViewTaskDetail(record.id)">详情</a-button>
            <a-button 
              v-if="record.status === 'SUBMITTED'" 
              size="small" 
              type="primary" 
              @click="handleOpenReview(record.id)"
            >
              审核
            </a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </a-modal>

  <!-- 审核对话框 -->
  <a-modal 
    v-model:open="openReview" 
    title="审核任务" 
    @ok="handleReview" 
    :confirmLoading="reviewing"
  >
    <a-form layout="vertical">
      <a-form-item label="决策">
        <a-select v-model:value="reviewForm.decision" :options="decisionOptions" />
      </a-form-item>
      <a-form-item label="审核意见">
        <a-textarea v-model:value="reviewForm.comment" rows="3" placeholder="请输入审核意见" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import api from '../api';
import { message } from 'ant-design-vue';

const loading = ref(false);
const tasksLoading = ref(false);
const reviewing = ref(false);
const students = ref([]);
const studentTasks = ref([]);
const openStudentTasks = ref(false);
const openReview = ref(false);
const currentStudentId = ref(null);
const currentReviewTaskId = ref(null);
const reviewForm = ref({ decision: 'APPROVED', comment: '' });

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条记录`
});

const decisionOptions = [
  { label: '通过', value: 'APPROVED' },
  { label: '驳回', value: 'REJECTED' }
];

const getSelectionStatusColor = (status) => {
  const colors = {
    'SELECTED': 'blue',
    'LOCKED': 'green',
    'CANCELLED': 'red'
  };
  return colors[status] || 'default';
};

const getSelectionStatusText = (status) => {
  const texts = {
    'SELECTED': '已选择',
    'LOCKED': '已锁定',
    'CANCELLED': '已取消'
  };
  return texts[status] || status;
};

const getTaskStatusColor = (status) => {
  const colors = {
    'PENDING': 'default',
    'SUBMITTED': 'orange',
    'APPROVED': 'green',
    'REJECTED': 'red'
  };
  return colors[status] || 'default';
};

const getTaskStatusText = (status) => {
  const texts = {
    'PENDING': '待提交',
    'SUBMITTED': '待审核',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  };
  return texts[status] || status;
};

const fetchData = async () => {
  loading.value = true;
  try {
    const response = await api.get('/topics/my-students');
    students.value = response.data?.data || [];
    pagination.total = students.value.length;
  } catch (e) {
    console.error('加载我的学生失败:', e);
    message.error('加载我的学生失败');
    students.value = [];
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pag) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
};

const handleViewStudentTasks = async (studentId) => {
  currentStudentId.value = studentId;
  openStudentTasks.value = true;
  tasksLoading.value = true;
  try {
    const response = await api.get('/stages/tasks', { params: { studentId } });
    studentTasks.value = response.data || [];
  } catch (e) {
    console.error('加载学生任务失败:', e);
    message.error('加载学生任务失败');
    studentTasks.value = [];
  } finally {
    tasksLoading.value = false;
  }
};

const handleViewTopic = (topicId) => {
  window.open(`/topics?id=${topicId}`, '_blank');
};

const handleViewTaskDetail = async (taskId) => {
  // 这里可以打开任务详情对话框，或者跳转到阶段任务页面
  message.info('请到阶段任务页面查看详情');
};

const handleOpenReview = (taskId) => {
  currentReviewTaskId.value = taskId;
  reviewForm.value = { decision: 'APPROVED', comment: '' };
  openReview.value = true;
};

const handleReview = async () => {
  reviewing.value = true;
  try {
    await api.post(`/stages/tasks/${currentReviewTaskId.value}/review`, reviewForm.value);
    message.success('审核完成');
    openReview.value = false;
    if (currentStudentId.value) {
      await handleViewStudentTasks(currentStudentId.value);
    }
  } catch (e) {
    console.error('审核失败:', e);
    const errorMsg = e.response?.data?.message || e.message || '审核失败';
    message.error(errorMsg);
  } finally {
    reviewing.value = false;
  }
};

onMounted(fetchData);
</script>

