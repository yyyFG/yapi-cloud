<template>
  <div id="homePage">
    <!-- 极淡网格背景 -->
    <div class="grid-bg" aria-hidden="true"></div>

    <!-- Hero 区域 -->
    <section class="hero">
      <div class="container hero-inner">
        <div class="hero-badge">
          <ThunderboltOutlined />
          yApiRelay — 接口中转站
        </div>
        <h1 class="hero-title">
          发现优质 API，
          <span class="hero-title-accent">让开发更快一步</span>
        </h1>
        <p class="hero-subtitle">
          一站式接口市场：浏览热门接口、在线调试调用、一键申请开通，把后端能力轻松接入你的应用。
        </p>
        <div class="hero-actions">
          <a-button type="primary" size="large" class="btn-primary" @click="scrollTo('rank')">
            查看热门接口
            <template #icon>
              <ArrowRightOutlined />
            </template>
          </a-button>
          <a-button size="large" class="btn-outline" @click="scrollTo('features')">
            了解平台特性
          </a-button>
        </div>
      </div>
    </section>

    <!-- 特性卡片 -->
    <section id="features" class="container section">
      <div class="section-head">
        <h2 class="section-title">核心特性</h2>
        <p class="section-desc">为开发者打造的接口开放平台</p>
      </div>
      <div class="feature-grid">
        <div class="feature-card" v-for="feature in features" :key="feature.title">
          <div class="feature-icon">
            <component :is="feature.icon" />
          </div>
          <h3 class="feature-title">{{ feature.title }}</h3>
          <p class="feature-desc">{{ feature.desc }}</p>
        </div>
      </div>
    </section>

    <!-- 调用排行榜 -->
    <section id="rank" class="rank-section">
      <div class="container">
        <div class="section-head">
          <h2 class="section-title">
            <TrophyOutlined class="section-icon" />
            调用排行榜
          </h2>
          <p class="section-desc">最受开发者欢迎的接口 Top 10</p>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="rank-skeleton">
          <a-skeleton active :paragraph="{ rows: 5 }" />
        </div>

        <!-- 空态（后端未启动 / 无数据） -->
        <a-empty
          v-else-if="rankList.length === 0"
          class="rank-empty"
          description="暂无排行数据（后端服务启动后自动展示真实调用排行）"
        />

        <!-- 排行榜列表 -->
        <ul v-else class="rank-list">
          <li v-for="(item, index) in rankList" :key="item.id" class="rank-item">
            <span class="rank-no" :class="`rank-no-${index + 1}`">{{ index + 1 }}</span>
            <div class="rank-info">
              <span class="rank-name">{{ item.interfaceName }}</span>
              <span class="rank-desc">{{ item.description }}</span>
            </div>
            <span class="rank-count">
              <RiseOutlined />
              {{ (item.invokeCount ?? 0).toLocaleString() }} 次调用
            </span>
          </li>
        </ul>
      </div>
    </section>

    <!-- 底部 CTA -->
    <section class="container cta-section">
      <div class="cta-card">
        <h2 class="cta-title">准备好把你的应用接入强大接口了吗？</h2>
        <p class="cta-desc">浏览、调用、构建，从 接口中转站开始。</p>
        <div class="cta-actions">
          <a-button type="primary" size="large" class="btn-primary" @click="router.push('/interfaces')">
            查看接口市场
            <template #icon>
              <ArrowRightOutlined />
            </template>
          </a-button>
          <a-button size="large" class="btn-outline-on-blue" @click="scrollTo('features')">
            了解平台特性
          </a-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  ApiOutlined,
  ArrowRightOutlined,
  ExperimentOutlined,
  RiseOutlined,
  ThunderboltOutlined,
  TrophyOutlined,
} from '@ant-design/icons-vue'
import { listInterfaceRank } from '@/api/interfaceInfoController.ts'

// 路由实例（底部 CTA 按钮跳转接口市场用）
const router = useRouter()

// 特性卡片内容
const features = [
  {
    icon: ApiOutlined,
    title: '海量接口',
    desc: '覆盖多领域的优质接口，一站式浏览与检索，快速找到你需要的 API。',
  },
  {
    icon: ExperimentOutlined,
    title: '在线调试',
    desc: '无需搭建本地环境，页面内填写参数即可实时调用，结果即时返回。',
  },
  {
    icon: ThunderboltOutlined,
    title: '灵活开通',
    desc: '一键提交申请，快速审核开通，接口权限管理清晰可控。',
  },
]

// 排行榜数据
const rankList = ref<API.InterfaceRankVO[]>([])
const loading = ref(true)

// 加载排行榜（Top 10）
const loadRank = async () => {
  loading.value = true
  try {
    const res = await listInterfaceRank()
    if (res.data.code === 0 && res.data.data) {
      rankList.value = res.data.data.slice(0, 10)
    } else {
      rankList.value = []
    }
  } catch (error) {
    console.error('加载排行榜失败：', error)
    rankList.value = []
  } finally {
    loading.value = false
  }
}

// 平滑滚动到指定区域
const scrollTo = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
}

onMounted(() => {
  loadRank()
})
</script>

<style scoped>
#homePage {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background: #ffffff;
  overflow: hidden;
}

