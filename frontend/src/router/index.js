import { createRouter, createWebHistory } from 'vue-router';
import Dashboard from '../views/Dashboard.vue';
import Topics from '../views/Topics.vue';
import Stage from '../views/Stage.vue';
import Announcements from '../views/Announcements.vue';
import Applications from '../views/Applications.vue';
import Defense from '../views/Defense.vue';
import Users from '../views/Users.vue';
import Orgs from '../views/Orgs.vue';
import Menus from '../views/Menus.vue';
import StageAdmin from '../views/StageAdmin.vue';
import Profile from '../views/Profile.vue';
import MyStudents from '../views/MyStudents.vue';
import Login from '../views/Login.vue';
import { useAuthStore } from '../store/auth';

const routes = [
  { path: '/login', name: 'login', component: Login },
  { path: '/', redirect: { name: 'dashboard' } },
  { path: '/dashboard', name: 'dashboard', component: Dashboard, meta: { auth: true } },
  { path: '/topics', name: 'topics', component: Topics, meta: { auth: true } },
  { path: '/stage', name: 'stage', component: Stage, meta: { auth: true } },
  { path: '/announcements', name: 'announcements', component: Announcements, meta: { auth: true } },
  { path: '/applications', name: 'applications', component: Applications, meta: { auth: true } },
  { path: '/defense', name: 'defense', component: Defense, meta: { auth: true } },
  { path: '/users', name: 'users', component: Users, meta: { auth: true } },
  { path: '/orgs', name: 'orgs', component: Orgs, meta: { auth: true } },
  { path: '/menus', name: 'menus', component: Menus, meta: { auth: true } },
  { path: '/stage-admin', name: 'stage-admin', component: StageAdmin, meta: { auth: true } },
  { path: '/profile', name: 'profile', component: Profile, meta: { auth: true } },
  { path: '/my-students', name: 'my-students', component: MyStudents, meta: { auth: true } }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const auth = useAuthStore();
  if (to.meta.auth && !auth.isAuthed) {
    next({ name: 'login', query: { redirect: to.fullPath } });
  } else {
    next();
  }
});

export default router;

