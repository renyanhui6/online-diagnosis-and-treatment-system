import { createRouter, createWebHistory } from 'vue-router';

// 医生端路由
const doctorRoutes = [
  {
    path: '/doctor/login',
    name: 'DoctorLogin',
    component: () => import('../views/common/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/doctor',
    component: () => import('../views/doctor/Layout.vue'),
    meta: { requiresAuth: true, role: 'doctor' },
    children: [
      {
        path: '',
        redirect: '/doctor/dashboard'
      },
      {
        path: 'dashboard',
        name: 'DoctorDashboard',
        component: () => import('../views/doctor/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'appointments',
        redirect: '/doctor/consultations'
      },
      {
        path: 'consultations',
        name: 'DoctorConsultations',
        component: () => import('../views/doctor/Consultations.vue'),
        meta: { title: '在线问诊' }
      },

      {
        path: 'chat/:id',
        name: 'DoctorChat',
        component: () => import('../views/doctor/Chat.vue'),
        meta: { title: '在线聊天' }
      },
      {
        path: 'medical-records',
        name: 'DoctorMedicalRecords',
        component: () => import('../views/doctor/MedicalRecords.vue'),
        meta: { title: '就诊记录管理' }
      },
      {
        path: 'profile',
        name: 'DoctorProfile',
        component: () => import('../views/doctor/Profile.vue'),
        meta: { title: '个人信息' }
      }
    ]
  }
];

// 管理员端路由
const adminRoutes = [
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/common/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/admin',
    component: () => import('../views/admin/Layout.vue'),
    meta: { requiresAuth: true, role: 'admin' },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('../views/admin/Dashboard.vue'),
        meta: { title: '管理首页' }
      },
      {
        path: 'doctors',
        name: 'AdminDoctors',
        component: () => import('../views/admin/Doctors.vue'),
        meta: { title: '医生管理' }
      },
      {
        path: 'departments',
        name: 'AdminDepartments',
        component: () => import('../views/admin/Departments.vue'),
        meta: { title: '部门管理' }
      },
      {
        path: 'medicines',
        name: 'AdminMedicines',
        component: () => import('../views/admin/Medicines.vue'),
        meta: { title: '药品管理' }
      },
      {
        path: 'patients',
        name: 'AdminPatients',
        component: () => import('../views/admin/Patients.vue'),
        meta: { title: '患者管理' }
      },
      {
        path: 'schedule-templates',
        name: 'AdminScheduleTemplates',
        component: () => import('../views/admin/ScheduleTemplates.vue'),
        meta: { title: '排班模板' }
      }
    ]
  }
];

// 通用路由
const commonRoutes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/common/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/common/NotFound.vue')
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes: [
    ...commonRoutes,
    ...doctorRoutes,
    ...adminRoutes
  ]
});

// 路由守卫 - 启用登录校验
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token');
  const userRole = localStorage.getItem('userRole');
  
  // 如果访问的是登录页，直接放行
  if (to.path === '/login' || to.path === '/doctor/login' || to.path === '/admin/login') {
    // 如果已登录，根据用户角色跳转到对应首页
    if (token) {
      if (userRole === 'doctor') {
        next('/doctor');
      } else if (userRole === 'admin') {
        next('/admin');
      } else {
        next('/');
      }
    } else {
      next();
    }
    return;
  }
  
  // 需要认证的路由
  if (to.meta.requiresAuth !== false) {
    if (!token) {
      // 未登录，跳转到对应的登录页面并保存重定向路径
      let loginPath = '/login';
      if (to.path.startsWith('/doctor')) {
        loginPath = '/doctor/login';
      } else if (to.path.startsWith('/admin')) {
        loginPath = '/admin/login';
      }
      
      next({
        path: loginPath,
        query: { redirect: to.fullPath }
      });
      return;
    }
    
    // 角色验证
    if (to.meta.role && to.meta.role !== userRole) {
      if (userRole === 'doctor') {
        next('/doctor');
      } else if (userRole === 'admin') {
        next('/admin');
      } else {
        next('/login');
      }
      return;
    }
  }
  
  next();
});

export default router;
