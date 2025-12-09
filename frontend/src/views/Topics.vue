<template>
  <a-card title="课题管理" :loading="loading">
    <template #extra>
      <a-space>
        <a-button 
          v-if="auth.role === 'STUDENT'" 
          type="primary" 
          @click="handleViewMySelection"
        >
          我的选题
        </a-button>
        <a-button type="primary" @click="handleCreate" v-if="auth.role === 'ADMIN' || auth.role === 'TEACHER'">
          新建课题
        </a-button>
        <a-button @click="handleExportTopics" v-if="auth.role === 'ADMIN'">
          导出课题数据
        </a-button>
        <a-button @click="fetchTopics">刷新</a-button>
      </a-space>
    </template>

    <!-- 搜索和筛选 -->
    <a-form layout="inline" :model="searchForm" style="margin-bottom: 16px">
      <a-form-item label="关键词">
        <a-input 
          v-model:value="searchForm.keyword" 
          placeholder="标题/描述" 
          allow-clear
          style="width: 200px"
          @pressEnter="handleSearch"
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
      <a-form-item label="创建人" v-if="auth.role === 'ADMIN' || auth.role === 'TEACHER'">
        <a-select 
          v-model:value="searchForm.creatorId" 
          placeholder="全部创建人" 
          allow-clear
          style="width: 150px"
        >
          <a-select-option :value="auth.user.id">我的课题</a-select-option>
          <a-select-option :value="undefined">全部</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleSearch">搜索</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 课题表格 -->
    <a-table 
      :dataSource="items" 
      rowKey="id" 
      :pagination="pagination"
      @change="handleTableChange"
    >
      <a-table-column title="ID" dataIndex="id" width="80" />
      <a-table-column title="标题" dataIndex="title" />
      <a-table-column title="描述" dataIndex="description" :ellipsis="true" />
      <a-table-column title="状态" dataIndex="status" width="120">
        <template #customRender="{ text }">
          <a-tag :color="getStatusColor(text)">{{ getStatusText(text) }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="容量" width="100">
        <template #customRender="{ record }">
          {{ getCurrentCount(record.id) }}/{{ record.capacity || 1 }}
        </template>
      </a-table-column>
      <a-table-column title="创建人" dataIndex="creatorId" width="100" />
      <a-table-column title="创建时间" dataIndex="createdAt" width="180">
        <template #customRender="{ text }">
          {{ text ? new Date(text).toLocaleString() : '-' }}
        </template>
      </a-table-column>
      <a-table-column title="操作" width="400" fixed="right">
        <template #customRender="{ record }">
          <a-space>
            <a-button size="small" @click="handleViewDetail(record)">详情</a-button>
            <a-button 
              v-if="canEdit(record)" 
              size="small" 
              @click="handleEdit(record)"
            >
              编辑
            </a-button>
            <a-button 
              v-if="record.status === 'DRAFT' && (auth.role === 'ADMIN' || auth.role === 'TEACHER')" 
              size="small" 
              @click="handleSubmit(record.id)"
            >
              提交
            </a-button>
            <a-button 
              v-if="record.status === 'SUBMITTED' && (auth.role === 'ADMIN' || auth.role === 'TEACHER')" 
              size="small" 
              type="primary" 
              @click="handleOpenApprove(record.id)"
            >
              审批
            </a-button>
            <a-button 
              v-if="record.status === 'APPROVED' && auth.role === 'STUDENT'" 
              size="small" 
              type="primary"
              :disabled="isTopicFull(record)"
              @click="handleSelect(record.id)"
            >
              {{ isTopicFull(record) ? '已满' : '选题' }}
            </a-button>
            <a-button 
              size="small" 
              @click="handleShowApprovals(record.id)"
            >
              审批记录
            </a-button>
            <a-button 
              v-if="auth.role === 'ADMIN' || auth.role === 'TEACHER'" 
              size="small" 
              @click="handleShowSelections(record.id)"
            >
              选题情况
            </a-button>
            <a-popconfirm
              v-if="canDelete(record)"
              title="确定要删除该课题吗？此操作不可恢复！"
              @confirm="() => handleDelete(record.id)"
            >
              <a-button size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </a-card>

  <!-- 新建/编辑课题对话框 -->
  <a-modal 
    v-model:open="openEdit" 
    :title="editForm.id ? '编辑课题' : '新建课题'" 
    @ok="handleSave" 
    :confirmLoading="saving"
    :width="700"
  >
    <a-form 
      ref="editFormRef"
      :model="editForm" 
      :rules="editFormRules" 
      layout="vertical"
    >
      <a-form-item label="标题" name="title">
        <a-input v-model:value="editForm.title" placeholder="请输入课题标题" />
      </a-form-item>
      <a-form-item label="描述" name="description">
        <a-textarea v-model:value="editForm.description" :rows="4" placeholder="请输入课题描述" />
      </a-form-item>
      <a-form-item label="容量" name="capacity">
        <a-input-number v-model:value="editForm.capacity" :min="1" :max="10" style="width: 100%" />
        <div style="color: #999; font-size: 12px; margin-top: 4px">可容纳的学生数量</div>
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 课题详情对话框 -->
  <a-modal 
    v-model:open="openDetail" 
    title="课题详情" 
    :footer="null"
    :width="800"
  >
    <a-descriptions :column="2" bordered v-if="detailTopic">
      <a-descriptions-item label="ID">{{ detailTopic.id }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="getStatusColor(detailTopic.status)">{{ getStatusText(detailTopic.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="标题" :span="2">{{ detailTopic.title }}</a-descriptions-item>
      <a-descriptions-item label="描述" :span="2">{{ detailTopic.description || '-' }}</a-descriptions-item>
      <a-descriptions-item label="容量">{{ getCurrentCount(detailTopic.id) }}/{{ detailTopic.capacity || 1 }}</a-descriptions-item>
      <a-descriptions-item label="创建人ID">{{ detailTopic.creatorId || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间" :span="2">
        {{ detailTopic.createdAt ? new Date(detailTopic.createdAt).toLocaleString() : '-' }}
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>

  <!-- 审批对话框 -->
  <a-modal 
    v-model:open="openApprove" 
    title="审批课题" 
    @ok="handleApprove" 
    :confirmLoading="approving"
  >
    <a-form layout="vertical">
      <a-form-item label="决策" name="decision">
        <a-select v-model:value="approveForm.decision" :options="decisionOptions" />
      </a-form-item>
      <a-form-item label="审批意见" name="comment" :rules="[{ required: true, message: '审批意见不能为空' }]">
        <a-textarea v-model:value="approveForm.comment" :rows="4" placeholder="请输入审批意见" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 审批记录对话框 -->
  <a-modal 
    v-model:open="openApprovals" 
    title="审批记录" 
    :footer="null"
    :width="800"
  >
    <a-table 
      :dataSource="approvals" 
      rowKey="id" 
      :pagination="false" 
      :loading="approvalsLoading"
    >
      <a-table-column title="审批人ID" dataIndex="reviewerId" width="120" />
      <a-table-column title="决策" dataIndex="decision" width="100">
        <template #customRender="{ text }">
          <a-tag :color="text === 'APPROVED' ? 'green' : 'red'">
            {{ text === 'APPROVED' ? '通过' : '驳回' }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="意见" dataIndex="comment" />
      <a-table-column title="时间" dataIndex="createdAt" width="180">
        <template #customRender="{ text }">
          {{ text ? new Date(text).toLocaleString() : '-' }}
        </template>
      </a-table-column>
    </a-table>
  </a-modal>

  <!-- 选题情况对话框 -->
  <a-modal 
    v-model:open="openSelections" 
    title="选题情况" 
    :footer="null"
    :width="800"
  >
    <a-table 
      :dataSource="selections" 
      rowKey="id" 
      :pagination="false"
    >
      <a-table-column title="学生ID" dataIndex="studentId" width="120" />
      <a-table-column title="状态" dataIndex="status" width="120">
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
            <a-button 
              v-if="record.status === 'SELECTED' && (auth.role === 'ADMIN' || auth.role === 'TEACHER')" 
              size="small" 
              @click="handleLockSelection(record.id)"
            >
              锁定
            </a-button>
            <a-popconfirm
              v-if="(record.status === 'SELECTED' || record.status === 'LOCKED') && (auth.role === 'ADMIN' || auth.role === 'STUDENT')"
              title="确定要取消该选题吗？"
              @confirm="() => handleCancelSelection(record.id)"
            >
              <a-button size="small" danger>取消</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table-column>
      </a-table>
    </a-modal>

  <!-- 我的选题对话框 -->
  <a-modal 
    v-model:open="openMySelection" 
    title="我的选题" 
    :footer="null"
    :width="800"
  >
    <div v-if="mySelection">
      <a-descriptions :column="2" bordered>
        <a-descriptions-item label="选题ID">{{ mySelection.id }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="getSelectionStatusColor(mySelection.status)">
            {{ getSelectionStatusText(mySelection.status) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="课题ID">{{ mySelection.topicId }}</a-descriptions-item>
        <a-descriptions-item label="选择时间">
          {{ mySelection.createdAt ? new Date(mySelection.createdAt).toLocaleString() : '-' }}
        </a-descriptions-item>
      </a-descriptions>
      <a-divider />
      <a-space>
        <a-button 
          v-if="mySelection.status === 'SELECTED' || mySelection.status === 'LOCKED'" 
          danger
          @click="handleCancelMySelection"
        >
          取消选题
        </a-button>
        <a-button @click="handleViewTopicDetail">查看课题详情</a-button>
      </a-space>
    </div>
    <a-empty v-else description="您还没有选择课题" />
  </a-modal>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import api from '../api';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const approving = ref(false);
const approvalsLoading = ref(false);
const items = ref([]);
const selectionsMap = ref({});
const openEdit = ref(false);
const openDetail = ref(false);
const openApprove = ref(false);
const openApprovals = ref(false);
const openSelections = ref(false);
const editForm = ref({});
const editFormRef = ref(null);
const detailTopic = ref(null);
const approveForm = ref({ decision: 'APPROVED', comment: '' });
const currentApproveTopicId = ref(null);
const approvals = ref([]);
const selections = ref([]);
const currentSelectionsTopicId = ref(null);
const mySelection = ref(null);
const openMySelection = ref(false);

const searchForm = reactive({
  keyword: '',
  status: undefined,
  creatorId: undefined
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条记录`
});

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已提交', value: 'SUBMITTED' },
  { label: '已通过', value: 'APPROVED' },
  { label: '已驳回', value: 'REJECTED' }
];

const decisionOptions = [
  { label: '通过', value: 'APPROVED' },
  { label: '驳回', value: 'REJECTED' }
];

const editFormRules = {
  title: [
    { required: true, message: '请输入课题标题', trigger: 'blur' },
    { min: 3, max: 200, message: '标题长度在3-200个字符之间', trigger: 'blur' }
  ],
  capacity: [
    { required: true, message: '请输入容量', trigger: 'blur' },
    { type: 'number', min: 1, max: 10, message: '容量必须在1-10之间', trigger: 'blur' }
  ]
};

const getStatusColor = (status) => {
  const colors = {
    'DRAFT': 'default',
    'SUBMITTED': 'orange',
    'APPROVED': 'green',
    'REJECTED': 'red'
  };
  return colors[status] || 'default';
};

const getStatusText = (status) => {
  const texts = {
    'DRAFT': '草稿',
    'SUBMITTED': '已提交',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  };
  return texts[status] || status;
};

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

const getCurrentCount = (topicId) => {
  const sels = selectionsMap.value[topicId] || [];
  return sels.filter(s => s.status === 'SELECTED' || s.status === 'LOCKED').length;
};

const isTopicFull = (record) => {
  return getCurrentCount(record.id) >= (record.capacity || 1);
};

const canEdit = (record) => {
  if (auth.role === 'ADMIN') return record.status === 'DRAFT';
  if (auth.role === 'TEACHER') return record.status === 'DRAFT';
  return false;
};

const canDelete = (record) => {
  if (auth.role !== 'ADMIN' && auth.role !== 'TEACHER') return false;
  if (record.status !== 'DRAFT' && record.status !== 'REJECTED') return false;
  return getCurrentCount(record.id) === 0;
};

const fetchTopics = async () => {
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
    const response = await api.get('/topics/page', { params });
    console.log('课题列表响应:', response);
    
    const responseData = response.data;
    if (responseData && responseData.data) {
      const pageData = responseData.data;
      items.value = pageData.content || [];
      pagination.total = pageData.totalElements || 0;
      
      // 加载每个课题的选题情况
      for (const topic of items.value) {
        try {
          const selRes = await api.get(`/topics/${topic.id}/selections`);
          selectionsMap.value[topic.id] = selRes.data?.data || selRes.data || [];
        } catch (e) {
          selectionsMap.value[topic.id] = [];
        }
      }
    } else {
      items.value = [];
      pagination.total = 0;
    }
  } catch (e) {
    console.error('加载课题列表失败:', e);
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
  fetchTopics();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchTopics();
};

const handleReset = () => {
  searchForm.keyword = '';
  searchForm.status = undefined;
  searchForm.creatorId = undefined;
  pagination.current = 1;
  fetchTopics();
};

const handleCreate = () => {
  editForm.value = {
    title: '',
    description: '',
    capacity: 1
  };
  openEdit.value = true;
  if (editFormRef.value) {
    editFormRef.value.clearValidate();
  }
};

const handleEdit = (record) => {
  editForm.value = { ...record };
  openEdit.value = true;
  if (editFormRef.value) {
    editFormRef.value.clearValidate();
  }
};

const handleViewDetail = async (record) => {
  try {
    const response = await api.get(`/topics/${record.id}`);
    const responseData = response.data;
    detailTopic.value = responseData?.data || responseData;
    openDetail.value = true;
  } catch (e) {
    console.error('获取课题详情失败:', e);
    message.error('获取课题详情失败');
  }
};

const handleSave = async () => {
  try {
    await editFormRef.value.validate();
    saving.value = true;
    
    const response = editForm.value.id
      ? await api.put(`/topics/${editForm.value.id}`, {
          title: editForm.value.title,
          description: editForm.value.description,
          capacity: editForm.value.capacity
        })
      : await api.post('/topics', {
          title: editForm.value.title,
          description: editForm.value.description,
          capacity: editForm.value.capacity
        });
    
    console.log('保存课题响应:', response);
    message.success('保存成功');
    openEdit.value = false;
    editForm.value = {};
    await fetchTopics();
  } catch (e) {
    console.error('保存课题失败:', e);
    if (e.errorFields) {
      return;
    }
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '保存失败';
    message.error(errorMsg);
  } finally {
    saving.value = false;
  }
};

const handleDelete = async (id) => {
  try {
    await api.delete(`/topics/${id}`);
    message.success('删除成功');
    await fetchTopics();
  } catch (e) {
    console.error('删除课题失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '删除失败';
    message.error(errorMsg);
  }
};

const handleSubmit = async (id) => {
  try {
    await api.post(`/topics/${id}/submit`);
    message.success('提交成功');
    await fetchTopics();
  } catch (e) {
    console.error('提交课题失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '提交失败';
    message.error(errorMsg);
  }
};

const handleOpenApprove = (id) => {
  currentApproveTopicId.value = id;
  approveForm.value = { decision: 'APPROVED', comment: '' };
  openApprove.value = true;
};

const handleApprove = async () => {
  if (!approveForm.value.comment || approveForm.value.comment.trim() === '') {
    message.warning('请输入审批意见');
    return;
  }
  approving.value = true;
  try {
    await api.post(`/topics/${currentApproveTopicId.value}/approve`, approveForm.value);
    message.success('审批完成');
    openApprove.value = false;
    await fetchTopics();
  } catch (e) {
    console.error('审批失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '审批失败';
    message.error(errorMsg);
  } finally {
    approving.value = false;
  }
};

const handleSelect = async (id) => {
  try {
    await api.post(`/topics/${id}/select`, {});
    message.success('选题成功');
    await fetchTopics();
    if (auth.role === 'STUDENT') {
      await loadMySelection();
    }
  } catch (e) {
    console.error('选题失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '选题失败';
    message.error(errorMsg);
  }
};

const handleCancelMySelection = async () => {
  if (!mySelection.value) return;
  try {
    await api.post(`/topics/selections/${mySelection.value.id}/cancel`);
    message.success('已取消选题');
    await loadMySelection();
    await fetchTopics();
  } catch (e) {
    console.error('取消选题失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '取消选题失败';
    message.error(errorMsg);
  }
};

const handleViewTopicDetail = async () => {
  if (!mySelection.value || !mySelection.value.topicId) return;
  try {
    const response = await api.get(`/topics/${mySelection.value.topicId}`);
    const responseData = response.data;
    detailTopic.value = responseData?.data || responseData;
    openDetail.value = true;
    openMySelection.value = false;
  } catch (e) {
    console.error('获取课题详情失败:', e);
    message.error('获取课题详情失败');
  }
};

const handleShowApprovals = async (id) => {
  openApprovals.value = true;
  approvalsLoading.value = true;
  try {
    const response = await api.get(`/topics/${id}/approvals`);
    approvals.value = response.data?.data || response.data || [];
  } catch (e) {
    console.error('加载审批记录失败:', e);
    message.error('加载审批记录失败');
    approvals.value = [];
  } finally {
    approvalsLoading.value = false;
  }
};

const handleShowSelections = async (id) => {
  currentSelectionsTopicId.value = id;
  openSelections.value = true;
  try {
    const response = await api.get(`/topics/${id}/selections`);
    selections.value = response.data?.data || response.data || [];
  } catch (e) {
    console.error('加载选题情况失败:', e);
    message.error('加载选题情况失败');
    selections.value = [];
  }
};

const handleLockSelection = async (selectionId) => {
  try {
    await api.post(`/topics/selections/${selectionId}/lock`);
    message.success('已锁定');
    await handleShowSelections(currentSelectionsTopicId.value);
    await fetchTopics();
  } catch (e) {
    console.error('锁定失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '锁定失败';
    message.error(errorMsg);
  }
};

const handleCancelSelection = async (selectionId) => {
  try {
    await api.post(`/topics/selections/${selectionId}/cancel`);
    message.success('已取消');
    await handleShowSelections(currentSelectionsTopicId.value);
    await fetchTopics();
    if (auth.role === 'STUDENT') {
      await loadMySelection();
    }
  } catch (e) {
    console.error('取消失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '取消失败';
    message.error(errorMsg);
  }
};

const handleExportTopics = () => {
  window.open('/api/exports/topics', '_blank');
};

const handleViewMySelection = async () => {
  await loadMySelection();
  openMySelection.value = true;
};

const loadMySelection = async () => {
  try {
    const response = await api.get('/topics/my-selection');
    mySelection.value = response.data?.data || null;
  } catch (e) {
    console.error('加载我的选题失败:', e);
    mySelection.value = null;
  }
};

onMounted(() => {
  fetchTopics();
  if (auth.role === 'STUDENT') {
    loadMySelection();
  }
});
</script>
