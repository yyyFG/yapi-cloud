import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import InterfaceMarketPage from '@/pages/interface/InterfaceMarketPage.vue'
import InterfaceDetailPage from '@/pages/interface/InterfaceDetailPage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import InterfaceManagePage from '@/pages/admin/InterfaceManagePage.vue'
import UserProfilePage from '@/pages/user/UserProfilePage.vue'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    {
      path: '/interfaces',
      name: '接口市场',
      component: InterfaceMarketPage,
    },
    {
      path: '/interface/:id',
      name: '接口详情',
      component: InterfaceDetailPage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/user/profile',
      name: '个人中心',
      component: UserProfilePage,
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
    },
    {
      path: '/admin/interfaceManage',
      name: '接口管理',
      component: InterfaceManagePage,
    },
  ],
})

export default router
