<template>
  <div style="max-width: 360px; margin: 80px auto;">
    <a-card title="登录">
      <a-form :model="form" layout="vertical" @finish="onSubmit">
        <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="form.username" />
        </a-form-item>
        <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" block html-type="submit" :loading="loading">登录</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import api from '../api';
import { useAuthStore } from '../store/auth';
import { message } from 'ant-design-vue';

const form = reactive({ username: '', password: '' });
const loading = ref(false);
const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const onSubmit = async () => {
  loading.value = true;
  try {
    const { data } = await api.post('/auth/login', form);
    auth.setAuth(data.token, { id: data.id, username: data.username, role: data.role, fullName: data.fullName });
    message.success('登录成功');
    const redirect = route.query.redirect || '/';
    router.replace(redirect);
  } catch (e) {
    message.error(e.response?.data?.message || '登录失败');
  } finally {
    loading.value = false;
  }
};
</script>

