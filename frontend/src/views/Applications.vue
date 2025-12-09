<template>
  <a-card title="申请管理" :loading="loading">
    <template #extra>
      <a-space>
        <a-button 
          v-if="auth.role === 'STUDENT'" 
          type="primary" 
          @click="handleCreate"
        >
          提交申请
        </a-button>
        <a-button @click="fetchData">刷新</a-button>
      </a-space>
    </template>

    <!-- 搜索和筛选 -->
    <a-form layout="inline" :model="searchForm" style="margin-bottom: 16px">
      <a-form-item label="关键词">
        <a-input 
          v-model:value="searchForm.keyword" 
          placeholder="申请内容" 
          allow-clear
          style="width: 200px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item label="类型">
        <a-select 
          v-model:value="searchForm.type" 
          placeholder="全部类型" 
          allow-clear
          style="width: 150px"
          :options="typeOptions"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select 
          v-model:value="searchForm.status" 
          placeholder="全部状态" 
          allow-clear
          style="width: 120px"
          :options="statusOptions"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleSearch">搜索</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 申请表格 -->
    <a-table 
      :dataSource="items" 
      rowKey="id" 
      :pagination="pagination"
      @change="handleTableChange"
    >
      <a-table-column title="ID" dataIndex="id" width="80" />
      <a-table-column title="类型" dataIndex="type" width="120">
        <template #customRender="{ text }">
          <a-tag :color="getTypeColor(text)">{{ getTypeText(text) }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="学生ID" dataIndex="studentId" width="100" />
      <a-table-column title="课题ID" dataIndex="topicId" width="100" />
      <a-table-column title="状态" dataIndex="status" width="120">
        <template #customRender="{ text }">
          <a-tag :color="getStatusColor(text)">{{ getStatusText(text) }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="申请内容" dataIndex="payload" :ellipsis="true" />
      <a-table-column title="创建时间" dataIndex="createdAt" width="180">
        <template #customRender="{ text }">
          {{ text ? new Date(text).toLocaleString() : '-' }}
        </template>
      </a-table-column>
      <a-table-column title="操作" width="300" fixed="right">
        <template #customRender="{ record }">
          <a-space>
            <a-button size="small" @click="handleViewDetail(record)">详情</a-button>
            <a-button size="small" @click="handleViewLogs(record.id)">日志</a-button>
            <a-button 
              v-if="canReview && record.status === 'SUBMITTED'" 
              size="small" 
              type="primary" 
              @click="handleOpenReview(record.id, 'APPROVED')"
            >
              通过
            </a-button>
            <a-button 
              v-if="canReview && record.status === 'SUBMITTED'" 
              size="small" 
              danger 
              @click="handleOpenReview(record.id, 'REJECTED')"
            >
              驳回
            </a-button>
            <a-button 
              v-if="auth.role === 'STUDENT' && record.status === 'SUBMITTED'" 
              size="small" 
              @click="handleWithdraw(record.id)"
            >
              撤回
            </a-button>
            <a-button 
              v-if="auth.role === 'STUDENT' && record.status === 'REJECTED'" 
              size="small" 
              type="primary" 
              @click="handleOpenResubmit(record)"
            >
              重新提交
            </a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </a-card>

  <!-- 提交申请对话框 -->
  <a-modal 
    v-model:open="openCreate" 
    title="提交申请" 
    @ok="handleSubmit" 
    :confirmLoading="submitting"
    :width="700"
  >
    <a-form 
      ref="createFormRef"
      :model="createForm" 
      :rules="createFormRules" 
      layout="vertical"
    >
      <a-form-item label="类型" name="type">
        <a-select v-model:value="createForm.type" :options="typeOptions" placeholder="请选择申请类型" />
      </a-form-item>
      <a-form-item label="课题ID（可选）" name="topicId">
        <a-input-number v-model:value="createForm.topicId" style="width: 100%" :min="1" />
        <div style="color: #999; font-size: 12px; margin-top: 4px">如果申请与特定课题相关，请填写课题ID</div>
      </a-form-item>
      <a-form-item label="申请内容" name="payload">
        <a-textarea v-model:value="createForm.payload" :rows="6" placeholder="请详细填写申请内容" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 申请详情对话框 -->
  <a-modal 
    v-model:open="openDetail" 
    title="申请详情" 
    :footer="null"
    :width="800"
  >
    <a-descriptions :column="2" bordered v-if="detailApplication">
      <a-descriptions-item label="ID">{{ detailApplication.id }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(detailApplication.status)">
          {{ getStatusText(detailApplication.status) }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="类型">
        <a-tag :color="getTypeColor(detailApplication.type)">
          {{ getTypeText(detailApplication.type) }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="学生ID">{{ detailApplication.studentId || '-' }}</a-descriptions-item>
      <a-descriptions-item label="课题ID">{{ detailApplication.topicId || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">
        {{ detailApplication.createdAt ? new Date(detailApplication.createdAt).toLocaleString() : '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="申请内容" :span="2">
        <div style="white-space: pre-wrap">{{ detailApplication.payload || '-' }}</div>
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>

  <!-- 审批对话框 -->
  <a-modal 
    v-model:open="openReview" 
    title="审批申请" 
    @ok="handleReview" 
    :confirmLoading="reviewing"
  >
    <a-form layout="vertical">
      <a-form-item label="决策">
        <a-tag :color="currentDecision === 'APPROVED' ? 'green' : 'red'" style="font-size: 14px; padding: 4px 12px">
          {{ currentDecision === 'APPROVED' ? '通过' : '驳回' }}
        </a-tag>
      </a-form-item>
      <a-form-item label="审批意见" name="comment" :rules="[{ required: true, message: '审批意见不能为空' }]">
        <a-textarea v-model:value="reviewForm.comment" :rows="4" placeholder="请输入审批意见" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 申请日志对话框 -->
  <a-modal 
    v-model:open="openLogs" 
    title="申请日志" 
    :footer="null"
    :width="700"
  >
    <a-timeline>
      <a-timeline-item v-for="log in logs" :key="log.id">
        <p><strong>{{ getActionText(log.action) }}</strong></p>
        <p v-if="log.comment">{{ log.comment }}</p>
        <p style="color: #999; font-size: 12px; margin-top: 4px">
          {{ log.createdAt ? new Date(log.createdAt).toLocaleString() : '-' }}
        </p>
      </a-timeline-item>
    </a-timeline>
  </a-modal>

  <!-- 重新提交对话框 -->
  <a-modal 
    v-model:open="openResubmit" 
    title="重新提交申请" 
    @ok="handleResubmit" 
    :confirmLoading="resubmitting"
    :width="700"
  >
    <a-form layout="vertical">
      <a-form-item label="申请内容" :rules="[{ required: true, message: '申请内容不能为空' }]">
        <a-textarea v-model:value="resubmitForm.payload" :rows="6" placeholder="请详细填写申请内容" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import api from '../api';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const reviewing = ref(false);
const resubmitting = ref(false);
const items = ref([]);
const openCreate = ref(false);
const openDetail = ref(false);
const openReview = ref(false);
const openLogs = ref(false);
const openResubmit = ref(false);
const createForm = ref({ type: null, topicId: null, payload: '' });
const createFormRef = ref(null);
const detailApplication = ref(null);
const reviewForm = ref({ comment: '' });
const currentReviewId = ref(null);
const currentDecision = ref('APPROVED');
const logs = ref([]);
const resubmitForm = ref({ payload: '' });
const currentResubmitId = ref(null);

const searchForm = reactive({
  keyword: '',
  type: undefined,
  status: undefined
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条记录`
});

const typeOptions = [
  { label: '校外毕设', value: 'EXTERNAL' },
  { label: '答辩申请', value: 'DEFENSE' },
  { label: '延期/特殊申请', value: 'EXTENSION' }
];

const statusOptions = [
  { label: '待审批', value: 'SUBMITTED' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
];

const createFormRules = {
  type: [
    { required: true, message: '请选择申请类型', trigger: 'change' }
  ],
  payload: [
    { required: true, message: '请填写申请内容', trigger: 'blur' },
    { min: 10, message: '申请内容至少10个字符', trigger: 'blur' }
  ]
};

const canReview = computed(() => auth.role === 'ADMIN' || auth.role === 'TEACHER');

const getTypeColor = (type) => {
  const colors = {
    'EXTERNAL': 'blue',
    'DEFENSE': 'green',
    'EXTENSION': 'orange'
  };
  return colors[type] || 'default';
};

const getTypeText = (type) => {
  const texts = {
    'EXTERNAL': '校外毕设',
    'DEFENSE': '答辩申请',
    'EXTENSION': '延期/特殊申请'
  };
  return texts[type] || type;
};

const getStatusColor = (status) => {
  const colors = {
    'SUBMITTED': 'orange',
    'APPROVED': 'green',
    'REJECTED': 'red'
  };
  return colors[status] || 'default';
};

const getStatusText = (status) => {
  const texts = {
    'SUBMITTED': '待审批',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  };
  return texts[status] || status;
};

const getActionText = (action) => {
  const texts = {
    'SUBMITTED': '提交申请',
    'APPROVED': '审批通过',
    'REJECTED': '审批驳回',
    'WITHDRAWN': '撤回申请',
    'RESUBMITTED': '重新提交'
  };
  return texts[action] || action;
};

const fetchData = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize,
      ...searchForm
    };
    // 移除空值
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === undefined || params[key] === null) {
        delete params[key];
      }
    });
    const response = await api.get('/applications/page', { params });
    console.log('申请列表响应:', response);
    
    const responseData = response.data;
    if (responseData && responseData.data) {
      const pageData = responseData.data;
      items.value = pageData.content || [];
      pagination.total = pageData.totalElements || 0;
    } else {
      items.value = [];
      pagination.total = 0;
    }
  } catch (e) {
    console.error('加载申请列表失败:', e);
    const errorMsg = e.response?.data?.message || e.message || '加载失败';
    message.error(errorMsg);
    items.value = [];
    pagination.total = 0;
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pag) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchData();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchData();
};

const handleReset = () => {
  searchForm.keyword = '';
  searchForm.type = undefined;
  searchForm.status = undefined;
  pagination.current = 1;
  fetchData();
};

const handleCreate = () => {
  createForm.value = { type: null, topicId: null, payload: '' };
  openCreate.value = true;
  if (createFormRef.value) {
    createFormRef.value.clearValidate();
  }
};

const handleSubmit = async () => {
  try {
    await createFormRef.value.validate();
    submitting.value = true;
    await api.post(`/applications/${createForm.value.type}`, {
      topicId: createForm.value.topicId,
      payload: createForm.value.payload
    });
    message.success('提交成功');
    openCreate.value = false;
    createForm.value = { type: null, topicId: null, payload: '' };
    await fetchData();
  } catch (e) {
    console.error('提交申请失败:', e);
    if (e.errorFields) {
      return;
    }
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '提交失败';
    message.error(errorMsg);
  } finally {
    submitting.value = false;
  }
};

const handleViewDetail = async (record) => {
  try {
    const response = await api.get(`/applications/${record.id}`);
    const responseData = response.data;
    detailApplication.value = responseData?.data || responseData;
    openDetail.value = true;
  } catch (e) {
    console.error('获取申请详情失败:', e);
    message.error('获取申请详情失败');
  }
};

const handleViewLogs = async (id) => {
  openLogs.value = true;
  try {
    const response = await api.get(`/applications/${id}/logs`);
    logs.value = response.data?.data || response.data || [];
  } catch (e) {
    console.error('加载日志失败:', e);
    message.error('加载日志失败');
    logs.value = [];
  }
};

const handleOpenReview = (id, decision) => {
  currentReviewId.value = id;
  currentDecision.value = decision;
  reviewForm.value = { comment: '' };
  openReview.value = true;
};

const handleReview = async () => {
  if (!reviewForm.value.comment || reviewForm.value.comment.trim() === '') {
    message.warning('请输入审批意见');
    return;
  }
  reviewing.value = true;
  try {
    await api.post(`/applications/${currentReviewId.value}/review`, {
      decision: currentDecision.value,
      comment: reviewForm.value.comment
    });
    message.success('审批完成');
    openReview.value = false;
    await fetchData();
  } catch (e) {
    console.error('审批失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '审批失败';
    message.error(errorMsg);
  } finally {
    reviewing.value = false;
  }
};

const handleWithdraw = async (id) => {
  try {
    await api.post(`/applications/${id}/withdraw`);
    message.success('已撤回');
    await fetchData();
  } catch (e) {
    console.error('撤回失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '撤回失败';
    message.error(errorMsg);
  }
};

const handleOpenResubmit = (record) => {
  currentResubmitId.value = record.id;
  resubmitForm.value = { payload: record.payload || '' };
  openResubmit.value = true;
};

const handleResubmit = async () => {
  if (!resubmitForm.value.payload || resubmitForm.value.payload.trim() === '') {
    message.warning('请填写申请内容');
    return;
  }
  resubmitting.value = true;
  try {
    await api.post(`/applications/${currentResubmitId.value}/resubmit`, resubmitForm.value);
    message.success('重新提交成功');
    openResubmit.value = false;
    await fetchData();
  } catch (e) {
    console.error('重新提交失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '重新提交失败';
    message.error(errorMsg);
  } finally {
    resubmitting.value = false;
  }
};

onMounted(() => {
  fetchData();
});
</script>
