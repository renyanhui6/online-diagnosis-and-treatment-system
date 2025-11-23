import { createPinia } from 'pinia'
import { useUserStore } from './user'

/**
 * 初始化pinia
 */
export function setupStore() {
  // 初始化用户状态
  const userStore = useUserStore()
  userStore.initUserInfo()
}

export default createPinia()