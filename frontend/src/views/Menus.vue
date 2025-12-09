<template>
  <a-card title="菜单管理" :loading="loading" :extra="renderActions()">
    <a-table :dataSource="items" rowKey="id" :pagination="false">
      <a-table-column title="ID" dataIndex="id" />
      <a-table-column title="名称" dataIndex="name" />
      <a-table-column title="路径" dataIndex="path" />
      <a-table-column title="角色" dataIndex="role" />
      <a-table-column title="顺序" dataIndex="orderIndex" />
      <a-table-column title="操作" :customRender="(_, record) => renderRow(record)" />
    </a-table>
  </a-card>

  <a-modal v-model:open="openEdit" :title="form.id ? '编辑菜单' : '新建菜单'" @ok="save" :confirmLoading="saving">
    <a-form layout="vertical">
      <a-form-item label="名称">
        <a-input v-model:value="form.name" />
      </a-form-item>
      <a-form-item label="路径">
        <a-input v-model:value="form.path" />
      </a-form-item>
      <a-form-item label="角色">
        <a-select v-model:value="form.role" :options="roleOptions" />
      </a-form-item>
      <a-form-item label="顺序">
        <a-input-number v-model:value="form.orderIndex" style="width:100%;" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { h, onMounted, ref } from 'vue';
import api from '../api';
import { Button, message, Popconfirm, Space, Select } from 'ant-design-vue';

const loading = ref(false);
const saving = ref(false);
const items = ref([]);
const openEdit = ref(false);
const form = ref({});
const currentRole = ref('ADMIN');

const roleOptions = [
  { label: 'ADMIN', value: 'ADMIN' },
  { label: 'TEACHER', value: 'TEACHER' },
  { label: 'STUDENT', value: 'STUDENT' }
];

const fetchData = async () => {
  loading.value = true;
  try {
    const { data } = await api.get('/menus', { params: { role: currentRole.value } });
    items.value = data.data || data;
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败');
  } finally {
    loading.value = false;
  }
};

const reorder = async () => {
  const ids = items.value.map(m => m.id);
  try {
    await api.post('/menus/reorder', { ids });
    message.success('排序已保存');
    fetchData();
  } catch (e) {
    message.error(e.response?.data?.message || '排序失败');
  }
};

const renderActions = () =>
  h(Space, {}, () => [
    h(Button, { size: 'small', onClick: () => { form.value = { role: currentRole.value }; openEdit.value = true; } }, () => '新建'),
    h(Button, { size: 'small', onClick: reorder }, () => '保存排序'),
    h(Button, { size: 'small', onClick: fetchData }, () => '刷新'),
    h(Select, {
      value: currentRole.value,
      onChange: (v) => { currentRole.value = v; fetchData(); },
      style: { width: '120px' },
      options: roleOptions
    })
  ]);

const renderRow = (record) =>
  h(Space, {}, () => [
    h(Button, { size: 'small', onClick: () => { form.value = { ...record }; openEdit.value = true; } }, () => '编辑'),
    h(Popconfirm, { title: '确认删除？', onConfirm: () => remove(record.id) }, {
      default: () => h(Button, { size: 'small', danger: true }, () => '删除')
    })
  ]);

const save = async () => {
  if (!form.value.name || !form.value.path || !form.value.role) {
    message.warning('请填写名称、路径和角色');
    return;
  }
  saving.value = true;
  try {
    if (form.value.id) {
      await api.put(`/menus/${form.value.id}`, form.value);
    } else {
      await api.post('/menus', form.value);
    }
    message.success('已保存');
    openEdit.value = false;
    fetchData();
  } catch (e) {
    message.error(e.response?.data?.message || '保存失败');
  } finally {
    saving.value = false;
  }
};

const remove = async (id) => {
  try {
    await api.delete(`/menus/${id}`);
    message.success('已删除');
    fetchData();
  } catch (e) {
    message.error(e.response?.data?.message || '删除失败');
  }
};

onMounted(fetchData);
</script>