/* 极淡网格背景 */
.grid-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.035) 1px, transparent 1px);
  background-size: 48px 48px;
  -webkit-mask-image: radial-gradient(ellipse 80% 60% at 50% 0%, #000 40%, transparent 100%);
  mask-image: radial-gradient(ellipse 80% 60% at 50% 0%, #000 40%, transparent 100%);
}

.container {
  max-width: 1120px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
  z-index: 1;
}

/* ---------- Hero ---------- */
.hero {
  padding: 96px 0 72px;
  text-align: center;
  background:
    radial-gradient(ellipse 720px 360px at 50% -10%, rgba(59, 130, 246, 0.1) 0%, transparent 70%);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 16px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 28px;
}

.hero-title {
  margin: 0 0 20px;
  font-size: 52px;
  font-weight: 700;
  line-height: 1.2;
  letter-spacing: -1px;
  color: #0f172a;
}

.hero-title-accent {
  color: #2563eb;
}

.hero-subtitle {
  margin: 0 auto 40px;
  max-width: 640px;
  font-size: 18px;
  line-height: 1.7;
  color: #64748b;
}

.hero-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.btn-primary {
  height: 48px;
  padding: 0 28px;
  border-radius: 12px;
  font-size: 16px;
  background: #2563eb;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.3);
}

.btn-primary:hover {
  background: #3b82f6 !important;
}

.btn-outline {
  height: 48px;
  padding: 0 28px;
  border-radius: 12px;
  font-size: 16px;
  color: #2563eb;
  border-color: #2563eb;
  background: #fff;
}

.btn-outline:hover {
  color: #3b82f6 !important;
  border-color: #3b82f6 !important;
}

.btn-outline-on-blue {
  height: 48px;
  padding: 0 28px;
  border-radius: 12px;
  font-size: 16px;
  color: #2563eb;
  border-color: #2563eb;
  background: #fff;
}

.btn-outline-on-blue:hover {
  color: #3b82f6 !important;
  border-color: #3b82f6 !important;
}

/* ---------- 通用 section ---------- */
.section {
  padding: 64px 0;
}

.section-head {
  text-align: center;
  margin-bottom: 48px;
}

.section-title {
  margin: 0 0 12px;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: #0f172a;
}

.section-icon {
  color: #2563eb;
  margin-right: 8px;
}

.section-desc {
  margin: 0;
  font-size: 16px;
  color: #64748b;
}

/* ---------- 特性卡片 ---------- */
.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  padding: 36px 28px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  transition: all 0.25s ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  background: #ffffff;
  border-color: #bfdbfe;
  box-shadow: 0 12px 32px rgba(37, 99, 235, 0.1);
}

.feature-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  margin-bottom: 20px;
  border-radius: 14px;
  background: #dbeafe;
  color: #2563eb;
  font-size: 24px;
}

.feature-title {
  margin: 0 0 10px;
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
}

.feature-desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: #64748b;
}

/* ---------- 排行榜 ---------- */
.rank-section {
  padding: 64px 0;
  background:
    radial-gradient(ellipse 800px 400px at 50% 0%, rgba(59, 130, 246, 0.06) 0%, transparent 70%),
    #f8fafc;
}

.rank-skeleton {
  max-width: 760px;
  margin: 0 auto;
  padding: 16px;
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}

.rank-empty {
  margin: 24px auto;
}

.rank-list {
  list-style: none;
  margin: 0 auto;
  padding: 8px 24px;
  max-width: 760px;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 32px rgba(15, 23, 42, 0.04);
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 8px;
  border-bottom: 1px solid #f1f5f9;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-no {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 700;
  color: #64748b;
  background: #f1f5f9;
}

.rank-no-1 {
  color: #ffffff;
  background: #2563eb;
}

.rank-no-2 {
  color: #ffffff;
  background: #3b82f6;
}

.rank-no-3 {
  color: #ffffff;
  background: #93c5fd;
}

.rank-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rank-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.rank-desc {
  font-size: 14px;
  color: #64748b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-count {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 14px;
  font-weight: 500;
}

/* ---------- 底部 CTA ---------- */
.cta-section {
  padding: 72px 0 88px;
}

.cta-card {
  padding: 64px 32px;
  text-align: center;
  border-radius: 20px;
  background:
    radial-gradient(ellipse 600px 280px at 50% -20%, rgba(255, 255, 255, 0.25) 0%, transparent 70%),
    linear-gradient(135deg, #2563eb 0%, #3b82f6 100%);
}

.cta-title {
  margin: 0 0 14px;
  font-size: 32px;
  font-weight: 700;
  letter-spacing: -0.5px;
  color: #ffffff;
}

.cta-desc {
  margin: 0 0 36px;
  font-size: 16px;
  color: rgba(255, 255, 255, 0.85);
}

.cta-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.cta-actions .btn-primary {
  background: #ffffff;
  color: #2563eb;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.2);
}

.cta-actions .btn-primary:hover {
  background: #eff6ff !important;
  color: #2563eb !important;
}

/* ---------- 响应式 ---------- */
@media (max-width: 768px) {
  .hero {
    padding: 64px 0 48px;
  }

  .hero-title {
    font-size: 34px;
  }

  .hero-subtitle {
    font-size: 16px;
  }

  .feature-grid {
    grid-template-columns: 1fr;
  }

  .section-title {
    font-size: 28px;
  }

  .rank-item {
    gap: 12px;
  }

  .rank-desc {
    max-width: 180px;
  }

  .cta-title {
    font-size: 24px;
  }
}
</style>
