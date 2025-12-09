<template>
  <a-card title="阶段任务" :loading="loading" :extra="renderActions()">
    <!-- 学生端显示进度 -->
    <div v-if="auth.role === 'STUDENT'" style="margin-bottom: 16px">
      <a-progress 
        :percent="taskProgress" 
        :status="taskProgress === 100 ? 'success' : 'active'"
        :format="() => `${completedTasks}/${totalStages} 已完成`"
      />
      <div style="margin-top: 8px; color: #999; font-size: 12px">
        当前阶段：{{ currentStageName || '未开始' }}
      </div>
    </div>
    <a-table :dataSource="tasks" :pagination="false" rowKey="id">
      <a-table-column title="阶段" :customRender="({ record }) => stageName(record.stageId)" />
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
      <a-table-column title="操作" :customRender="(_, record) => actionRender(record)" />
    </a-table>
  </a-card>

  <a-modal v-model:open="openSubmit" title="提交阶段任务" @ok="submitTask" :confirmLoading="submitting">
    <a-form layout="vertical">
      <a-form-item label="阶段" :rules="[{ required: true, message: '请选择阶段' }]">
        <a-select v-model:value="form.stageId" :options="stageOptions" placeholder="请选择阶段" />
      </a-form-item>
      <a-form-item label="课题ID（可选）">
        <a-input-number v-model:value="form.topicId" style="width:100%;" />
      </a-form-item>
      <a-form-item label="内容" :rules="[{ required: true, message: '请输入内容' }]">
        <a-textarea v-model:value="form.content" rows="4" placeholder="请输入任务内容" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal v-model:open="taskDetailOpen" title="任务详情" :footer="null" width="800px">
    <a-descriptions :column="2" bordered>
      <a-descriptions-item label="阶段">{{ stageName(currentTask.stageId) }}</a-descriptions-item>
      <a-descriptions-item label="状态">{{ currentTask.status }}</a-descriptions-item>
      <a-descriptions-item label="学生ID">{{ currentTask.studentId }}</a-descriptions-item>
      <a-descriptions-item label="课题ID">{{ currentTask.topicId }}</a-descriptions-item>
      <a-descriptions-item label="内容" :span="2">
        <div style="white-space: pre-wrap;">{{ currentTask.content }}</div>
      </a-descriptions-item>
    </a-descriptions>
    <a-divider>审核记录</a-divider>
    <a-table :dataSource="taskReviews" rowKey="id" :pagination="false" size="small">
      <a-table-column title="审核人ID" dataIndex="reviewerId" />
      <a-table-column title="决策" dataIndex="decision" />
      <a-table-column title="意见" dataIndex="comment" />
      <a-table-column title="时间" dataIndex="createdAt" />
    </a-table>
  </a-modal>

  <a-modal v-model:open="reviewModalOpen" title="审核任务" @ok="doReview" :confirmLoading="reviewing">
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
import { ref, onMounted, h, computed } from 'vue';
import api from '../api';
import { Button, message, Space } from 'ant-design-vue';
import { useAuthStore } from '../store/auth';

const tasks = ref([]);
const stages = ref([]);
const loading = ref(false);
const openSubmit = ref(false);
const submitting = ref(false);
const form = ref({ stageId: null, topicId: null, content: '' });
const auth = useAuthStore();
const taskDetailOpen = ref(false);
const currentTask = ref({});
const taskReviews = ref([]);
const reviewModalOpen = ref(false);
const reviewing = ref(false);
const reviewForm = ref({ decision: 'APPROVED', comment: '' });
const currentReviewTaskId = ref(null);
const taskProgress = ref(0);
const completedTasks = ref(0);
const totalStages = ref(0);
const currentStageName = ref('');

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

const decisionOptions = [
  { label: '通过', value: 'APPROVED' },
  { label: '驳回', value: 'REJECTED' }
];

