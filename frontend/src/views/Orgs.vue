<template>
  <a-card title="组织管理" :loading="loading">
    <template #extra>
      <a-space>
        <a-button type="primary" @click="handleCreate">新建组织</a-button>
        <a-button 
          :disabled="selectedKeys.length === 0" 
          danger 
          @click="handleBatchDelete"
        >
          批量删除
        </a-button>
        <a-radio-group v-model:value="viewMode" @change="handleViewModeChange">
          <a-radio-button value="table">列表视图</a-radio-button>
          <a-radio-button value="tree">树形视图</a-radio-button>
        </a-radio-group>
        <a-button @click="fetchData">刷新</a-button>
      </a-space>
    </template>

    <!-- 搜索和筛选 -->
    <a-form layout="inline" :model="searchForm" style="margin-bottom: 16px">
      <a-form-item label="关键词">
        <a-input 
          v-model:value="searchForm.keyword" 
          placeholder="组织名称" 
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
          style="width: 120px"
          :options="typeOptions"
        />
      </a-form-item>
      <a-form-item label="上级组织" v-if="viewMode === 'table'">
        <a-select 
          v-model:value="searchForm.parentId" 
          placeholder="全部组织" 
          allow-clear
          style="width: 200px"
          :options="orgOptions"
          :loading="loadingOrgs"
          show-search
          :filter-option="filterOrgOption"
        />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleSearch">搜索</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <!-- 列表视图 -->
    <a-table 
      v-if="viewMode === 'table'"
      :dataSource="items" 
      rowKey="id" 
      :pagination="pagination"
      :rowSelection="{ selectedRowKeys: selectedKeys, onChange: onSelectChange }"
      @change="handleTableChange"
    >
      <a-table-column title="ID" dataIndex="id" width="80" />
      <a-table-column title="名称" dataIndex="name" />
      <a-table-column title="类型" dataIndex="type" width="120">
        <template #customRender="{ text }">
          <a-tag :color="text === 'COLLEGE' ? 'blue' : 'green'">
            {{ text === 'COLLEGE' ? '学院' : '系部' }}
          </a-tag>
        </template>
      </a-table-column>
      <a-table-column title="上级组织" dataIndex="parentId" width="200">
        <template #customRender="{ text }">
          {{ getOrgName(text) }}
        </template>
      </a-table-column>
      <a-table-column title="操作" width="200" fixed="right">
        <template #customRender="{ record }">
          <a-space>
            <a-button size="small" @click="handleEdit(record)">编辑</a-button>
            <a-popconfirm
              title="确定要删除该组织吗？此操作不可恢复！"
              @confirm="() => handleDelete(record.id)"
            >
              <a-button size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </a-table-column>
    </a-table>

    <!-- 树形视图 -->
    <a-tree
      v-if="viewMode === 'tree'"
      :tree-data="treeData"
      :field-names="{ children: 'children', title: 'name', key: 'id' }"
      :default-expand-all="false"
      :show-line="true"
    >
      <template #title="{ name, id, type }">
        <span style="margin-right: 8px">{{ name }}</span>
        <a-tag :color="type === 'COLLEGE' ? 'blue' : 'green'" size="small">
          {{ type === 'COLLEGE' ? '学院' : '系部' }}
        </a-tag>
        <a-space style="margin-left: 16px">
          <a-button size="small" type="link" @click="() => handleEditById(id)">编辑</a-button>
          <a-popconfirm
            title="确定要删除该组织吗？"
            @confirm="() => handleDelete(id)"
          >
            <a-button size="small" type="link" danger>删除</a-button>
          </a-popconfirm>
        </a-space>
      </template>
    </a-tree>
  </a-card>

  <!-- 新建/编辑组织对话框 -->
  <a-modal 
    v-model:open="openEdit" 
    :title="editForm.id ? '编辑组织' : '新建组织'" 
    @ok="handleSave" 
    :confirmLoading="saving"
    :width="600"
  >
    <a-form 
      ref="editFormRef"
      :model="editForm" 
      :rules="editFormRules" 
      layout="vertical"
    >
      <a-form-item label="名称" name="name">
        <a-input v-model:value="editForm.name" placeholder="请输入组织名称" />
      </a-form-item>
      <a-form-item label="类型" name="type">
        <a-select v-model:value="editForm.type" :options="typeOptions" placeholder="请选择类型" />
      </a-form-item>
      <a-form-item label="上级组织" name="parentId">
        <a-select 
          v-model:value="editForm.parentId" 
          :options="parentOrgOptions" 
          placeholder="请选择上级组织（可选）" 
          allow-clear
          :loading="loadingOrgs"
          show-search
          :filter-option="filterOrgOption"
        />
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
const saving = ref(false);
const loadingOrgs = ref(false);
const items = ref([]);
const orgs = ref([]);
const treeData = ref([]);
const openEdit = ref(false);
const editForm = ref({});
const editFormRef = ref(null);
const selectedKeys = ref([]);
const viewMode = ref('table');

