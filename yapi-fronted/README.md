# yapi-frontend（接口开放平台前端）

## 项目简介

本项目是「yApiRelay · 接口开放平台」的前端工程，提供用户注册登录、接口市场浏览、接口在线调用测试、管理后台与调用排行榜等页面。前端通过 axios 与后端微服务网关（默认 `http://localhost:18098`）交互，所有后端请求代码由 `@umijs/openapi` 从后端 OpenAPI 文档自动生成，页面只需 import 生成函数即可调用接口。

## 功能特性

- 首页落地页（`/`）：hero 展示区、特性卡片、接口调用排行榜（数据来自 `listInterfaceRank`）
- 接口市场（`/interfaces`）：接口搜索与分页列表，卡片展示接口名、描述、请求方法、调用次数
- 接口详情（`/interface/:id`）：接口信息展示、在线调用测试（填参数点击调用并显示返回结果）、申请开通
- 用户登录（`/user/login`）、用户注册（`/user/register`）
- 个人中心（`/user/profile`）：我的信息维护、我创建的接口、我申请的接口
- 管理后台（`/admin/interfaceManage`、`/admin/userManage`）：接口管理（发布/下线/删除）与用户管理
- 全局权限控制（`src/access/`）：路由级权限检查、登录态管理与 40100 未登录自动跳转
- 公共布局与组件：`BasicLayout` 页面骨架、`GlobalHeader` 导航栏、`GlobalFooter` 页脚

## 技术栈

| 技术 | 版本 | 用途 |
| ---- | ---- | ---- |
| Vue | 3.5 | 前端框架（Composition API，`<script setup lang="ts">`） |
| TypeScript | 5.8 | 类型系统，`vue-tsc` 做构建前类型检查 |
| Vite | 7 | 构建工具与开发服务器 |
| Ant Design Vue | 4.x | 主 UI 组件库（`main.ts` 中全局注册，使用 `a-xxx` 组件） |
| Arco Design Web Vue | 2.x | UI 组件库（依赖中存在，按需使用） |
| Pinia | 3 | 状态管理（如 `stores/loginUser.ts` 登录用户状态） |
| vue-router | 4 | 路由管理（history 模式） |
| axios | 1.x | HTTP 请求（`src/request.ts` 统一封装实例） |
| markdown-it + highlight.js | 14.x / 11.x | Markdown 渲染与代码高亮（接口文档描述展示） |
| @umijs/openapi | 1.x | 由后端 OpenAPI 文档生成 TypeScript 请求代码（devDependency） |
| ESLint + Prettier | 9.x / 3.x | 代码检查与格式化 |

## 目录结构

| 目录 | 职责 |
| ---- | ---- |
| `src/access/` | 权限控制：角色枚举（`accessEnum.ts`）、权限检查函数（`checkAccess.ts`） |
| `src/api/` | 后端接口请求代码：由 openapi2ts 生成（`userController.ts`、`interfaceInfoController.ts`、`userInterfaceController.ts`、`imageController.ts`），类型定义在 `typings.d.ts` |
| `src/assets/` | 静态资源 |
| `src/components/` | 公共组件：`GlobalHeader`、`GlobalFooter` |
| `src/config/` | 配置：`env.ts`（当前生效的 API 基础地址）、`env.example.ts`（环境变量配置说明） |
| `src/layouts/` | 页面布局：`BasicLayout.vue` |
| `src/pages/` | 页面：`user/`（登录、注册、个人中心）、`interface/`（接口市场、接口详情）、`admin/`（接口管理、用户管理）、`HomePage.vue`（首页） |
| `src/router/` | 路由配置（`index.ts`） |
| `src/stores/` | Pinia 状态仓库：`loginUser.ts`（登录用户）、`counter.ts`（示例） |
| `src/utils/` | 工具函数（如 `time.ts` 时间格式化） |
| `src/request.ts` | axios 实例封装：baseURL、超时、withCredentials、40100 未登录自动跳转登录页 |

## 快速开始

### 安装依赖

```sh
npm install
```

### 启动开发服务器

```sh
npm run dev
```

启动后按终端输出的地址访问（默认 `http://localhost:5173`）。

### 生产构建

```sh
npm run build
```

该命令先执行 `vue-tsc --build` 类型检查，再执行 `vite build` 打包（两条命令通过 `npm-run-all2` 并行运行），产物输出到 `dist/`。只打包不做类型检查可用 `npm run build-only`，本地预览产物用 `npm run preview`。

### 代码检查

```sh
npm run lint
```

执行 `eslint . --fix`，自动修复可修复的问题。格式化代码使用：

```sh
npm run format
```

### 推荐 IDE

推荐使用 [VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) 插件（如果安装了 Vetur，请禁用），以获得 `.vue` 文件的 TypeScript 类型提示支持。

## 接口代码生成说明

本项目不使用手写的 axios/fetch 调用，而是用 `@umijs/openapi` 从后端 OpenAPI 文档自动生成 TypeScript 请求代码，避免前后端接口定义不一致。

### 配置文件

`openapi2ts.config.ts`：

```ts
export default {
  requestLibPath: "import request from '@/request'",
  schemaPath: 'http://localhost:18098/user/v3/api-docs',
  serversPath: './src',
}
```

- `schemaPath`：OpenAPI 文档地址，指向网关（18098）聚合的接口文档，重新生成前需保证后端网关运行中；
- `requestLibPath`：生成代码统一通过 `@/request` 中的 axios 实例发请求（自动携带登录 Cookie）；
- `serversPath`：生成代码输出目录为 `./src`，产物落在 `src/api/*.ts` 与 `src/api/typings.d.ts`。

### 重新生成命令

```sh
npm run openapi2ts
```

生成后可直接在页面中 import 使用，例如：

```ts
import { listInterfaceByPage } from '@/api/interfaceInfoController.ts'
```

### 响应约定

生成函数返回 axios response，`res.data.code === 0` 表示成功，真实数据在 `res.data.data`；`code === 40100` 表示未登录，`request.ts` 已自动跳转登录页，页面无需额外处理。

## 环境配置说明

### 当前生效配置

`src/config/env.ts` 中硬编码了 API 基础地址：

```ts
export const API_BASE_URL = 'http://localhost:18098'
```

前端请求直接指向后端网关 18098（`request.ts` 的 axios 实例以它为 baseURL，`withCredentials: true` 携带登录 Cookie）。

### 开发代理

`vite.config.ts` 中配置了 `/api` 路径代理（用于相对路径请求场景）：

```ts
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8183',
      // target: 'http://localhost:10001',
      changeOrigin: true,
      secure: false,
    },
  },
},
```

### 如何修改后端地址

- 当前代码通过 `src/config/env.ts` 中的 `API_BASE_URL` 直连后端，修改该值即可切换后端地址；
- 若改用相对路径 `/api` 由 Vite 代理转发，修改 `vite.config.ts` 中 `server.proxy['/api'].target`（文件中留有 `http://localhost:10001` 备用配置注释）；
- `src/config/env.example.ts` 提供了 `VITE_API_BASE_URL` 环境变量方案（`.env.local` / `.env.development` / `.env.production`）的说明，可按需落地。