const fetchData = async () => {
  loading.value = true;
  try {
    const [stageRes, taskRes] = await Promise.all([
      api.get('/stages'),
      api.get('/stages/tasks', { params: auth.role === 'STUDENT' ? { studentId: auth.user.id } : {} })
    ]);
    stages.value = stageRes.data;
    tasks.value = taskRes.data;
    
    // 计算学生端进度
    if (auth.role === 'STUDENT') {
      const activeStages = stages.value.filter(s => s.active);
      totalStages.value = activeStages.length;
      const approvedTasks = tasks.value.filter(t => t.status === 'APPROVED');
      completedTasks.value = approvedTasks.length;
      taskProgress.value = totalStages.value > 0 ? Math.round((completedTasks.value / totalStages.value) * 100) : 0;
      
      // 找到当前阶段（第一个未通过或待提交的阶段）
      const currentStage = activeStages.find(stage => {
        const task = tasks.value.find(t => t.stageId === stage.id);
        return !task || task.status !== 'APPROVED';
      });
      currentStageName.value = currentStage ? currentStage.name : (activeStages.length > 0 ? activeStages[activeStages.length - 1].name : '');
    }
  } finally {
    loading.value = false;
  }
};

const stageName = (id) => stages.value.find(s => s.id === id)?.name || `阶段${id}`;

const stageOptions = computed(() =>
  stages.value.filter(s => s.active).map(s => ({ label: s.name, value: s.id }))
);

const renderActions = () =>
  h(Space, {}, () => {
    const btns = [];
    if (auth.role === 'STUDENT') {
      btns.push(h(Button, { type: 'primary', size: 'small', onClick: () => openSubmit.value = true }, () => '提交任务'));
    }
    btns.push(h(Button, { size: 'small', onClick: fetchData }, () => '刷新'));
    if (auth.role === 'ADMIN') {
      btns.push(h(Button, { size: 'small', onClick: () => window.open('/api/exports/stage-reviews', '_blank') }, () => '导出审核记录'));
    }
    return btns;
  });

const submitTask = async () => {
  if (!form.value.stageId) {
    message.warning('请选择阶段');
    return;
  }
  if (!form.value.content || form.value.content.trim() === '') {
    message.warning('请输入内容');
    return;
  }
  submitting.value = true;
  try {
    await api.post(`/stages/${form.value.stageId}/tasks`, {
      topicId: form.value.topicId,
      content: form.value.content
    });
    message.success('提交成功');
    openSubmit.value = false;
    form.value = { stageId: null, topicId: null, content: '' };
    fetchData();
  } catch (e) {
    message.error(e.response?.data?.message || '提交失败');
  } finally {
    submitting.value = false;
  }
};

const viewTaskDetail = async (taskId) => {
  taskDetailOpen.value = true;
  try {
    const [taskRes, reviewsRes] = await Promise.all([
      api.get(`/stages/tasks/${taskId}`),
      api.get(`/stages/tasks/${taskId}/reviews`)
    ]);
    currentTask.value = taskRes.data;
    taskReviews.value = reviewsRes.data;
  } catch (e) {
    message.error('加载详情失败');
  }
};

const openReviewModal = (taskId) => {
  currentReviewTaskId.value = taskId;
  reviewForm.value = { decision: 'APPROVED', comment: '' };
  reviewModalOpen.value = true;
};

const doReview = async () => {
  reviewing.value = true;
  try {
    await api.post(`/stages/tasks/${currentReviewTaskId.value}/review`, reviewForm.value);
    message.success('审核完成');
    reviewModalOpen.value = false;
    fetchData();
  } catch (e) {
    message.error(e.response?.data?.message || '审核失败');
  } finally {
    reviewing.value = false;
  }
};

const actionRender = (record) =>
  h(Space, {}, () => {
    const btns = [];
    btns.push(h(Button, { size: 'small', onClick: () => viewTaskDetail(record.id) }, () => '详情'));
    if ((auth.role === 'ADMIN' || auth.role === 'TEACHER') && record.status === 'SUBMITTED') {
      btns.push(h(Button, { size: 'small', type: 'primary', onClick: () => openReviewModal(record.id) }, () => '审核'));
    }
    return btns;
  });

onMounted(fetchData);
</script>