const searchForm = reactive({
  keyword: '',
  type: undefined,
  parentId: undefined
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条记录`
});

const typeOptions = [
  { label: '学院', value: 'COLLEGE' },
  { label: '系部', value: 'DEPT' }
];

const orgOptions = computed(() => {
  return orgs.value.map(org => ({
    label: org.name,
    value: org.id
  }));
});

const parentOrgOptions = computed(() => {
  // 编辑时，排除自己和自己可能的子组织（防止循环引用）
  const currentId = editForm.value.id;
  return orgs.value
    .filter(org => !currentId || org.id !== currentId)
    .map(org => ({
      label: org.name,
      value: org.id
    }));
});

const editFormRules = {
  name: [
    { required: true, message: '请输入组织名称', trigger: 'blur' },
    { min: 2, max: 50, message: '组织名称长度在2-50个字符之间', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择组织类型', trigger: 'change' }
  ]
};

const getOrgName = (orgId) => {
  if (!orgId) return '-';
  const org = orgs.value.find(o => o.id === orgId);
  return org ? org.name : orgId;
};

const filterOrgOption = (input, option) => {
  return option.label.toLowerCase().indexOf(input.toLowerCase()) >= 0;
};

const fetchData = async () => {
  if (viewMode.value === 'tree') {
    await fetchTree();
  } else {
    await fetchList();
  }
};

const fetchList = async () => {
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
    const response = await api.get('/orgs/page', { params });
    console.log('组织列表响应:', response);
    
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
    console.error('加载组织列表失败:', e);
    const errorMsg = e.response?.data?.message || e.message || '加载失败';
    message.error(errorMsg);
    items.value = [];
    pagination.total = 0;
  } finally {
    loading.value = false;
  }
};

const fetchTree = async () => {
  loading.value = true;
  try {
    const response = await api.get('/orgs/tree');
    console.log('组织树响应:', response);
    
    const responseData = response.data;
    const orgList = responseData?.data || responseData || [];
    
    // 构建树形结构
    const buildTree = (parentId = null) => {
      return orgList
        .filter(org => {
          if (parentId === null) return org.parentId === null;
          return org.parentId === parentId;
        })
        .map(org => {
          const children = buildTree(org.id);
          return {
            ...org,
            children: children.length > 0 ? children : undefined
          };
        });
    };
    
    treeData.value = buildTree();
  } catch (e) {
    console.error('加载组织树失败:', e);
    const errorMsg = e.response?.data?.message || e.message || '加载失败';
    message.error(errorMsg);
    treeData.value = [];
  } finally {
    loading.value = false;
  }
};

const fetchOrgs = async () => {
  loadingOrgs.value = true;
  try {
    const response = await api.get('/orgs');
    console.log('组织列表响应（用于下拉选择）:', response);
    const responseData = response.data;
    if (responseData && responseData.data) {
      orgs.value = responseData.data;
    } else if (Array.isArray(responseData)) {
      orgs.value = responseData;
    } else {
      orgs.value = [];
    }
  } catch (e) {
    console.error('加载组织列表失败', e);
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
  fetchList();
};

const handleViewModeChange = () => {
  selectedKeys.value = [];
  fetchData();
};

const handleSearch = () => {
  if (viewMode.value === 'table') {
    pagination.current = 1;
    fetchList();
  } else {
    fetchTree();
  }
};

const handleReset = () => {
  searchForm.keyword = '';
  searchForm.type = undefined;
  searchForm.parentId = undefined;
  if (viewMode.value === 'table') {
    pagination.current = 1;
    fetchList();
  } else {
    fetchTree();
  }
};

const handleCreate = () => {
  editForm.value = {
    name: '',
    type: 'COLLEGE',
    parentId: undefined
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

const handleEditById = async (id) => {
  try {
    const response = await api.get(`/orgs/${id}`);
    const responseData = response.data;
    const org = responseData?.data || responseData;
    handleEdit(org);
  } catch (e) {
    console.error('获取组织详情失败:', e);
    message.error('获取组织详情失败');
  }
};

const handleSave = async () => {
  try {
    await editFormRef.value.validate();
    saving.value = true;
    
    const response = editForm.value.id
      ? await api.put(`/orgs/${editForm.value.id}`, {
          name: editForm.value.name,
          type: editForm.value.type,
          parentId: editForm.value.parentId
        })
      : await api.post('/orgs', {
          name: editForm.value.name,
          type: editForm.value.type,
          parentId: editForm.value.parentId
        });
    
    console.log('保存组织响应:', response);
    message.success('保存成功');
    openEdit.value = false;
    editForm.value = {};
    
    // 保存成功后刷新数据
    await fetchData();
    await fetchOrgs(); // 刷新下拉选择列表
  } catch (e) {
    console.error('保存组织失败:', e);
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
    await api.delete(`/orgs/${id}`);
    message.success('删除成功');
    await fetchData();
    await fetchOrgs(); // 刷新下拉选择列表
  } catch (e) {
    console.error('删除组织失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '删除失败';
    message.error(errorMsg);
  }
};

const handleBatchDelete = async () => {
  if (selectedKeys.value.length === 0) {
    message.warning('请先选择要删除的组织');
    return;
  }
  try {
    await api.delete('/orgs/batch', {
      data: { ids: selectedKeys.value }
    });
    message.success('批量删除成功');
    selectedKeys.value = [];
    await fetchData();
    await fetchOrgs();
  } catch (e) {
    console.error('批量删除失败:', e);
    const errorMsg = e.response?.data?.message || e.response?.data?.data?.message || e.message || '批量删除失败';
    message.error(errorMsg);
  }
};

onMounted(() => {
  if (auth.role === 'ADMIN') {
    fetchOrgs();
    fetchData();
  }
});
</script>
