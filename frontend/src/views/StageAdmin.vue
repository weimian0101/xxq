<template>
  <a-card title="阶段配置" :loading="loading" :extra="renderActions()">
    <a-table :dataSource="items" rowKey="id" :pagination="false">
      <a-table-column title="ID" dataIndex="id" />
      <a-table-column title="名称" dataIndex="name" />
      <a-table-column title="顺序" dataIndex="orderIndex" />
      <a-table-column title="启用" :customRender="({ record }) => record.active ? '是' : '否'" />
      <a-table-column title="开始" dataIndex="startAt" />
      <a-table-column title="结束" dataIndex="endAt" />
      <a-table-column title="操作" :customRender="(_, record) => renderRow(record)" />
    </a-table>
  </a-card>

  <a-modal v-model:open="openEdit" :title="form.id ? '编辑阶段' : '新建阶段'" @ok="save" :confirmLoading="saving">
    <a-form layout="vertical">
      <a-form-item label="名称">
        <a-input v-model:value="form.name" />
      </a-form-item>
      <a-form-item label="顺序">
        <a-input-number v-model:value="form.orderIndex" style="width:100%;" />
      </a-form-item>
      <a-form-item label="启用">
        <a-switch v-model:checked="form.active" />
      </a-form-item>
      <a-form-item label="开始时间 (yyyy-MM-dd HH:mm:ss)">
        <a-input v-model:value="form.startAt" />
      </a-form-item>
      <a-form-item label="结束时间 (yyyy-MM-dd HH:mm:ss)">
        <a-input v-model:value="form.endAt" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { h, onMounted, ref } from 'vue';
import api from '../api';
import { Button, message, Popconfirm, Space } from 'ant-design-vue';

const loading = ref(false);
const saving = ref(false);
const items = ref([]);
const openEdit = ref(false);
const form = ref({});

const fetchData = async () => {
  loading.value = true;
  try {
    const { data } = await api.get('/stages/all');
    items.value = data;
  } catch (e) {
    message.error(e.response?.data?.message || '加载失败');
  } finally {
    loading.value = false;
  }
};

const renderActions = () =>
  h(Space, {}, () => [
    h(Button, { type: 'primary', size: 'small', onClick: () => { form.value = { active: true }; openEdit.value = true; } }, () => '新建'),
    h(Button, { size: 'small', onClick: fetchData }, () => '刷新')
  ]);

const renderRow = (record) =>
  h(Space, {}, () => [
    h(Button, { size: 'small', onClick: () => { form.value = { ...record }; openEdit.value = true; } }, () => '编辑'),
    h(Popconfirm, { title: '确认删除？', onConfirm: () => remove(record.id) }, {
      default: () => h(Button, { size: 'small', danger: true }, () => '删除')
    })
  ]);

const save = async () => {
  if (!form.value.name) {
    message.warning('请输入名称');
    return;
  }
  saving.value = true;
  try {
    if (form.value.id) {
      await api.put(`/stages/${form.value.id}`, form.value);
    } else {
      await api.post('/stages', form.value);
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
    await api.delete(`/stages/${id}`);
    message.success('已删除');
    fetchData();
  } catch (e) {
    message.error(e.response?.data?.message || '删除失败');
  }
};

onMounted(fetchData);
</script>


