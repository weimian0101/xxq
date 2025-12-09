<template>
  <a-card title="公告管理" :loading="loading">
    <template #extra>
      <a-space>
        <a-button 
          v-if="auth.role === 'ADMIN' || auth.role === 'TEACHER'" 
          type="primary" 
          @click="handleCreate"
        >
          新建公告
        </a-button>
        <a-button @click="fetchData">刷新</a-button>
      </a-space>
    </template>

    <!-- 搜索和筛选 -->
    <a-form layout="inline" :model="searchForm" style="margin-bottom: 16px">
      <a-form-item label="关键词">
        <a-input 
          v-model:value="searchForm.keyword" 
          placeholder="标题/内容" 
          allow-clear
          style="width: 200px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item label="状态" v-if="auth.role === 'ADMIN' || auth.role === 'TEACHER'">
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

    <!-- 公告表格 -->
    <a-table 
      :dataSource="items" 
      rowKey="id" 
      :pagination="pagination"
      @change="handleTableChange"
    >
      <a-table-column title="ID" dataIndex="id" width="80" />
      <a-table-column title="标题" dataIndex="title" />
      <a-table-column title="内容" dataIndex="content" :ellipsis="true" />
      <a-table-column title="状态" dataIndex="status" width="100">
        <template #customRender="{ text }">
          <a-tag :color="text === 'PUBLISHED' ? 'green' : 'orange'">
            {{ text === 'PUBLISHED' ? '已发布' : '草稿' }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="创建人" dataIndex="createdBy" width="100" />
      <a-table-column title="创建时间" dataIndex="createdAt" width="180">
        <template #customRender="{ text }">
          {{ text ? new Date(text).toLocaleString() : '-' }}
        </template>
      </a-table-column>
      <a-table-column title="发布时间" dataIndex="publishedAt" width="180">
        <template #customRender="{ text }">
          {{ text ? new Date(text).toLocaleString() : '-' }}
        </template>
      </a-table-column>
      <a-table-column title="操作" width="350" fixed="right">
        <template #customRender="{ record }">
          <a-space>
            <a-button size="small" @click="handleViewDetail(record)">详情</a-button>
            <a-button 
              v-if="(auth.role === 'ADMIN' || auth.role === 'TEACHER') && canEdit(record)" 
              size="small" 
              @click="handleEdit(record)"
            >
              编辑
            </a-button>
            <a-button 
              v-if="(auth.role === 'ADMIN' || (auth.role === 'TEACHER' && record.createdBy === auth.user.id)) && record.status === 'DRAFT'" 
              size="small" 
              type="primary" 
              @click="handlePublish(record.id)"
            >
              发布
            </a-button>
            <a-button 
              v-if="(auth.role === 'ADMIN' || (auth.role === 'TEACHER' && record.createdBy === auth.user.id)) && record.status === 'PUBLISHED'" 
              size="small" 
              @click="handleViewStats(record.id)"
            >
              统计
            </a-button>
            <a-popconfirm
              v-if="(auth.role === 'ADMIN' || auth.role === 'TEACHER') && canDelete(record)"
              title="确定要删除该公告吗？此操作不可恢复！"
              @confirm="() => handleDelete(record.id)"
            >
              <a-button size="small" danger>删除</a-button>
            </a-popconfirm>
            <a-button 
              v-if="auth.role === 'STUDENT' && record.status === 'PUBLISHED'" 
              size="small" 
              @click="handleMarkRead(record.id)"
            >
              标记已读
            </a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </a-card>

  <!-- 新建/编辑公告对话框 -->
  <a-modal 
    v-model:open="openEdit" 
    :title="editForm.id ? '编辑公告' : '新建公告'" 
    @ok="handleSave" 
    :confirmLoading="saving"
    :width="800"
  >
    <a-form 
      ref="editFormRef"
      :model="editForm" 
      :rules="editFormRules" 
      layout="vertical"
    >
      <a-form-item label="标题" name="title">
        <a-input v-model:value="editForm.title" placeholder="请输入公告标题" />
      </a-form-item>
      <a-form-item label="内容" name="content">
        <a-textarea v-model:value="editForm.content" :rows="8" placeholder="请输入公告内容" />
      </a-form-item>
      <a-form-item label="状态" v-if="!editForm.id">
        <a-radio-group v-model:value="editForm.status">
          <a-radio value="DRAFT">保存为草稿</a-radio>
          <a-radio value="PUBLISHED">立即发布</a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 公告详情对话框 -->
  <a-modal 
    v-model:open="openDetail" 
    title="公告详情" 
    :footer="null"
    :width="800"
  >
    <a-descriptions :column="2" bordered v-if="detailAnnouncement">
      <a-descriptions-item label="ID">{{ detailAnnouncement.id }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="detailAnnouncement.status === 'PUBLISHED' ? 'green' : 'orange'">
          {{ detailAnnouncement.status === 'PUBLISHED' ? '已发布' : '草稿' }}
        </a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="标题" :span="2">{{ detailAnnouncement.title }}</a-descriptions-item>
      <a-descriptions-item label="内容" :span="2">
        <div style="white-space: pre-wrap">{{ detailAnnouncement.content || '-' }}</div>
      </a-descriptions-item>
      <a-descriptions-item label="创建人ID">{{ detailAnnouncement.createdBy || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">
        {{ detailAnnouncement.createdAt ? new Date(detailAnnouncement.createdAt).toLocaleString() : '-' }}
      </a-descriptions-item>
      <a-descriptions-item label="发布时间" :span="2">
        {{ detailAnnouncement.publishedAt ? new Date(detailAnnouncement.publishedAt).toLocaleString() : '-' }}
      </a-descriptions-item>
    </a-descriptions>
  </a-modal>

  <!-- 统计对话框 -->
  <a-modal 
    v-model:open="openStats" 
    title="已读统计" 
    :footer="null"
  >
    <a-descriptions :column="1" bordered>
      <a-descriptions-item label="公告标题">{{ stats.title || '-' }}</a-descriptions-item>
      <a-descriptions-item label="已读人数">
        <a-statistic :value="stats.totalRead || 0" />
      </a-descriptions-item>
    </a-descriptions>
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
const items = ref([]);
const openEdit = ref(false);
const openDetail = ref(false);
const openStats = ref(false);
const editForm = ref({});
const editFormRef = ref(null);
const detailAnnouncement = ref(null);
const stats = ref({ totalRead: 0, title: '' });

const searchForm = reactive({
  keyword: '',
  status: undefined
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
  { label: '已发布', value: 'PUBLISHED' }
];

const editFormRules = {
  title: [
    { required: true, message: '请输入公告标题', trigger: 'blur' },
    { min: 3, max: 200, message: '标题长度在3-200个字符之间', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入公告内容', trigger: 'blur' },
    { min: 10, message: '内容至少10个字符', trigger: 'blur' }
  ]
};

const canEdit = (record) => {
  // 只有创建者或管理员可以编辑
  if (auth.role === 'ADMIN') return true;
  // 教师只能编辑自己创建的草稿
  if (auth.role === 'TEACHER') {
    return record.status === 'DRAFT' && record.createdBy === auth.user.id;
  }
  return false;
};

const canDelete = (record) => {
  // 只有创建者或管理员可以删除
  if (auth.role === 'ADMIN') return true;
  // 教师只能删除自己创建的草稿
  if (auth.role === 'TEACHER') {
    return record.status === 'DRAFT' && record.createdBy === auth.user.id;
  }
  return false;
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
    console.log('请求参数:', params);
    const response = await api.get('/announcements/page', { params });
    console.log('公告列表响应:', response);
    console.log('响应数据:', response.data);
    
    // 处理响应数据 - 支持多种格式
    const responseData = response.data;
    if (responseData && responseData.data) {
      // ApiResponse包装的Page对象
      const pageData = responseData.data;
      items.value = pageData.content || [];
      pagination.total = pageData.totalElements || 0;
      console.log('加载成功，共', pagination.total, '条记录，当前页', items.value.length, '条');
    } else if (responseData && Array.isArray(responseData)) {
      // 直接返回数组的情况
      items.value = responseData;
      pagination.total = responseData.length;
      console.log('加载成功，共', pagination.total, '条记录');
    } else if (responseData && responseData.content) {
      // 直接返回Page对象的情况
      items.value = responseData.content || [];
      pagination.total = responseData.totalElements || 0;
      console.log('加载成功，共', pagination.total, '条记录，当前页', items.value.length, '条');
    } else {
      console.warn('未识别的响应格式:', responseData);
      items.value = [];
      pagination.total = 0;
    }
  } catch (e) {
    console.error('加载公告列表失败:', e);
    console.error('错误详情:', e.response);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '加载失败';
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
  searchForm.status = undefined;
  pagination.current = 1;
  fetchData();
};

const handleCreate = () => {
  editForm.value = {
    title: '',
    content: '',
    status: 'DRAFT'
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
    const response = await api.get(`/announcements/${record.id}`);
    const responseData = response.data;
    detailAnnouncement.value = responseData?.data || responseData;
    openDetail.value = true;
  } catch (e) {
    console.error('获取公告详情失败:', e);
    message.error('获取公告详情失败');
  }
};

const handleSave = async () => {
  try {
    await editFormRef.value.validate();
    saving.value = true;
    
    if (editForm.value.id) {
      // 编辑
      await api.put(`/announcements/${editForm.value.id}`, {
        title: editForm.value.title,
        content: editForm.value.content
      });
    } else {
      // 新建
      const response = await api.post('/announcements', {
        title: editForm.value.title,
        content: editForm.value.content,
        status: editForm.value.status
      });
      
      // 如果选择立即发布，需要调用发布接口
      if (editForm.value.status === 'PUBLISHED') {
        const responseData = response.data;
        const announcement = responseData?.data || responseData;
        if (announcement && announcement.id) {
          await api.post(`/announcements/${announcement.id}/publish`);
        }
      }
    }
    
    message.success('保存成功');
    openEdit.value = false;
    editForm.value = {};
    await fetchData();
  } catch (e) {
    console.error('保存公告失败:', e);
    if (e.errorFields) {
      return;
    }
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '保存失败';
    message.error(errorMsg);
  } finally {
    saving.value = false;
  }
};

const handlePublish = async (id) => {
  try {
    await api.post(`/announcements/${id}/publish`);
    message.success('发布成功');
    await fetchData();
  } catch (e) {
    console.error('发布失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '发布失败';
    message.error(errorMsg);
  }
};

const handleDelete = async (id) => {
  try {
    await api.delete(`/announcements/${id}`);
    message.success('删除成功');
    await fetchData();
  } catch (e) {
    console.error('删除公告失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '删除失败';
    message.error(errorMsg);
  }
};

const handleViewStats = async (id) => {
  openStats.value = true;
  try {
    const response = await api.get(`/announcements/${id}/stats`);
    stats.value = response.data?.data || response.data || {};
  } catch (e) {
    console.error('加载统计失败:', e);
    message.error('加载统计失败');
    stats.value = { totalRead: 0, title: '' };
  }
};

const handleMarkRead = async (id) => {
  try {
    await api.post(`/announcements/${id}/read`, {});
    message.success('已标记为已读');
  } catch (e) {
    console.error('标记已读失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '标记已读失败';
    message.error(errorMsg);
  }
};

onMounted(() => {
  fetchData();
});
</script>
