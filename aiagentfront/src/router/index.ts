import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

// 为路由meta定义类型
interface RouteMeta {
  title: string
  description?: string
}

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: {
      title: '首页 - 审计智能体应用平台',
      description: 'AI超级智能体服务，满足您的各种AI对话需求'
    }
  },
  {
    path: '/audit-master',
    name: 'AuditMaster',
    component: () => import('../views/AuditMaster.vue'),
    meta: {
      title: 'AI审计大师 - 智能体应用平台',
      description: 'AI审计大师是专业审计顾问，帮你解答各种审计问题，提供建议'
    }
  },
  {
    path: '/super-agent',
    name: 'SuperAgent',
    component: () => import('../views/SuperAgent.vue'),
    meta: {
      title: 'AI超级智能体',
      description: '用于生成审计底稿'
    },
  },
  {
    path:'/audit-generate',
    name: 'AuditGenerate',
    component: () => import('../views/AuditGenerate.vue'),
    meta: {
      title: 'AI审计生成器',
      description: '用于生成审计底稿'
    }
  },
  {    path:'/table-sql',    name: 'TableSqlQuery',    component: () => import('../views/TableSqlQuery.vue'),    meta: {      title: '数据分析',      description: '上传表格文件并使用SQL进行查询'    }  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局导航守卫，设置文档标题
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title && typeof to.meta.title === 'string') {
    document.title = to.meta.title
  }
  next()
})

export default router
