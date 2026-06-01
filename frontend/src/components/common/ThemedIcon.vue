<script setup>
defineProps({
  name: { type: String, required: true },
  size: { type: Number, default: 16 }
})

// 动物档案馆手账风图标集
// 风格：细线条、圆角端点、手绘感、暖色调
const icons = {
  // 爪印 — 墨水印章感
  paw: `<ellipse cx="6.5" cy="5" rx="2" ry="2.2"/>
    <ellipse cx="11" cy="3.8" rx="1.6" ry="1.8"/>
    <ellipse cx="14.8" cy="6" rx="1.5" ry="1.7"/>
    <ellipse cx="3.8" cy="8.2" rx="1.5" ry="1.7"/>
    <path d="M7.5 11.5c-.8 1.8-1.5 3.2-1.5 4.2 0 1.2.8 1.8 1.8 1.8s2-.6 2-1.8c0-1-.5-2.2-1.2-3.5-.3-.5-.6-.5-1.1-.7z"/>`,

  // 记录折线 — 手绘曲线 + 上升箭头
  trend: `<path d="M3 15c1.5-3 3-5 5-5s2.5 2 4 0 3-5 5-7" fill="none" stroke-width="1.5" stroke-linecap="round"/>
    <polyline points="14,3 17,3 17,6" fill="none" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
    <circle cx="8" cy="10" r="1" fill="currentColor" stroke="none"/>`,

  // 书签 — 丝带书签
  bookmark: `<path d="M6 2h8v16l-4-2.5L6 18V2z" fill="none" stroke-width="1.4" stroke-linejoin="round"/>
    <line x1="8" y1="6" x2="12" y2="6" stroke-width="1.2" stroke-linecap="round"/>
    <line x1="8" y1="9" x2="11" y2="9" stroke-width="1.2" stroke-linecap="round"/>`,

  // 铃铛 — 项圈小铃铛
  bell: `<path d="M10 2.5c-2.5 0-4 2-4 4.5 0 3-1.5 4-1.5 4h11s-1.5-1-1.5-4c0-2.5-1.5-4.5-4-4.5z" fill="none" stroke-width="1.4" stroke-linejoin="round"/>
    <path d="M7.5 15.5a2.5 2.5 0 005 0" fill="none" stroke-width="1.4" stroke-linecap="round"/>
    <circle cx="10" cy="17.5" r="0.8" fill="currentColor" stroke="none"/>`,

  // 听诊器 — 兽医问诊卡风
  stethoscope: `<path d="M7 3v5.5a3 3 0 006 0V3" fill="none" stroke-width="1.4" stroke-linecap="round"/>
    <circle cx="5" cy="13" r="2.2" fill="none" stroke-width="1.4"/>
    <path d="M5 15.2v1.3" stroke-width="1.4" stroke-linecap="round"/>
    <circle cx="15" cy="13" r="2.2" fill="none" stroke-width="1.4"/>
    <path d="M15 15.2v1.3" stroke-width="1.4" stroke-linecap="round"/>
    <line x1="12" y1="11" x2="15" y2="11" stroke-width="1.2" stroke-linecap="round"/>`,

  // 卷轴 — 纸质卷轴 / 时间线
  scroll: `<path d="M5 4c0-1 .8-2 2-2h6c1.2 0 2 1 2 2v1H5V4z" fill="none" stroke-width="1.3"/>
    <rect x="4" y="5" width="12" height="10" rx="1" fill="none" stroke-width="1.4"/>
    <path d="M4 5c-1 0-2 .8-2 2s1 2 2 2" fill="none" stroke-width="1.3" stroke-linecap="round"/>
    <path d="M16 5c1 0 2 .8 2 2s-1 2-2 2" fill="none" stroke-width="1.3" stroke-linecap="round"/>
    <line x1="7" y1="9" x2="13" y2="9" stroke-width="1.1" stroke-linecap="round"/>
    <line x1="7" y1="12" x2="11" y2="12" stroke-width="1.1" stroke-linecap="round"/>`,

  // 对话便签 — 问诊卡 / 便签纸
  chat: `<rect x="3" y="3" width="14" height="11" rx="1.5" fill="none" stroke-width="1.4"/>
    <path d="M7 14l-2 3.5v-3.5" fill="none" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
    <line x1="6" y1="7.5" x2="14" y2="7.5" stroke-width="1.1" stroke-linecap="round"/>
    <line x1="6" y1="10.5" x2="11" y2="10.5" stroke-width="1.1" stroke-linecap="round"/>
    <circle cx="8.5" cy="5.5" r="0.6" fill="currentColor" stroke="none"/>
    <circle cx="11.5" cy="5.5" r="0.6" fill="currentColor" stroke="none"/>`,

  // 旧书 — 护理手册 / 档案册
  book: `<path d="M10 3v14" stroke-width="1.3" stroke-linecap="round"/>
    <path d="M4 3h6v14H5.5C4.2 17 3 16.5 3 15V4.5C3 3.7 3.5 3 4 3z" fill="none" stroke-width="1.4"/>
    <path d="M16 3h-6v14h4.5c1.3 0 2.5-.5 2.5-2V4.5c0-.8-.5-1.5-1-1.5z" fill="none" stroke-width="1.4"/>
    <line x1="5.5" y1="7" x2="8.5" y2="7" stroke-width="1" stroke-linecap="round"/>
    <line x1="5.5" y1="9.5" x2="7.5" y2="9.5" stroke-width="1" stroke-linecap="round"/>`,

  // 爪印 + — 新宠物档案
  pawPlus: `<ellipse cx="6" cy="4.5" rx="1.6" ry="1.8"/>
    <ellipse cx="9.5" cy="3.5" rx="1.3" ry="1.5"/>
    <ellipse cx="12.5" cy="5" rx="1.2" ry="1.4"/>
    <ellipse cx="4" cy="7" rx="1.2" ry="1.4"/>
    <path d="M7 9.5c-.5 1.2-1 2.2-1 3 0 .8.5 1.2 1.2 1.2s1.5-.4 1.5-1.2c0-.7-.4-1.6-.8-2.5-.2-.3-.4-.3-.9-.5z"/>
    <line x1="15" y1="9" x2="15" y2="15" stroke-width="1.5" stroke-linecap="round"/>
    <line x1="12" y1="12" x2="18" y2="12" stroke-width="1.5" stroke-linecap="round"/>`,

  // 相纸 — 旧相册 / 拍立得
  image: `<rect x="3" y="3" width="14" height="14" rx="1" fill="none" stroke-width="1.4"/>
    <rect x="5" y="5" width="10" height="7" rx="0.5" fill="none" stroke-width="1.1"/>
    <circle cx="8" cy="8.5" r="1.3" fill="none" stroke-width="1.1"/>
    <line x1="5" y1="14" x2="15" y2="14" stroke-width="1.1"/>
    <line x1="8" y1="15.5" x2="12" y2="15.5" stroke-width="0.8" stroke-linecap="round"/>`,

  // 档案页 — 成长报告
  file: `<path d="M6 2h6l4 4v11a1 1 0 01-1 1H6a1 1 0 01-1-1V3a1 1 0 011-1z" fill="none" stroke-width="1.4" stroke-linejoin="round"/>
    <polyline points="12,2 12,6 16,6" fill="none" stroke-width="1.4"/>
    <line x1="7" y1="10" x2="13" y2="10" stroke-width="1" stroke-linecap="round"/>
    <line x1="7" y1="12.5" x2="11" y2="12.5" stroke-width="1" stroke-linecap="round"/>
    <line x1="7" y1="15" x2="10" y2="15" stroke-width="1" stroke-linecap="round"/>`,

  // 积木 — 3D 模型
  cube: `<path d="M10 2L3.5 6v8L10 18l6.5-4V6L10 2z" fill="none" stroke-width="1.4" stroke-linejoin="round"/>
    <line x1="10" y1="10.5" x2="10" y2="18" stroke-width="1.1"/>
    <line x1="10" y1="10.5" x2="3.5" y2="6.5" stroke-width="1.1"/>
    <line x1="10" y1="10.5" x2="16.5" y2="6.5" stroke-width="1.1"/>
    <circle cx="10" cy="10.5" r="0.8" fill="currentColor" stroke="none"/>`,

  // 羽毛笔 — 书写 / 时光助手
  quill: `<path d="M16.5 2.5c-1.5 1-4 3.5-6.5 6.5l-1.5 3c-.2.4 0 .6.3.8l3-1.5c3-2.5 5.5-5 6.5-6.5.3-.3.3-.8 0-1.1-.3-.3-.8-.3-1.1 0z" fill="none" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/>
    <path d="M8.5 12L5.5 15.5" stroke-width="1.4" stroke-linecap="round"/>
    <path d="M5.5 15.5L4 17.5" stroke-width="1.2" stroke-linecap="round"/>
    <path d="M5.5 15.5L7.5 16.5" stroke-width="1.2" stroke-linecap="round"/>`,

  // 爱心 — 温暖爱心
  heart: `<path d="M10 16.5S3.5 12.5 3.5 8c0-2.2 1.5-3.5 3.2-3.5 1.2 0 2.3.8 2.8 1.3.5-.5 1.6-1.3 2.8-1.3 1.7 0 3.2 1.3 3.2 3.5 0 4.5-6.5 8.5-6.5 8.5z" fill="none" stroke-width="1.4" stroke-linejoin="round"/>`,

  // 小旗 — 标记 / 里程碑
  flag: `<line x1="5.5" y1="2.5" x2="5.5" y2="17.5" stroke-width="1.4" stroke-linecap="round"/>
    <path d="M5.5 2.5h8.5l-2.5 3.5 2.5 3.5H5.5" fill="none" stroke-width="1.4" stroke-linejoin="round"/>`,

  // 用户 — 档案人像
  user: `<circle cx="10" cy="6.5" r="3" fill="none" stroke-width="1.4"/>
    <path d="M4 17c0-3 2.7-5.5 6-5.5s6 2.5 6 5.5" fill="none" stroke-width="1.4" stroke-linecap="round"/>`,

  // 档案夹 — 分类目录
  folder: `<path d="M3 5.5a1 1 0 011-1h3.5l2 2h7a1 1 0 011 1v8a1 1 0 01-1 1H4a1 1 0 01-1-1v-10z" fill="none" stroke-width="1.4"/>
    <line x1="3" y1="9" x2="17" y2="9" stroke-width="1.1"/>`,

  // 项圈挂牌 — 宠物标识
  collar: `<path d="M5 9.5c0-3.5 2.2-5.5 5-5.5s5 2 5 5.5" fill="none" stroke-width="1.5" stroke-linecap="round"/>
    <circle cx="10" cy="12" r="2.5" fill="none" stroke-width="1.4"/>
    <line x1="10" y1="14.5" x2="10" y2="17.5" stroke-width="1.3" stroke-linecap="round"/>
    <circle cx="10" cy="12" r="0.8" fill="currentColor" stroke="none"/>`,

  // 爱心脉搏 — 健康档案
  heartPulse: `<path d="M10 16.5S3.5 12.5 3.5 8c0-2.2 1.5-3.5 3.2-3.5 1.2 0 2.3.8 2.8 1.3.5-.5 1.6-1.3 2.8-1.3 1.7 0 3.2 1.3 3.2 3.5 0 4.5-6.5 8.5-6.5 8.5z" fill="none" stroke-width="1.4" stroke-linejoin="round"/>
    <polyline points="6,10 8.5,10 9.5,8 11,12 12.5,10 14,10" fill="none" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>`,

  // 双相纸 — 成长相册
  images: `<rect x="5" y="5" width="11" height="11" rx="1" fill="none" stroke-width="1.3"/>
    <circle cx="8.5" cy="8.5" r="1.2" fill="none" stroke-width="1.1"/>
    <polyline points="5,13 8,10 10.5,12.5 12,11 16,14" fill="none" stroke-width="1.1" stroke-linecap="round" stroke-linejoin="round"/>
    <rect x="3" y="3" width="11" height="11" rx="1" fill="none" stroke-width="1.1" stroke-dasharray="2 1.5"/>`,

  // 水平省略 — 更多
  dots: `<circle cx="5" cy="10" r="1.2" fill="currentColor" stroke="none"/>
    <circle cx="10" cy="10" r="1.2" fill="currentColor" stroke="none"/>
    <circle cx="15" cy="10" r="1.2" fill="currentColor" stroke="none"/>`,

  // 餐碗 — 饮食记录
  bowl: `<path d="M3 10h14c0 4-3 7-7 7s-7-3-7-7z" fill="none" stroke-width="1.4" stroke-linejoin="round"/>
    <path d="M6 10c1-2 2.5-3 4-3s3 1 4 3" fill="none" stroke-width="1.1" stroke-linecap="round"/>
    <line x1="10" y1="17" x2="10" y2="18.5" stroke-width="1.3" stroke-linecap="round"/>`,

  // 蛋糕 — 纪念日
  cake: `<rect x="4" y="10" width="12" height="7" rx="1" fill="none" stroke-width="1.4"/>
    <path d="M4 13h12" stroke-width="1.1"/>
    <path d="M7 10V8.5a1.5 1.5 0 013 0V10" fill="none" stroke-width="1.2"/>
    <path d="M12 10V8.5a1.5 1.5 0 013 0V10" fill="none" stroke-width="1.2"/>
    <line x1="8.5" y1="7" x2="8.5" y2="5.5" stroke-width="1" stroke-linecap="round"/>
    <circle cx="8.5" cy="5" r="0.6" fill="currentColor" stroke="none"/>
    <line x1="13.5" y1="7" x2="13.5" y2="5.5" stroke-width="1" stroke-linecap="round"/>
    <circle cx="13.5" cy="5" r="0.6" fill="currentColor" stroke="none"/>`,

  // 柱形图 — 数据报告
  chart: `<line x1="4" y1="17" x2="16" y2="17" stroke-width="1.3" stroke-linecap="round"/>
    <rect x="5" y="11" width="2.5" height="6" rx="0.5" fill="none" stroke-width="1.2"/>
    <rect x="8.75" y="7" width="2.5" height="10" rx="0.5" fill="none" stroke-width="1.2"/>
    <rect x="12.5" y="4" width="2.5" height="13" rx="0.5" fill="none" stroke-width="1.2"/>`,

  // 双人 — 家庭成员
  users: `<circle cx="7.5" cy="7" r="2.5" fill="none" stroke-width="1.3"/>
    <path d="M3 16c0-2.5 2-4.5 4.5-4.5s4.5 2 4.5 4.5" fill="none" stroke-width="1.3" stroke-linecap="round"/>
    <circle cx="13.5" cy="7.5" r="1.8" fill="none" stroke-width="1.1"/>
    <path d="M12 16c0-1.8 1-3 2.5-3.5" fill="none" stroke-width="1.1" stroke-linecap="round"/>`,

  // 齿轮 — 设置
  gear: `<circle cx="10" cy="10" r="2.5" fill="none" stroke-width="1.4"/>
    <path d="M10 2.5v2M10 15.5v2M2.5 10h2M15.5 10h2M4.7 4.7l1.4 1.4M13.9 13.9l1.4 1.4M15.3 4.7l-1.4 1.4M6.1 13.9l-1.4 1.4" stroke-width="1.3" stroke-linecap="round"/>`,
}
</script>

<template>
  <svg
    :width="size" :height="size"
    viewBox="0 0 20 20"
    fill="none"
    stroke="currentColor"
    class="themed-icon"
    v-html="icons[name]"
  />
</template>

<style scoped>
.themed-icon {
  flex-shrink: 0;
  vertical-align: middle;
  stroke-linecap: round;
  stroke-linejoin: round;
}
</style>
