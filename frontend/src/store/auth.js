import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '');
  const user = ref(JSON.parse(localStorage.getItem('user') || '{}'));

  const isAuthed = computed(() => !!token.value);
  const role = computed(() => user.value.role);

  function setAuth(t, u) {
    token.value = t;
    user.value = u;
    localStorage.setItem('token', t);
    localStorage.setItem('user', JSON.stringify(u));
  }

  function clear() {
    token.value = '';
    user.value = {};
    localStorage.removeItem('token');
    localStorage.removeItem('user');
  }

  return { token, user, role, isAuthed, setAuth, clear };
});

