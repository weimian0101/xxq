<template>
  <a-layout style="min-height: 100vh">
    <a-layout-header style="color: #fff; font-weight: 600; display: flex; justify-content: space-between; align-items: center;">
      <span>毕业设计过程管理及质量控制平台</span>
      <div v-if="auth.isAuthed" style="display:flex;gap:12px;align-items:center;">
        <a-button type="link" size="small" @click="router.push({ name: 'profile' })" style="color: #fff">
          {{ auth.user.fullName || auth.user.username }} ({{ auth.user.role }})
        </a-button>
        <a-button size="small" @click="logout">退出</a-button>
      </div>
    </a-layout-header>
    <a-layout>
      <a-layout-sider width="200" v-if="auth.isAuthed">
        <a-menu mode="inline" :selectedKeys="[activeKey]" @click="onMenu">
          <a-menu-item v-for="item in menuItems" :key="item.key">
            {{ item.name }}
          </a-menu-item>
        </a-menu>
      </a-layout-sider>
      <a-layout-content style="padding: 16px">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { computed, onMounted, ref, watch } from 'vue';
import { useAuthStore } from './store/auth';
import api from './api';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const activeKey = computed(() => route.name);
const menuItems = ref([
  { key: 'dashboard', name: '仪表盘' },
  { key: 'topics', name: '课题管理' },
  { key: 'stage', name: '阶段任务' },
  { key: 'ann', name: '公告' },
  { key: 'applications', name: '申请/审批' },
  { key: 'defense', name: '答辩' },
  { key: 'users', name: '用户管理' },
  { key: 'orgs', name: '组织管理' },
  { key: 'menus', name: '菜单管理' },
  { key: 'stage-admin', name: '阶段配置' },
]);

const onMenu = ({ key }) => router.push({ name: key });
const logout = () => {
  auth.clear();
  router.replace({ name: 'login' });
};

const loadMenus = async () => {
  if (!auth.role) return;
  try {
    const { data } = await api.get('/menus', { params: { role: auth.role } });
    menuItems.value = data.data?.map(m => ({
      key: (m.path || '').replace('/', '') || 'dashboard',
      name: m.name
    })) || menuItems.value;
  } catch {
    // fallback 静态菜单
  }
};

onMounted(loadMenus);
watch(() => auth.role, loadMenus);
</script>

