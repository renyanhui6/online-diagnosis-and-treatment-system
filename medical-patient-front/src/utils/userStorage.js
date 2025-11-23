/**
 * 用户信息本地存储工具类
 * 替代Pinia store，直接使用localStorage管理用户状态
 */

// 存储键名
const USER_INFO_KEY = 'userInfo'
const TOKEN_KEY = 'token'

/**
 * 用户存储工具类
 */
export class UserStorage {
  
  /**
   * 获取用户信息
   * @returns {Object} 用户信息对象
   */
  static getUserInfo() {
    try {
      const userInfoStr = localStorage.getItem(USER_INFO_KEY)
      if (!userInfoStr || userInfoStr === 'undefined' || userInfoStr === 'null') {
        return {}
      }
      return JSON.parse(userInfoStr)
    } catch (error) {
      console.error('获取用户信息失败:', error)
      localStorage.removeItem(USER_INFO_KEY)
      return {}
    }
  }

  /**
   * 设置用户信息
   * @param {Object} userInfo 用户信息对象
   */
  static setUserInfo(userInfo) {
    try {
      if (!userInfo || typeof userInfo !== 'object') {
        console.warn('用户信息为空或无效')
        localStorage.removeItem(USER_INFO_KEY)
        return
      }
      
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo))
      console.log('用户信息已保存:', userInfo)
    } catch (error) {
      console.error('保存用户信息失败:', error)
    }
  }

  /**
   * 获取Token
   * @returns {string} token字符串
   */
  static getToken() {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token || token === 'undefined' || token === 'null') {
      return ''
    }
    return token
  }

  /**
   * 设置Token
   * @param {string} token token字符串
   */
  static setToken(token) {
    if (!token || typeof token !== 'string' || token.trim() === '') {
      console.warn('Token为空或无效')
      localStorage.removeItem(TOKEN_KEY)
      return
    }
    
    localStorage.setItem(TOKEN_KEY, token)
    console.log('Token已保存')
  }

  /**
   * 清除所有用户数据
   */
  static clearUserData() {
    localStorage.removeItem(USER_INFO_KEY)
    localStorage.removeItem(TOKEN_KEY)
    console.log('用户数据已清除')
  }

  /**
   * 清除Token
   */
  static clearToken() {
    localStorage.removeItem(TOKEN_KEY)
    console.log('Token已清除')
  }

  /**
   * 判断是否已登录
   * @returns {boolean} 是否已登录
   */
  static isLoggedIn() {
    const token = this.getToken()
    return !!token && token.trim() !== ''
  }

  /**
   * 获取用户ID
   * @returns {string} 用户ID
   */
  static getUserId() {
    const userInfo = this.getUserInfo()
    if (!userInfo || Object.keys(userInfo).length === 0) {
      return ''
    }
    
    // 检查多种可能的ID字段名
    const possibleIdFields = ['id', 'userId', 'user_id', 'patientId', 'patient_id']
    for (const field of possibleIdFields) {
      const value = userInfo[field]
      if (value && (typeof value === 'string' || typeof value === 'number') && value.toString().trim() !== '') {
        console.log(`找到用户ID字段 ${field}:`, value)
        return value.toString()
      }
    }
    
    console.log('未找到有效的用户ID字段')
    return ''
  }

  /**
   * 获取用户名
   * @returns {string} 用户名
   */
  static getUserName() {
    const userInfo = this.getUserInfo()
    return userInfo?.name || userInfo?.username || ''
  }

  /**
   * 更新用户信息的某个字段
   * @param {string} key 字段名
   * @param {any} value 字段值
   */
  static updateUserField(key, value) {
    const userInfo = this.getUserInfo()
    if (Object.keys(userInfo).length > 0) {
      userInfo[key] = value
      this.setUserInfo(userInfo)
    }
  }

  /**
   * 检查用户信息是否有效
   * @returns {boolean} 用户信息是否有效
   */
  static isUserInfoValid() {
    const userInfo = this.getUserInfo()
    console.log('检查用户信息有效性:', userInfo)
    
    if (!userInfo || Object.keys(userInfo).length === 0) {
      console.log('用户信息为空或不存在')
      return false
    }
    
    // 检查多种可能的ID字段名
    const possibleIdFields = ['id', 'userId', 'user_id', 'patientId', 'patient_id']
    const hasValidId = possibleIdFields.some(field => {
      const value = userInfo[field]
      const isValid = value && (typeof value === 'string' || typeof value === 'number') && value.toString().trim() !== ''
      console.log(`检查字段 ${field}:`, value, '有效:', isValid)
      return isValid
    })
    
    if (!hasValidId) {
      console.log('用户信息中没有有效的ID字段')
      console.log('可用的字段:', Object.keys(userInfo))
      return false
    }
    
    console.log('用户信息验证通过')
    return true
  }
}

// 导出默认实例（兼容旧的使用方式）
export default UserStorage