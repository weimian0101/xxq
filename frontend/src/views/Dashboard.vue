<template>
  <div>
    <!-- 学生端Dashboard -->
    <template v-if="auth.role === 'STUDENT'">
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="6">
          <a-card>
            <a-statistic title="我的选题" :value="studentStats.mySelection ? '已选题' : '未选题'" />
            <a-button v-if="studentStats.mySelection" type="link" @click="$router.push('/topics')">
              查看详情
            </a-button>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="阶段任务" :value="studentStats.taskCount" suffix="个" />
            <a-button type="link" @click="$router.push('/stage')">查看任务</a-button>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="待审批申请" :value="studentStats.pendingApplications" suffix="个" />
            <a-button type="link" @click="$router.push('/applications')">查看申请</a-button>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="未读公告" :value="studentStats.unreadAnnouncements" suffix="条" />
            <a-button type="link" @click="$router.push('/ann')">查看公告</a-button>
          </a-card>
        </a-col>
      </a-row>
      <a-card title="我的选题信息" v-if="studentStats.mySelection">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="课题ID">{{ studentStats.mySelection.topicId }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <a-tag :color="getSelectionStatusColor(studentStats.mySelection.status)">
              {{ getSelectionStatusText(studentStats.mySelection.status) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="选择时间">
            {{ studentStats.mySelection.createdAt ? new Date(studentStats.mySelection.createdAt).toLocaleString() : '-' }}
          </a-descriptions-item>
        </a-descriptions>
      </a-card>
      <a-card title="答辩信息" v-if="studentStats.myGroup">
        <a-descriptions :column="2" bordered>
          <a-descriptions-item label="分组ID">{{ studentStats.myGroup.groupId }}</a-descriptions-item>
          <a-descriptions-item label="课题ID">{{ studentStats.myGroup.topicId }}</a-descriptions-item>
        </a-descriptions>
        <a-divider />
        <h4>成绩信息</h4>
        <a-table 
          v-if="studentStats.myScores && studentStats.myScores.length > 0"
          :dataSource="studentStats.myScores" 
          :pagination="false"
          rowKey="id"
        >
          <a-table-column title="成绩" dataIndex="score" />
          <a-table-column title="备注" dataIndex="comment" />
        </a-table>
        <a-empty v-else description="暂无成绩" />
      </a-card>
    </template>

    <!-- 教师端Dashboard -->
    <template v-else-if="auth.role === 'TEACHER'">
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="6">
          <a-card>
            <a-statistic title="我的课题" :value="teacherStats.myTopics" suffix="个" />
            <a-button type="link" @click="$router.push('/topics')">管理课题</a-button>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="待审核任务" :value="teacherStats.pendingTasks" suffix="个" />
            <a-button type="link" @click="$router.push('/stage')">审核任务</a-button>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="待审批申请" :value="teacherStats.pendingApplications" suffix="个" />
            <a-button type="link" @click="$router.push('/applications')">审批申请</a-button>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="评阅任务" :value="teacherStats.reviewTasks" suffix="个" />
            <a-button type="link" @click="$router.push('/defense')">查看评阅</a-button>
          </a-card>
        </a-col>
      </a-row>
    </template>

    <!-- 管理员端Dashboard -->
    <template v-else>
      <a-row :gutter="16" style="margin-bottom: 16px">
        <a-col :span="6">
          <a-card>
            <a-statistic title="课题总数" :value="adminStats.topics" suffix="个" />
            <a-button type="link" @click="$router.push('/topics')">管理课题</a-button>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="用户总数" :value="adminStats.users" suffix="人" />
            <a-button type="link" @click="$router.push('/users')">管理用户</a-button>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="待审批申请" :value="adminStats.pendingApplications" suffix="个" />
            <a-button type="link" @click="$router.push('/applications')">审批申请</a-button>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card>
            <a-statistic title="答辩分组" :value="adminStats.defenseGroups" suffix="个" />
            <a-button type="link" @click="$router.push('/defense')">管理分组</a-button>
          </a-card>
        </a-col>
      </a-row>
    </template>

    <a-card title="快速入口">
      <a-space>
        <a-button v-if="auth.role === 'STUDENT'" type="primary" @click="$router.push('/topics')">课题选择</a-button>
        <a-button v-if="auth.role === 'STUDENT'" @click="$router.push('/stage')">阶段任务</a-button>
        <a-button v-if="auth.role === 'STUDENT'" @click="$router.push('/applications')">提交申请</a-button>
        <a-button v-if="auth.role === 'TEACHER'" type="primary" @click="$router.push('/topics')">课题管理</a-button>
        <a-button v-if="auth.role === 'TEACHER'" @click="$router.push('/stage')">任务审核</a-button>
        <a-button v-if="auth.role === 'TEACHER'" @click="$router.push('/defense')">答辩任务</a-button>
        <a-button v-if="auth.role === 'ADMIN'" type="primary" @click="$router.push('/topics')">课题管理</a-button>
        <a-button v-if="auth.role === 'ADMIN'" @click="$router.push('/users')">用户管理</a-button>
        <a-button v-if="auth.role === 'ADMIN'" @click="$router.push('/orgs')">组织管理</a-button>
      </a-space>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import api from '../api';
import { useAuthStore } from '../store/auth';
import { message } from 'ant-design-vue';

const auth = useAuthStore();
const studentStats = ref({ 
  mySelection: null, 
  taskCount: 0, 
  pendingApplications: 0, 
  unreadAnnouncements: 0,
  myGroup: null,
  myScores: []
});
const teacherStats = ref({ 
  myTopics: 0, 
  pendingTasks: 0, 
  pendingApplications: 0, 
  reviewTasks: 0 
});
const adminStats = ref({ 
  topics: 0, 
  users: 0, 
  pendingApplications: 0, 
  defenseGroups: 0 
});

const getSelectionStatusColor = (status) => {
  const colors = {
    'SELECTED': 'blue',
    'LOCKED': 'green',
    'CANCELLED': 'red'
  };
  return colors[status] || 'default';
};

const getSelectionStatusText = (status) => {
  const texts = {
    'SELECTED': '已选择',
    'LOCKED': '已锁定',
    'CANCELLED': '已取消'
  };
  return texts[status] || status;
};

const loadStudentStats = async () => {
  try {
    const [selectionRes, tasksRes, applicationsRes, announcementsRes, groupRes, scoresRes] = await Promise.all([
      api.get('/topics/my-selection').catch(() => ({ data: { data: null } })),
      api.get('/stages/tasks', { params: { studentId: auth.user.id } }).catch(() => ({ data: [] })),
      api.get('/applications', { params: { status: 'SUBMITTED' } }).catch(() => ({ data: { data: [] } })),
      api.get('/announcements/page', { params: { page: 0, size: 10, status: 'PUBLISHED' } }).catch(() => ({ data: { data: { content: [] } } })),
      api.get('/defense/my-group').catch(() => ({ data: { data: null } })),
      api.get('/defense/my-scores').catch(() => ({ data: { data: [] } }))
    ]);
    
    studentStats.value.mySelection = selectionRes.data?.data || null;
    studentStats.value.taskCount = tasksRes.data?.length || 0;
    studentStats.value.pendingApplications = applicationsRes.data?.data?.length || 0;
    studentStats.value.unreadAnnouncements = announcementsRes.data?.data?.content?.length || 0;
    studentStats.value.myGroup = groupRes.data?.data || null;
    studentStats.value.myScores = scoresRes.data?.data || [];
  } catch (e) {
    console.error('加载学生统计失败:', e);
  }
};

const loadTeacherStats = async () => {
  try {
    const [topicsRes, tasksRes, applicationsRes, reviewsRes] = await Promise.all([
      api.get('/topics/page', { params: { page: 0, size: 100, creatorId: auth.user.id } }).catch(() => ({ data: { data: { content: [] } } })),
      api.get('/stages/tasks').catch(() => ({ data: [] })),
      api.get('/applications/page', { params: { page: 0, size: 100, status: 'SUBMITTED' } }).catch(() => ({ data: { data: { content: [] } } })),
      api.get('/defense/reviews', { params: { reviewerId: auth.user.id, status: 'PENDING' } }).catch(() => ({ data: { data: [] } }))
    ]);
    
    teacherStats.value.myTopics = topicsRes.data?.data?.content?.length || 0;
    const allTasks = tasksRes.data || [];
    teacherStats.value.pendingTasks = allTasks.filter(t => t.status === 'SUBMITTED').length;
    teacherStats.value.pendingApplications = applicationsRes.data?.data?.content?.length || 0;
    teacherStats.value.reviewTasks = reviewsRes.data?.data?.length || 0;
  } catch (e) {
    console.error('加载教师统计失败:', e);
  }
};

const loadAdminStats = async () => {
  try {
    const [topicsRes, usersRes, applicationsRes, groupsRes] = await Promise.all([
      api.get('/topics/page', { params: { page: 0, size: 1 } }).catch(() => ({ data: { data: { totalElements: 0 } } })),
      api.get('/users/page', { params: { page: 0, size: 1 } }).catch(() => ({ data: { data: { totalElements: 0 } } })),
      api.get('/applications/page', { params: { page: 0, size: 1, status: 'SUBMITTED' } }).catch(() => ({ data: { data: { totalElements: 0 } } })),
      api.get('/defense/groups').catch(() => ({ data: { data: [] } }))
    ]);
    
    adminStats.value.topics = topicsRes.data?.data?.totalElements || 0;
    adminStats.value.users = usersRes.data?.data?.totalElements || 0;
    adminStats.value.pendingApplications = applicationsRes.data?.data?.totalElements || 0;
    adminStats.value.defenseGroups = groupsRes.data?.data?.length || 0;
  } catch (e) {
    console.error('加载管理员统计失败:', e);
  }
};

const loadStats = async () => {
  if (auth.role === 'STUDENT') {
    await loadStudentStats();
  } else if (auth.role === 'TEACHER') {
    await loadTeacherStats();
  } else if (auth.role === 'ADMIN') {
    await loadAdminStats();
  }
};

onMounted(loadStats);
</script>

