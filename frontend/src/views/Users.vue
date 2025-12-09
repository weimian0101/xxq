<template>
  <a-card title="用户管理" :loading="loading">
    <template #extra>
      <a-space>
        <a-button type="primary" @click="handleCreate">新建用户</a-button>
        <a-button 
          :disabled="selectedKeys.length === 0" 
          danger 
          @click="handleBatchDelete"
        >
          批量删除
        </a-button>
        <a-button 
          :disabled="selectedKeys.length === 0" 
          @click="openBatchRole = true"
        >
          批量设置角色
        </a-button>
        <a-button @click="openImport = true">导入用户</a-button>
        <a-button @click="fetchUsers">刷新</a-button>
      </a-space>
    </template>

    <!-- 搜索和筛选 -->
    <a-form layout="inline" :model="searchForm" style="margin-bottom: 16px">
      <a-form-item label="关键词">
        <a-input 
          v-model:value="searchForm.keyword" 
          placeholder="用户名/姓名/电话" 
          allow-clear
          style="width: 200px"
          @pressEnter="handleSearch"
        />
      </a-form-item>
      <a-form-item label="角色">
        <a-select 
          v-model:value="searchForm.role" 
          placeholder="全部角色" 
          allow-clear
          style="width: 120px"
          :options="roleOptions"
        />
      </a-form-item>
      <a-form-item label="院系">
        <a-select 
          v-model:value="searchForm.orgId" 
          placeholder="全部院系" 
          allow-clear
          style="width: 200px"
          :options="orgOptions"
          :loading="loadingOrgs"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select 
          v-model:value="searchForm.enabled" 
          placeholder="全部状态" 
          allow-clear
          style="width: 120px"
          :options="[
            { label: '启用', value: true },
            { label: '禁用', value: false }
          ]"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleSearch">搜索</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 用户表格 -->
    <a-table 
      :dataSource="items" 
      rowKey="id" 
      :pagination="pagination"
      :rowSelection="{ selectedRowKeys: selectedKeys, onChange: onSelectChange }"
      @change="handleTableChange"
    >
      <a-table-column title="ID" dataIndex="id" width="80" />
      <a-table-column title="用户名" dataIndex="username" />
      <a-table-column title="姓名" dataIndex="fullName" />
      <a-table-column title="角色" dataIndex="role">
        <template #customRender="{ text }">
          <a-tag :color="getRoleColor(text)">{{ text }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="院系" dataIndex="orgId">
        <template #customRender="{ text }">
          {{ getOrgName(text) }}
        </template>
      </a-table-column>
      <a-table-column title="电话" dataIndex="phone" />
      <a-table-column title="状态" dataIndex="enabled" width="100">
        <template #customRender="{ text }">
          <a-tag :color="text ? 'green' : 'red'">{{ text ? '启用' : '禁用' }}</a-tag>
        </template>
      </a-table-column>
      <a-table-column title="创建时间" dataIndex="createdAt" width="180">
        <template #customRender="{ text }">
          {{ text ? new Date(text).toLocaleString() : '-' }}
        </template>
      </a-table-column>
      <a-table-column title="操作" width="280" fixed="right">
        <template #customRender="{ record }">
          <a-space>
            <a-button size="small" @click="handleEdit(record)">编辑</a-button>
            <a-popconfirm
              :title="record.enabled ? '确定要禁用该用户吗？' : '确定要启用该用户吗？'"
              @confirm="() => toggleEnable(record)"
            >
              <a-button size="small" :type="record.enabled ? 'default' : 'primary'">
                {{ record.enabled ? '禁用' : '启用' }}
              </a-button>
            </a-popconfirm>
            <a-popconfirm
              title="确定要重置该用户的密码为 123456 吗？"
              @confirm="() => resetPwd(record.id)"
            >
              <a-button size="small">重置密码</a-button>
            </a-popconfirm>
            <a-popconfirm
              title="确定要删除该用户吗？此操作不可恢复！"
              @confirm="() => handleDelete(record.id)"
            >
              <a-button size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </a-card>

  <!-- 新建/编辑用户对话框 -->
  <a-modal 
    v-model:open="openEdit" 
    :title="editForm.id ? '编辑用户' : '新建用户'" 
    @ok="handleSaveUser" 
    :confirmLoading="saving"
    :width="600"
  >
    <a-form 
      ref="editFormRef"
      :model="editForm" 
      :rules="editFormRules" 
      layout="vertical"
    >
      <a-form-item label="用户名" name="username" v-if="!editForm.id">
        <a-input v-model:value="editForm.username" placeholder="请输入用户名" />
      </a-form-item>
      <a-form-item label="密码" name="password" v-if="!editForm.id">
        <a-input-password v-model:value="editForm.password" placeholder="请输入密码，默认123456" />
      </a-form-item>
      <a-form-item label="姓名" name="fullName">
        <a-input v-model:value="editForm.fullName" placeholder="请输入姓名" />
      </a-form-item>
      <a-form-item label="角色" name="role">
        <a-select v-model:value="editForm.role" :options="roleOptions" placeholder="请选择角色" />
      </a-form-item>
      <a-form-item label="院系" name="orgId">
        <a-select 
          v-model:value="editForm.orgId" 
          :options="orgOptions" 
          placeholder="请选择院系" 
          allow-clear
          :loading="loadingOrgs"
        />
      </a-form-item>
      <a-form-item label="电话" name="phone">
        <a-input v-model:value="editForm.phone" placeholder="请输入电话" />
      </a-form-item>
      <a-form-item label="签名URL" name="signatureUrl">
        <a-input v-model:value="editForm.signatureUrl" placeholder="请输入签名URL" />
      </a-form-item>
      <a-form-item label="启用状态" name="enabled">
        <a-switch v-model:checked="editForm.enabled" />
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 批量设置角色对话框 -->
  <a-modal 
    v-model:open="openBatchRole" 
    title="批量设置角色" 
    @ok="batchUpdateRole" 
    :confirmLoading="batchSaving"
  >
    <a-form layout="vertical">
      <a-form-item label="选择角色">
        <a-select v-model:value="batchRole" :options="roleOptions" />
      </a-form-item>
      <a-form-item>
        <span>已选择 {{ selectedKeys.length }} 个用户</span>
      </a-form-item>
    </a-form>
  </a-modal>

  <!-- 导入用户对话框 -->
  <a-modal 
    v-model:open="openImport" 
    title="导入用户" 
    @ok="handleImport" 
    :confirmLoading="importing"
    :width="700"
  >
    <a-alert 
      message="导入说明" 
      type="info" 
      style="margin-bottom: 16px"
      :description="`请按照以下格式输入用户信息，每行一个用户，字段用逗号分隔：\n用户名,密码,角色,姓名\n例如：\nstudent001,123456,STUDENT,张三\nteacher001,123456,TEACHER,李老师`"
      show-icon
    />
    <a-textarea 
      v-model:value="importText" 
      :rows="10" 
      placeholder="请输入用户信息，每行一个用户，格式：用户名,密码,角色,姓名"
    />
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
const batchSaving = ref(false);
const importing = ref(false);
const loadingOrgs = ref(false);
const items = ref([]);
const orgs = ref([]);
const openEdit = ref(false);
const openBatchRole = ref(false);
const openImport = ref(false);
const editForm = ref({});
const editFormRef = ref(null);
const selectedKeys = ref([]);
const batchRole = ref('STUDENT');
const importText = ref('');
const searchForm = reactive({
  keyword: '',
  role: undefined,
  orgId: undefined,
  enabled: undefined
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条记录`
});

const roleOptions = [
  { label: '管理员', value: 'ADMIN' },
  { label: '教师', value: 'TEACHER' },
  { label: '学生', value: 'STUDENT' },
  { label: '工作人员', value: 'STAFF' }
];

const orgOptions = computed(() => {
  return orgs.value.map(org => ({
    label: org.name,
    value: org.id
  }));
});

const editFormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '用户名长度在3-50个字符之间', trigger: 'blur' }
  ],
  password: [
    { min: 6, message: '密码长度至少6个字符', trigger: 'blur' }
  ],
  fullName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ]
};

const getRoleColor = (role) => {
  const colors = {
    'ADMIN': 'red',
    'TEACHER': 'blue',
    'STUDENT': 'green',
    'STAFF': 'orange'
  };
  return colors[role] || 'default';
};

const getOrgName = (orgId) => {
  if (!orgId) return '-';
  const org = orgs.value.find(o => o.id === orgId);
  return org ? org.name : orgId;
};

const fetchUsers = async () => {
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
    const response = await api.get('/users/page', { params });
    console.log('用户列表响应:', response);
    
    // 处理响应数据
    const responseData = response.data;
    if (responseData && responseData.data) {
      // ApiResponse包装的Page对象
      const pageData = responseData.data;
      items.value = pageData.content || [];
      pagination.total = pageData.totalElements || 0;
    } else if (responseData && Array.isArray(responseData)) {
      // 直接返回数组的情况
      items.value = responseData;
      pagination.total = responseData.length;
    } else if (responseData && responseData.content) {
      // 直接返回Page对象的情况
      items.value = responseData.content || [];
      pagination.total = responseData.totalElements || 0;
    } else {
      items.value = [];
      pagination.total = 0;
    }
    
    console.log('加载用户列表成功，共', pagination.total, '条记录');
  } catch (e) {
    console.error('加载用户列表失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '加载失败';
    message.error(errorMsg);
    items.value = [];
    pagination.total = 0;
  } finally {
    loading.value = false;
  }
};

const fetchOrgs = async () => {
  loadingOrgs.value = true;
  try {
    const response = await api.get('/orgs');
    console.log('院系列表响应:', response);
    const responseData = response.data;
    // 处理不同的响应结构
    if (responseData && responseData.data) {
      orgs.value = responseData.data;
    } else if (Array.isArray(responseData)) {
      orgs.value = responseData;
    } else {
      orgs.value = [];
    }
    console.log('加载院系列表成功，共', orgs.value.length, '个院系');
  } catch (e) {
    console.error('加载院系失败', e);
    orgs.value = [];
  } finally {
    loadingOrgs.value = false;
  }
};

const onSelectChange = (keys) => {
  selectedKeys.value = keys;
};

const handleTableChange = (pag) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchUsers();
};

const handleSearch = () => {
  pagination.current = 1;
  fetchUsers();
};

const handleReset = () => {
  searchForm.keyword = '';
  searchForm.role = undefined;
  searchForm.orgId = undefined;
  searchForm.enabled = undefined;
  pagination.current = 1;
  fetchUsers();
};

const handleCreate = () => {
  editForm.value = {
    enabled: true,
    role: 'STUDENT',
    username: '',
    password: '',
    fullName: '',
    phone: '',
    signatureUrl: '',
    orgId: undefined
  };
  openEdit.value = true;
  // 清除表单验证状态
  if (editFormRef.value) {
    editFormRef.value.clearValidate();
  }
};

const handleEdit = (record) => {
  editForm.value = { ...record };
  openEdit.value = true;
  // 清除表单验证状态
  if (editFormRef.value) {
    editFormRef.value.clearValidate();
  }
};

const handleSaveUser = async () => {
  try {
    await editFormRef.value.validate();
    saving.value = true;
    
    let response;
    if (editForm.value.id) {
      response = await api.put(`/users/${editForm.value.id}`, {
        fullName: editForm.value.fullName,
        phone: editForm.value.phone,
        signatureUrl: editForm.value.signatureUrl,
        role: editForm.value.role,
        orgId: editForm.value.orgId,
        enabled: editForm.value.enabled
      });
    } else {
      response = await api.post('/users', {
        username: editForm.value.username,
        password: editForm.value.password || '123456',
        fullName: editForm.value.fullName,
        role: editForm.value.role,
        orgId: editForm.value.orgId,
        phone: editForm.value.phone,
        signatureUrl: editForm.value.signatureUrl,
        enabled: editForm.value.enabled ?? true
      });
    }
    
    console.log('保存用户响应:', response);
    message.success('保存成功');
    openEdit.value = false;
    editForm.value = {};
    
    // 保存成功后刷新列表
    await fetchUsers();
  } catch (e) {
    console.error('保存用户失败:', e);
    if (e.errorFields) {
      // 表单验证错误
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
    await api.delete(`/users/${id}`);
    message.success('删除成功');
    await fetchUsers();
  } catch (e) {
    console.error('删除用户失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '删除失败';
    message.error(errorMsg);
  }
};

const handleBatchDelete = async () => {
  if (selectedKeys.value.length === 0) {
    message.warning('请先选择要删除的用户');
    return;
  }
  try {
    await api.delete('/users/batch', {
      data: { userIds: selectedKeys.value }
    });
    message.success('批量删除成功');
    selectedKeys.value = [];
    await fetchUsers();
  } catch (e) {
    console.error('批量删除失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '批量删除失败';
    message.error(errorMsg);
  }
};

const batchUpdateRole = async () => {
  if (selectedKeys.value.length === 0) {
    message.warning('请先选择用户');
    return;
  }
  batchSaving.value = true;
  try {
    await api.post('/users/batch-role', { 
      userIds: selectedKeys.value, 
      role: batchRole.value 
    });
    message.success('批量设置成功');
    openBatchRole.value = false;
    selectedKeys.value = [];
    await fetchUsers();
  } catch (e) {
    console.error('批量设置角色失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '批量设置失败';
    message.error(errorMsg);
  } finally {
    batchSaving.value = false;
  }
};

const toggleEnable = async (record) => {
  try {
    await api.post(`/users/${record.id}/enable`, null, { 
      params: { enabled: !record.enabled } 
    });
    message.success('状态已更新');
    await fetchUsers();
  } catch (e) {
    console.error('更新用户状态失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '更新失败';
    message.error(errorMsg);
  }
};

const resetPwd = async (id) => {
  try {
    await api.post(`/users/${id}/password`, null, { 
      params: { password: '123456' } 
    });
    message.success('密码已重置为 123456');
  } catch (e) {
    message.error(e.response?.data?.message || '重置失败');
  }
};

const handleImport = async () => {
  if (!importText.value.trim()) {
    message.warning('请输入要导入的用户信息');
    return;
  }
  
  importing.value = true;
  try {
    const lines = importText.value.trim().split('\n').filter(line => line.trim());
    const users = lines.map(line => {
      const parts = line.split(',').map(p => p.trim());
      if (parts.length < 4) {
        throw new Error(`格式错误：${line}`);
      }
      return {
        username: parts[0],
        password: parts[1] || '123456',
        role: parts[2],
        fullName: parts[3]
      };
    });
    
    await api.post('/users/import', users);
    message.success(`成功导入 ${users.length} 个用户`);
    openImport.value = false;
    importText.value = '';
    await fetchUsers();
  } catch (e) {
    console.error('导入用户失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '导入失败';
    message.error(errorMsg);
  } finally {
    importing.value = false;
  }
};

onMounted(() => {
  if (auth.role === 'ADMIN') {
    fetchOrgs();
    fetchUsers();
  }
});
</script>
