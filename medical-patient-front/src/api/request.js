import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 创建axios实例
const service = axios.create({
  baseURL: 'http://localhost:8080/treat', // API基础URL
  timeout: 30000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    if (token) {
      // 设置access-key请求头
      config.headers['access-key'] = token
      console.log('请求拦截器设置token:', token, '请求URL:', config.url)
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 检查是否有新的token，有则更新
    const newToken = response.headers['new-access-key'] || response.headers['new-access-token']
    if (newToken) {
      localStorage.setItem('token', newToken)
      console.log('更新token:', newToken)
    }
    
    // 打印响应信息，用于调试
    console.log('API响应:', response.config.url, response.data)
    
    const res = response.data
    
    // 如果返回的状态码不是200，说明接口请求有误
    if (res.code !== 200) {
      console.error('API错误:', response.config.url, res.code, res.message)
      // 只在非登录相关错误时显示错误消息
      if (res.code !== 601 && res.code !== 602 && res.code !== 501 && res.code !== 201 && res.code !== 9001) {
        ElMessage({
          message: res.message || '请求失败',
          type: 'error',
          duration: 5 * 1000
        })
      }
      
      // 对201错误码进行特殊处理
      if (res.code === 201) {
        ElMessage({
          message: '操作失败，请稍后重试',
          type: 'warning',
          duration: 5 * 1000
        })
      }
      
      // 未登录或token过期
      if (res.code == 601 || res.code == 602 || res.code == 501) {
        console.log('检测到未登录状态，错误码:', res.code)
        // 清除本地存储的token
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        
        // 跳转到登录页，并保存当前路径用于登录后重定向
        const currentPath = window.location.pathname + window.location.search
        router.replace({ path: '/login', query: { redirect: currentPath } })
      }
      
      const requestError = new Error(res.message || '请求失败')
      requestError.code = res.code
      requestError.data = res
      return Promise.reject(requestError)
    } else {
      return res
    }
  },
  error => {
    console.error('响应错误:', error)
    // 只在开发环境显示网络错误
    if (process.env.NODE_ENV === 'development') {
      ElMessage({
        message: error.message || '网络请求失败',
        type: 'error',
        duration: 5 * 1000
      })
    }
    return Promise.reject(error)
  }
)

export default service
