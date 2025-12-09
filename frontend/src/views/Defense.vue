<template>
  <a-space direction="vertical" style="width:100%" size="large">
    <!-- 分组列表 -->
    <a-card title="分组列表" :loading="loadingGroups" :extra="renderGroupActions()">
      <a-space style="margin-bottom:12px">
        <a-input 
          v-model:value="groupSearch.keyword" 
          placeholder="搜索分组名称" 
          allow-clear
          style="width:200px"
          @pressEnter="handleGroupSearch"
        />
        <a-select 
          v-model:value="groupSearch.type" 
          placeholder="类型筛选" 
          allow-clear
          style="width:150px"
          :options="typeOptions"
        />
        <a-button type="primary" @click="handleGroupSearch">搜索</a-button>
        <a-button @click="handleGroupReset">重置</a-button>
      </a-space>
      
      <a-table 
        :dataSource="groups" 
        :pagination="groupPagination"
        rowKey="id"
        @change="handleGroupTableChange"
      >
        <a-table-column title="ID" dataIndex="id" width="80" />
        <a-table-column title="名称" dataIndex="name" />
        <a-table-column title="类型" dataIndex="type" width="120">
          <template #customRender="{ text }">
            <a-tag :color="text === 'FINAL' ? 'blue' : 'green'">
              {{ text === 'FINAL' ? '毕业答辩' : '开题答辩' }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="容量" dataIndex="capacity" width="100" />
        <a-table-column title="成员数" width="100">
          <template #customRender="{ record }">
            {{ memberCountMap[record.id] ?? '-' }}
          </template>
        </a-table-column>
        <a-table-column title="创建时间" dataIndex="createdAt" width="180">
          <template #customRender="{ text }">
            {{ text ? new Date(text).toLocaleString() : '-' }}
          </template>
        </a-table-column>
        <a-table-column title="操作" width="250" fixed="right">
          <template #customRender="{ record }">
            <a-space>
              <a-button size="small" @click="() => viewGroupMembers(record)">查看成员</a-button>
              <a-button 
                v-if="canManageGroup" 
                size="small" 
                @click="() => editGroup(record)"
              >
                编辑
              </a-button>
              <a-popconfirm
                v-if="canManageGroup"
                title="确定要删除该分组吗？"
                @confirm="() => removeGroup(record.id)"
              >
                <a-button size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <!-- 分组成员与成绩 -->
    <a-card title="分组成员与成绩" :loading="loadingMembers">
      <a-space style="margin-bottom:12px">
        <a-select
          style="width:250px"
          v-model:value="selectedGroup"
          :options="groups.map(g => ({ label: `${g.name} (${g.type === 'FINAL' ? '毕业答辩' : '开题答辩'})`, value: g.id }))"
          placeholder="请选择分组"
          @change="loadMembers"
          allow-clear
        />
        <a-button size="small" @click="() => selectedGroup && loadMembers(selectedGroup)">刷新</a-button>
        <a-button 
          v-if="canManageGroup && selectedGroup" 
          size="small" 
          type="primary" 
          @click="openAddMember"
        >
          添加成员
        </a-button>
        <a-button 
          v-if="canManageGroup && selectedGroup" 
          size="small" 
          @click="openBatchScore"
        >
          批量录入成绩
        </a-button>
      </a-space>
      
      <a-table
        :dataSource="membersWithDetails"
        :columns="scoreColumns"
        rowKey="member.id"
        :pagination="false"
        :loading="loadingMembers"
      >
      </a-table>
    </a-card>

    <!-- 评阅任务 -->
    <a-card title="评阅任务" :loading="loadingReviews" :extra="renderReviewActions()">
      <a-space style="margin-bottom:12px">
        <a-select 
          v-model:value="reviewFilter.status" 
          style="width:120px" 
          placeholder="状态" 
          allowClear
        >
          <a-select-option value="PENDING">待处理</a-select-option>
          <a-select-option value="DONE">已完成</a-select-option>
        </a-select>
        <a-select 
          v-model:value="reviewFilter.type" 
          style="width:120px" 
          placeholder="类型" 
          allowClear
        >
          <a-select-option value="CROSS">交叉评阅</a-select-option>
          <a-select-option value="ADVISOR">指导教师</a-select-option>
        </a-select>
        <a-button size="small" type="primary" @click="fetchReviews">筛选</a-button>
        <a-button size="small" @click="handleReviewReset">重置</a-button>
      </a-space>
      
      <a-table 
        :dataSource="reviews" 
        :pagination="reviewPagination"
        rowKey="id"
        @change="handleReviewTableChange"
      >
        <a-table-column title="ID" dataIndex="id" width="80" />
        <a-table-column title="学生ID" dataIndex="studentId" width="100" />
        <a-table-column title="课题ID" dataIndex="topicId" width="100" />
        <a-table-column title="类型" dataIndex="type" width="120">
          <template #customRender="{ text }">
            <a-tag :color="text === 'CROSS' ? 'orange' : 'blue'">
              {{ text === 'CROSS' ? '交叉评阅' : '指导教师' }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="状态" dataIndex="status" width="100">
          <template #customRender="{ text }">
            <a-tag :color="text === 'DONE' ? 'green' : 'orange'">
              {{ text === 'DONE' ? '已完成' : '待处理' }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="评分" dataIndex="score" width="100">
          <template #customRender="{ text }">
            {{ text != null ? text.toFixed(1) : '-' }}
          </template>
        </a-table-column>
        <a-table-column title="创建时间" dataIndex="createdAt" width="180">
          <template #customRender="{ text }">
            {{ text ? new Date(text).toLocaleString() : '-' }}
          </template>
        </a-table-column>
        <a-table-column title="操作" width="200" fixed="right">
          <template #customRender="{ record }">
            <a-space>
              <a-button 
                v-if="record.status === 'PENDING'" 
                size="small" 
                type="primary" 
                @click="() => openCompleteModal(record)"
              >
                完成评阅
              </a-button>
              <a-button 
                size="small" 
                @click="() => viewReviewDetail(record)"
              >
                详情
              </a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <!-- 成绩统计 -->
    <a-card title="成绩统计" v-if="canManageGroup" :loading="loadingStats">
      <a-table 
        :dataSource="gradeStats" 
        :pagination="false" 
        rowKey="studentId"
      >
        <a-table-column title="学生ID" dataIndex="studentId" width="120" />
        <a-table-column title="平均分" dataIndex="avgScore" width="120">
          <template #customRender="{ text }">
            {{ text != null ? text.toFixed(2) : '-' }}
          </template>
        </a-table-column>
        <a-table-column title="评分次数" dataIndex="count" width="120" />
      </a-table>
      <a-button style="margin-top:12px" @click="fetchGradeStats">刷新统计</a-button>
    </a-card>
  </a-space>

  <!-- 自动分组对话框 -->
  <a-modal 
    v-model:open="openAuto" 
    title="自动分组" 
    @ok="doAutoAssign" 
    :confirmLoading="autoLoading"
  >
    <a-form layout="vertical">
      <a-form-item label="类型" required>
        <a-select v-model:value="autoForm.type" :options="typeOptions" />
      </a-form-item>
      <a-form-item label="容量" required>
        <a-input-number 
          v-model:value="autoForm.capacity" 
          :min="2" 
          :max="100"
          style="width:100%;" 
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 编辑分组对话框 -->
  <a-modal 
    v-model:open="openEditGroup" 
    title="编辑分组" 
    @ok="doEditGroup" 
    :confirmLoading="editingGroup"
  >
    <a-form layout="vertical" :model="editGroupForm">
      <a-form-item label="名称" required>
        <a-input v-model:value="editGroupForm.name" placeholder="请输入分组名称" />
      </a-form-item>
      <a-form-item label="类型" required>
        <a-select v-model:value="editGroupForm.type" :options="typeOptions" />
      </a-form-item>
      <a-form-item label="容量" required>
        <a-input-number 
          v-model:value="editGroupForm.capacity" 
          :min="1" 
          :max="100"
          style="width:100%;" 
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 添加成员对话框 -->
  <a-modal 
    v-model:open="openAddMember" 
    title="添加成员" 
    @ok="doAddMember" 
    :confirmLoading="addingMember"
  >
    <a-form layout="vertical" :model="memberForm">
      <a-form-item label="学生ID" required>
        <a-input-number 
          v-model:value="memberForm.studentId" 
          style="width:100%;" 
          :min="1"
        />
      </a-form-item>
      <a-form-item label="课题ID">
        <a-input-number 
          v-model:value="memberForm.topicId" 
          style="width:100%;" 
          :min="1"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 完成评阅对话框 -->
  <a-modal 
    v-model:open="openCompleteReview" 
    title="完成评阅" 
    @ok="doCompleteReview" 
    :confirmLoading="completingReview"
  >
    <a-form layout="vertical" :model="reviewForm">
      <a-form-item label="评分" required>
        <a-input-number 
          v-model:value="reviewForm.score" 
          :min="0" 
          :max="100" 
          style="width:100%;"
          :precision="1"
        />
      </a-form-item>
      <a-form-item label="评阅意见">
        <a-textarea 
          v-model:value="reviewForm.comment" 
          rows="4" 
          :maxlength="1000"
          show-count
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 评阅详情对话框 -->
  <a-modal 
    v-model:open="openReviewDetail" 
    title="评阅详情" 
    :footer="null"
    :width="700"
  >
    <a-descriptions :column="2" bordered v-if="currentReview">
      <a-descriptions-item label="ID">{{ currentReview.id }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="currentReview.status === 'DONE' ? 'green' : 'orange'">
          {{ currentReview.status === 'DONE' ? '已完成' : '待处理' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="类型">
        <a-tag :color="currentReview.type === 'CROSS' ? 'orange' : 'blue'">
          {{ currentReview.type === 'CROSS' ? '交叉评阅' : '指导教师' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="学生ID">{{ currentReview.studentId }}</a-descriptions-item>
      <a-descriptions-item label="课题ID">{{ currentReview.topicId }}</a-descriptions-item>
      <a-descriptions-item label="评阅人ID">{{ currentReview.reviewerId }}</a-descriptions-item>
      <a-descriptions-item label="评分" :span="2">
        {{ currentReview.score != null ? currentReview.score.toFixed(1) : '未评分' }}
      </a-descriptions-item>
      <a-descriptions-item label="评阅意见" :span="2">
        <div style="white-space: pre-wrap">{{ currentReview.comment || '无' }}</div>
      </a-descriptions-item>
      <a-descriptions-item label="创建时间" :span="2">
        {{ currentReview.createdAt ? new Date(currentReview.createdAt).toLocaleString() : '-' }}
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>
</template>

<script setup>
import { h, onMounted, ref, reactive, computed } from 'vue';
import api from '../api';
import { Button, message, Space, Select, Table, InputNumber, Input, Descriptions } from 'ant-design-vue';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const groups = ref([]);
const reviews = ref([]);
const selectedGroup = ref(null);
const membersWithDetails = ref([]);
const gradeStats = ref([]);
const loadingGroups = ref(false);
const loadingMembers = ref(false);
const loadingReviews = ref(false);
const loadingStats = ref(false);

// 分组搜索
const groupSearch = reactive({
  keyword: '',
  type: undefined
});

// 分页
const groupPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条记录`
});

const reviewPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条记录`
});

// 对话框状态
const openAuto = ref(false);
const autoLoading = ref(false);
const autoForm = ref({ type: 'FINAL', capacity: 8 });

const openEditGroup = ref(false);
const editingGroup = ref(false);
const editGroupForm = ref({});

const openAddMember = ref(false);
const addingMember = ref(false);
const memberForm = ref({ studentId: null, topicId: null });

const openCompleteReview = ref(false);
const completingReview = ref(false);
const reviewForm = ref({ score: null, comment: '' });
const currentReviewId = ref(null);
const currentReview = ref(null);
const openReviewDetail = ref(false);

const reviewFilter = ref({ status: null, type: null });

const typeOptions = [
  { label: '开题答辩', value: 'OPENING' },
  { label: '毕业答辩', value: 'FINAL' }
];

const canManageGroup = computed(() => auth.role === 'ADMIN');
const canReview = computed(() => auth.role === 'TEACHER' || auth.role === 'ADMIN');

// 成员数量映射
const memberCountMap = ref({});

// 加载成员数量
const loadMemberCounts = async () => {
  for (const group of groups.value) {
    try {
      const { data } = await api.get(`/defense/groups/${group.id}/detail`);
      const detail = data?.data || data;
      memberCountMap.value[group.id] = detail.memberCount || 0;
    } catch {
      memberCountMap.value[group.id] = 0;
    }
  }
};

// 分组列表
const fetchGroups = async () => {
  loadingGroups.value = true;
  try {
    const params = {
      page: groupPagination.current - 1,
      size: groupPagination.pageSize,
      ...groupSearch
    };
    Object.keys(params).forEach(key => {
      if (params[key] === '' || params[key] === undefined || params[key] === null) {
        delete params[key];
      }
    });
    
    const response = await api.get('/defense/groups/page', { params });
    const responseData = response.data;
    
    if (responseData && responseData.data) {
      const pageData = responseData.data;
      groups.value = pageData.content || [];
      groupPagination.total = pageData.totalElements || 0;
      // 加载成员数量
      loadMemberCounts();
    } else if (responseData && Array.isArray(responseData)) {
      groups.value = responseData;
      groupPagination.total = responseData.length;
      loadMemberCounts();
    } else {
      groups.value = [];
      groupPagination.total = 0;
    }
  } catch (e) {
    console.error('加载分组失败:', e);
    message.error(e.response?.data?.message || '加载分组失败');
    groups.value = [];
    groupPagination.total = 0;
  } finally {
    loadingGroups.value = false;
  }
};

const handleGroupTableChange = (pag) => {
  groupPagination.current = pag.current;
  groupPagination.pageSize = pag.pageSize;
  fetchGroups();
};

const handleGroupSearch = () => {
  groupPagination.current = 1;
  fetchGroups();
};

const handleGroupReset = () => {
  groupSearch.keyword = '';
  groupSearch.type = undefined;
  groupPagination.current = 1;
  fetchGroups();
};

const viewGroupMembers = (record) => {
  selectedGroup.value = record.id;
  loadMembers(record.id);
  // 滚动到成员列表
  setTimeout(() => {
    const titleElements = document.querySelectorAll('.ant-card-head-title');
    let targetElement = null;
    for (const titleElement of titleElements) {
      if (titleElement.textContent.includes('分组成员与成绩')) {
        targetElement = titleElement.closest('.ant-card');
        break;
      }
    }
    if (targetElement) {
      targetElement.scrollIntoView({ behavior: 'smooth' });
    }
  }, 100);
};

const editGroup = (record) => {
  editGroupForm.value = {
    id: record.id,
    name: record.name,
    type: record.type,
    capacity: record.capacity
  };
  openEditGroup.value = true;
};

const doEditGroup = async () => {
  if (!editGroupForm.value.name || !editGroupForm.value.type || !editGroupForm.value.capacity) {
    message.warning('请填写完整信息');
    return;
  }
  editingGroup.value = true;
  try {
    await api.put(`/defense/groups/${editGroupForm.value.id}`, {
      name: editGroupForm.value.name,
      type: editGroupForm.value.type,
      capacity: editGroupForm.value.capacity
    });
    message.success('编辑成功');
    openEditGroup.value = false;
    fetchGroups();
  } catch (e) {
    message.error(e.response?.data?.message || '编辑失败');
  } finally {
    editingGroup.value = false;
  }
};

const renderGroupActions = () =>
  h(Space, {}, () => [
    canManageGroup.value ? h(Button, { type: 'primary', size: 'small', onClick: () => openAuto.value = true }, () => '自动分组') : null,
    canManageGroup.value ? h(Button, { size: 'small', onClick: () => autoCrossReview() }, () => '自动生成交叉评阅') : null,
    canManageGroup.value ? h(Button, { size: 'small', onClick: () => handleExportDefenseData() }, () => '导出答辩数据') : null,
    h(Button, { size: 'small', onClick: fetchGroups }, () => '刷新')
  ].filter(Boolean));

const handleExportDefenseData = () => {
  window.open('/api/exports/groups', '_blank');
  window.open('/api/exports/scores', '_blank');
  window.open('/api/exports/reviews', '_blank');
};

const doAutoAssign = async () => {
  if (!autoForm.value.type || !autoForm.value.capacity) {
    message.warning('请填写完整信息');
    return;
  }
  autoLoading.value = true;
  try {
    const { data } = await api.post('/defense/groups/auto', autoForm.value);
    message.success(`已自动分组，共创建 ${data?.data?.length || 0} 个成员`);
    openAuto.value = false;
    fetchGroups();
  } catch (e) {
    message.error(e.response?.data?.message || '分组失败');
  } finally {
    autoLoading.value = false;
  }
};

const autoCrossReview = async () => {
  try {
    const { data } = await api.post('/defense/reviews/auto-cross');
    message.success(`已自动生成交叉评阅，共 ${data?.data?.length || 0} 个任务`);
    fetchReviews();
  } catch (e) {
    message.error(e.response?.data?.message || '生成失败');
  }
};

// 成员与成绩
const loadMembers = async (groupId) => {
  if (!groupId) return;
  loadingMembers.value = true;
  try {
    const { data } = await api.get(`/defense/groups/${groupId}/members/detail`);
    const responseData = data?.data || data;
    membersWithDetails.value = Array.isArray(responseData) ? responseData : [];
  } catch (e) {
    console.error('加载成员失败:', e);
    message.error(e.response?.data?.message || '加载失败');
    membersWithDetails.value = [];
  } finally {
    loadingMembers.value = false;
  }
};

const doAddMember = async () => {
  if (!selectedGroup.value) {
    message.warning('请先选择分组');
    return;
  }
  if (!memberForm.value.studentId) {
    message.warning('请输入学生ID');
    return;
  }
  addingMember.value = true;
  try {
    await api.post(`/defense/groups/${selectedGroup.value}/members`, memberForm.value);
    message.success('已添加');
    openAddMember.value = false;
    memberForm.value = { studentId: null, topicId: null };
    loadMembers(selectedGroup.value);
  } catch (e) {
    message.error(e.response?.data?.message || '添加失败');
  } finally {
    addingMember.value = false;
  }
};

const removeMember = async (memberId) => {
  if (!selectedGroup.value) {
    message.warning('请先选择分组');
    return;
  }
  try {
    await api.delete(`/defense/groups/${selectedGroup.value}/members/${memberId}`);
    message.success('已移除');
    loadMembers(selectedGroup.value);
  } catch (e) {
    message.error(e.response?.data?.message || '移除失败');
  }
};

const submitScore = async (studentId, score, comment) => {
  if (!selectedGroup.value) {
    message.warning('请先选择分组');
    return;
  }
  if (score == null) {
    message.warning('请填写成绩');
    return;
  }
  try {
    await api.post('/defense/scores', {
      groupId: selectedGroup.value,
      studentId,
      score,
      comment: comment || ''
    });
    message.success('已保存');
    loadMembers(selectedGroup.value);
  } catch (e) {
    message.error(e.response?.data?.message || '保存失败');
  }
};

const scoreColumns = [
  { 
    title: '学生ID', 
    dataIndex: ['member', 'studentId'],
    width: 100
  },
  {
    title: '学生姓名',
    dataIndex: 'studentName',
    width: 150
  },
  {
    title: '课题ID',
    dataIndex: ['member', 'topicId'],
    width: 100
  },
  {
    title: '成绩',
    width: 150,
    customRender: ({ record }) => {
      const score = record.score;
      return h(InputNumber, {
        value: score,
        min: 0,
        max: 100,
        precision: 1,
        style: { width: '100%' },
        onChange: (val) => {
          record.score = val;
        }
      });
    }
  },
  {
    title: '备注',
    width: 200,
    customRender: ({ record }) => {
      const comment = record.comment || '';
      return h(Input, {
        value: comment,
        placeholder: '请输入备注',
        onChange: (e) => {
          record.comment = e.target.value;
        }
      });
    }
  },
  {
    title: '操作',
    width: 150,
    fixed: 'right',
    customRender: ({ record }) => h(Space, {}, () => [
      h(Button, {
        size: 'small',
        type: 'primary',
        onClick: () => submitScore(record.member.studentId, record.score, record.comment)
      }, () => '保存'),
      canManageGroup.value ? h(Button, {
        size: 'small',
        danger: true,
        onClick: () => removeMember(record.member.id)
      }, () => '移除') : null
    ].filter(Boolean))
  }
];

// 评阅任务
const fetchReviews = async () => {
  if (!canReview.value) return;
  loadingReviews.value = true;
  try {
    const params = {
      page: reviewPagination.current - 1,
      size: reviewPagination.pageSize,
      reviewerId: auth.user.id
    };
    if (reviewFilter.value.status) params.status = reviewFilter.value.status;
    if (reviewFilter.value.type) params.type = reviewFilter.value.type;
    
    const response = await api.get('/defense/reviews/page', { params });
    const responseData = response.data;
    
    if (responseData && responseData.data) {
      const pageData = responseData.data;
      reviews.value = pageData.content || [];
      reviewPagination.total = pageData.totalElements || 0;
    } else if (responseData && Array.isArray(responseData)) {
      reviews.value = responseData;
      reviewPagination.total = responseData.length;
    } else {
      reviews.value = [];
      reviewPagination.total = 0;
    }
  } catch (e) {
    console.error('加载评阅任务失败:', e);
    message.error(e.response?.data?.message || '加载评阅任务失败');
    reviews.value = [];
    reviewPagination.total = 0;
  } finally {
    loadingReviews.value = false;
  }
};

const handleReviewTableChange = (pag) => {
  reviewPagination.current = pag.current;
  reviewPagination.pageSize = pag.pageSize;
  fetchReviews();
};

const handleReviewReset = () => {
  reviewFilter.value.status = null;
  reviewFilter.value.type = null;
  reviewPagination.current = 1;
  fetchReviews();
};

const openCompleteModal = (record) => {
  currentReviewId.value = record.id;
  reviewForm.value = { score: null, comment: '' };
  openCompleteReview.value = true;
};

const doCompleteReview = async () => {
  if (reviewForm.value.score == null) {
    message.warning('请输入评分');
    return;
  }
  completingReview.value = true;
  try {
    await api.post(`/defense/reviews/${currentReviewId.value}/complete`, {
      score: reviewForm.value.score,
      comment: reviewForm.value.comment || ''
    });
    message.success('已完成');
    openCompleteReview.value = false;
    fetchReviews();
  } catch (e) {
    message.error(e.response?.data?.message || '操作失败');
  } finally {
    completingReview.value = false;
  }
};

const viewReviewDetail = (record) => {
  currentReview.value = record;
  openReviewDetail.value = true;
};

const renderReviewActions = () =>
  h(Space, {}, () => [
    h(Button, { size: 'small', onClick: fetchReviews }, () => '刷新')
  ]);

// 成绩统计
const fetchGradeStats = async () => {
  if (!canManageGroup.value) return;
  loadingStats.value = true;
  try {
    const { data } = await api.get('/defense/grades');
    const responseData = data?.data || data;
    gradeStats.value = Array.isArray(responseData) ? responseData : [];
  } catch (e) {
    console.error('加载成绩统计失败:', e);
    gradeStats.value = [];
  } finally {
    loadingStats.value = false;
  }
};

onMounted(() => {
  fetchGroups();
  if (canReview.value) {
    fetchReviews();
  }
  if (canManageGroup.value) {
    fetchGradeStats();
  }
});
</script>
