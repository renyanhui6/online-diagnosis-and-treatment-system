import { createRouter, createWebHistory } from 'vue-router'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 路由配置
const routes = [
  {
    path: '/hybridaction/:action',
    name: 'HybridAction',
    component: () => import('../views/home/index.vue'),
    meta: { title: '系统操作' }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/login/register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/forget',
    name: 'Forget',
    component: () => import('../views/login/forget.vue'),
    meta: { title: '找回密码' }
  },
  {
    path: '/',
    component: () => import('../layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('../views/home/index.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'appointment',
        name: 'Appointment',
        component: () => import('../views/appointment/index.vue'),
        meta: { title: '预约挂号' }
      },
      {
        path: 'appointment/list',
        name: 'AppointmentList',
        component: () => import('../views/appointment/list.vue'),
        meta: { title: '我的预约' }
      },
      {
        path: 'appointment/chat/:id',
        name: 'AppointmentChat',
        component: () => import('../views/appointment/chat.vue'),
        meta: { title: '在线问诊' }
      },
      {
        path: 'medicine',
        name: 'Medicine',
        component: () => import('../views/medicine/index.vue'),
        meta: { title: '在线购药' }
      },
      {
        path: 'record',
        name: 'Record',
        component: () => import('../views/record/index.vue'),
        meta: { title: '就诊记录' }
      },
      {
        path: 'payment/appointment',
        name: 'PaymentAppointment',
        component: () => import('../views/payment/appointment.vue'),
        meta: { title: '挂号支付记录' }
      },
      {
        path: 'payment/medicine',
        name: 'PaymentMedicine',
        component: () => import('../views/payment/medicine.vue'),
        meta: { title: '药品支付记录' }
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('../views/user/index.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'user/verification',
        name: 'UserVerification',
        component: () => import('../views/user/verification.vue'),
        meta: { title: '实名认证' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 配置NProgress进度条
NProgress.configure({ 
  easing: 'ease',
  speed: 500,
  showSpinner: false,
  trickleSpeed: 200,
  minimum: 0.3
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 启动进度条
  NProgress.start()
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 医院在线预约挂号系统` : '医院在线预约挂号系统'
  
  // 获取token
  const token = localStorage.getItem('token')
  
  // 如果访问的是登录页、注册页或找回密码页，直接放行
  if (to.path === '/login' || to.path === '/register' || to.path === '/forget') {
    // 如果已登录，跳转到首页
    if (token) {
      next('/')
    } else {
      next()
    }
  } else {
    // 如果访问其他页面，检查是否已登录
    if (token) {
      next()
    } else {
      // 未登录，强制跳转到登录页
      console.log('用户未登录，强制跳转到登录页')
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }
})

// 路由后置守卫
router.afterEach(() => {
  // 完成进度条
  NProgress.done()
})

export default router