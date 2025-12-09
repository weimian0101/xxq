<template>
  <a-card title="个人信息" :loading="loading">
    <a-form 
      ref="formRef"
      :model="form" 
      :rules="rules" 
      layout="vertical"
      style="max-width: 600px"
    >
      <a-form-item label="用户名" name="username">
        <a-input v-model:value="form.username" disabled />
      </a-form-item>
      <a-form-item label="姓名" name="fullName">
        <a-input v-model:value="form.fullName" placeholder="请输入姓名" />
      </a-form-item>
      <a-form-item label="手机号" name="phone">
        <a-input v-model:value="form.phone" placeholder="请输入手机号" />
      </a-form-item>
      <a-form-item label="角色">
        <a-input :value="form.role" disabled />
      </a-form-item>
      <a-form-item label="签名图片URL" name="signatureUrl">
        <a-input v-model:value="form.signatureUrl" placeholder="请输入签名图片URL" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="handleSave" :loading="saving">保存</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>
  </a-card>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../api';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const loading = ref(false);
const saving = ref(false);
const form = ref({
  username: '',
  fullName: '',
  phone: '',
  role: '',
  signatureUrl: ''
});
const formRef = ref(null);
const originalForm = ref({});

const rules = {
  fullName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
};

const fetchProfile = async () => {
  loading.value = true;
  try {
    const response = await api.get('/users/profile/me');
    const userData = response.data?.data || response.data;
    form.value = {
      username: userData.username || '',
      fullName: userData.fullName || '',
      phone: userData.phone || '',
      role: userData.role || '',
      signatureUrl: userData.signatureUrl || ''
    };
    originalForm.value = { ...form.value };
  } catch (e) {
    console.error('加载个人信息失败:', e);
    message.error('加载个人信息失败');
  } finally {
    loading.value = false;
  }
};

const handleSave = async () => {
  try {
    await formRef.value.validate();
    saving.value = true;
    await api.put('/users/profile/me', {
      fullName: form.value.fullName,
      phone: form.value.phone,
      signatureUrl: form.value.signatureUrl
    });
    message.success('保存成功');
    originalForm.value = { ...form.value };
    // 更新auth store中的用户信息
    auth.setAuth(auth.token, { ...auth.user, ...form.value });
    await fetchProfile();
  } catch (e) {
    console.error('保存失败:', e);
    if (e.errorFields) {
      return;
    }
    const errorMsg = e.response?.data?.message || e.message || '保存失败';
    message.error(errorMsg);
  } finally {
    saving.value = false;
  }
};

const handleReset = () => {
  form.value = { ...originalForm.value };
  formRef.value?.clearValidate();
};

onMounted(fetchProfile);
</script>

