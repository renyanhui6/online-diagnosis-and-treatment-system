import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // 用户信息
  const userInfo = ref({})
  // token
  const token = ref('')
  
  // 初始化用户信息
  function initUserInfo() {
    try {
      const storedUserInfo = localStorage.getItem('userInfo')
      const storedToken = localStorage.getItem('token')
      
      console.log('=== 用户信息初始化开始 ===')
      
      // 临时存储当前值，避免在初始化过程中丢失
      const currentUserInfo = { ...userInfo.value }
      const currentToken = token.value
      
      if (storedToken && storedToken !== 'undefined' && storedToken !== 'null' && storedToken.trim() !== '') {
        token.value = storedToken
        console.log('Token已恢复')
      } else {
        console.log('localStorage中没有有效的token')
        // 如果localStorage中没有token，但当前有token，保持当前token
        if (!currentToken) {
          token.value = ''
        }
      }
      
      if (storedUserInfo && storedUserInfo !== 'undefined' && storedUserInfo !== 'null' && storedUserInfo.trim() !== '') {
        try {
          const parsedUserInfo = JSON.parse(storedUserInfo)
          
          if (parsedUserInfo && typeof parsedUserInfo === 'object' && parsedUserInfo.id) {
            userInfo.value = parsedUserInfo
            console.log('用户信息已恢复，用户ID:', parsedUserInfo.id)
          } else {
            console.warn('解析的用户信息无效或缺少ID')
            localStorage.removeItem('userInfo')
            // 如果解析失败，但当前有用户信息，保持当前用户信息
            if (Object.keys(currentUserInfo).length > 0 && currentUserInfo.id) {
              userInfo.value = currentUserInfo
            } else {
              userInfo.value = {}
            }
          }
        } catch (parseError) {
          console.error('解析用户信息失败:', parseError)
          localStorage.removeItem('userInfo')
          // 如果解析失败，但当前有用户信息，保持当前用户信息
          if (Object.keys(currentUserInfo).length > 0 && currentUserInfo.id) {
            userInfo.value = currentUserInfo
          } else {
            userInfo.value = {}
          }
        }
      } else {
        console.log('localStorage中没有有效的用户信息')
        // 如果localStorage中没有用户信息，但当前有用户信息，保持当前用户信息
        if (Object.keys(currentUserInfo).length === 0 || !currentUserInfo.id) {
          userInfo.value = {}
        }
      }
      
      console.log('初始化完成 - 登录状态:', !!token.value)
      console.log('初始化完成 - 用户信息:', userInfo.value)
      console.log('=== 用户信息初始化结束 ===')
    } catch (error) {
      console.error('初始化用户信息失败:', error)
      clearUserInfo()
    }
  }
  
  // 设置用户信息
  function setUserInfo(info) {
    console.log('=== 设置用户信息开始 ===')
    console.log('传入的用户信息:', info)
    
    if (!info || typeof info !== 'object') {
      console.warn('用户信息为空或无效，将清除用户信息')
      userInfo.value = {}
      localStorage.removeItem('userInfo')
      return
    }
    
    if (!info.id) {
      console.warn('警告：用户信息中没有ID字段！')
      console.log('完整的用户信息:', JSON.stringify(info))
    }
    
    userInfo.value = { ...info }  // 创建副本避免引用问题
    localStorage.setItem('userInfo', JSON.stringify(info))
    console.log('用户信息已保存到localStorage，用户ID:', info?.id)
    console.log('=== 设置用户信息结束 ===')
  }
  
  // 设置token
  function setToken(newToken) {
    console.log('设置Token:', newToken)
    if (!newToken || typeof newToken !== 'string' || newToken.trim() === '') {
      console.warn('Token为空或无效，将清除token')
      token.value = ''
      localStorage.removeItem('token')
      return
    }
    
    token.value = newToken
    localStorage.setItem('token', newToken)
    console.log('Token已保存到localStorage')
  }
  
  // 清除用户信息
  function clearUserInfo() {
    console.log('清除用户信息')
    userInfo.value = {}
    token.value = ''
    localStorage.removeItem('userInfo')
    localStorage.removeItem('token')
  }
  
  // 清除token
  function clearToken() {
    token.value = ''
    localStorage.removeItem('token')
  }
  
  // 判断是否已登录
  const isLoggedIn = computed(() => {
    return !!token.value && token.value.trim() !== ''
  })
  
  // 获取用户ID
  const getUserId = computed(() => {
    const id = userInfo.value?.id || ''
    return id
  })
  
  // 获取用户名
  const getUserName = computed(() => userInfo.value?.name || '')
  
  // 自动初始化
  initUserInfo()
  
  return {
    userInfo,
    token,
    initUserInfo,
    setUserInfo,
    setToken,
    clearUserInfo,
    clearToken,
    isLoggedIn,
    getUserId,
    getUserName
  }
})