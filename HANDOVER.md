# Apple TV 风格电视盒子项目交接文档 (Project Handover)

> **文档版本**: 1.3 (对应代码 Release `v1.0.70` / Commit `5ebbbab`)  
> **更新时间**: 2026-09-28  
> **适用场景**: 新对话无缝接续开发、团队协作交接、技术架构回顾

---

## 1. 项目基本信息

- **项目名称**: Apple TV 风格沉浸式电视盒子 (FongMi TVBox Leanback 重构版)
- **代码仓库**: `https://github.com/liujiawei-zuopin/apple-tvbox`
- **主要分支**: `main`
- **最新发布**: [Release v1.0.70](https://github.com/liujiawei-zuopin/apple-tvbox/releases)
- **包名与启动 Activity**: `com.fongmi.android.tv` / `com.fongmi.android.tv.ui.activity.HomeActivity`
- **模拟器/测试设备**: MuMu 模拟器 Android 12 (1080P TV 模式, `127.0.0.1:16384`)
- **本地 ADB 路径**: `D:\Program Files\Netease\MuMuPlayer\nx_device\15.0\shell\adb.exe`

---

## 2. 核心视觉体系与已实现功能

```
┌─────────────────────────────────────────────────────────────────┐
│                    顶部磨砂药丸导航栏 (Top Nav)                     │
│               [ 主页 ]  [ 电影 ]  [ 剧集 ]  [ 综艺 ]  [ 🔍 ]       │
├─────────────────────────────────────────────────────────────────┤
│ 1. 主页 (Home):                                                 │
│    - 100vh 全屏海报轮播 + 渐变遮罩                                │
│    - “现在观看” 横版大卡片轮播                                   │
│    - “继续观看”、“正在热播” 6 列对齐货架                          │
│    - 向下滚动时：顶部药丸平滑向上移出屏幕，背景自动模糊            │
│    - 向上回滚时：顶部药丸平滑复位，无闪烁刷新                     │
├─────────────────────────────────────────────────────────────────┤
│ 2. 分类页 (电影 / 剧集 / 综艺):                                 │
│    - 100vh 全屏沉浸海报轮播，文字位于中下部黄金视觉区               │
│    - “▶ 立即播放” 完美无裁切纯白胶囊（聚焦时 1.08x 放大，左原点锚定）│
│    - 左右键切换与自动轮播：标题/简介方向性滑入滑出+指示点平滑缩放 │
│    - 海报底部采用双层无缝渐变过渡层，彻底消除硬切横线               │
│    - 底部露出半截（40%~50%）“推荐”栏卡片（476dp 稳定布局无闪烁）  │
│    - 向下滚动时顶部药丸随画面向上滑出，向上返回顶部药丸平滑无重载 │
├─────────────────────────────────────────────────────────────────┤
│ 3. 专属暗色高斯毛玻璃背景 (FrostedGlassUtil + BlurView):          │
│    - 电影：深祖母绿真毛玻璃 (#08160F + #00E599 光核)              │
│    - 剧集：深蓝宝石真毛玻璃 (#070E1A + #0A84FF 光核)              │
│    - 综艺：深紫水晶真毛玻璃 (#120717 + #BF5AF2 光核)              │
│    - 顶部药丸：真硬件加速 BlurView 实时背景漫反射采样             │
├─────────────────────────────────────────────────────────────────┤
│ 4. 搜索页 (Search):                                             │
│    - 极简磨砂药丸容器，选中元素为白底黑字                          │
│    - 焦点可顺畅返回顶部药丸，向下滚动时药丸平滑移出               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 最新问题修复与架构优化 (v1.0.70)

| 序号 | 用户反馈问题 | 根因剖析 | 解决方案与实现代码 |
| :--- | :--- | :--- | :--- |
| **1** | “立即播放”按钮左侧被直角切边（Bug） | 按钮聚焦放大（1.08x）时以中心为原点，左边缘进入负坐标，被父布局 `categoryHeroInfo` 的 `clipChildren=true` 边界裁剪 | ① 将按钮缩放锚点设为左侧中心 `setPivotX(0f)`，向右上方平滑放大；<br>② 在 `categoryHeroInfo` 与 `categoryHeroActionRow` 显式声明 `clipChildren="false"` 与 `clipToPadding="false"`；<br>③ 简化为纯净 18dp 圆角纯白背景，根除裁切毛边。 |
| **2** | 轮播海报切换无动画效果 | 之前仅有 Glide 单图 CrossFade，文字简介直接突变，指示点直接跳变宽度 | ① 将标题与简介包装入 `categoryHeroTextGroup`，切换时执行带方向性的 Slide-Fade（滑出 120ms $\to$ 滑入 180ms）；<br>② 指示点切换引入 `ValueAnimator` 平滑过渡宽度（6dp $\leftrightarrow$ 16dp）；<br>③ 背景海报配合 250ms 优雅淡入。 |
| **3** | 分类页切换海报尺寸跳变 | 异步数据返回重复触发 `setLayoutParams` 导致 Glide 矩阵重算 | 固定 `categoryHeroSection` 为 476dp，移除重复布局测算。 |
| **4** | 海报与“推荐”过渡硬切 | 底部渐变遮罩未覆盖至纯色主题底 | 引入 `gradient_category_hero_bottom_fade.xml`（180dp 渐变至 `#FF101012`）。 |
| **5** | 向下滚动时药丸未移出 | `updateTopBarOnScroll` 之前被固定 | 恢复视差向上折叠算法（跟随滚动距离平滑移出）。 |
| **6** | 回到药丸页面闪烁 | 聚焦当前 Tab 时重复触发 `switchTab` | 增加当前 Tab 拦截守护，仅复位顶部栏坐标，零重载零闪烁。 |

---

## 4. 关键架构设计与源码对照

### 4.1 顶部药丸导航系统 (`TopNavController`)
- **源码文件**:
  - 控制器: [`TopNavController.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/home/TopNavController.java)
  - 适配器: [`TopNavAdapter.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/adapter/TopNavAdapter.java)
  - 药丸背景: [`bg_top_capsule_bar.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/drawable/bg_top_capsule_bar.xml)

### 4.2 分类频道控制器与全屏海报 (`CategoryViewController`)
- **源码文件**:
  - 控制器: [`CategoryViewController.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/home/CategoryViewController.java)
  - 布局文件: [`layout_category_channel.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/layout/layout_category_channel.xml)
  - 海报渐变过渡: [`gradient_category_hero_mask.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/drawable/gradient_category_hero_mask.xml)
  - 底部渐变羽化: [`gradient_category_hero_bottom_fade.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/drawable/gradient_category_hero_bottom_fade.xml)
  - 播放按钮聚焦状态: [`bg_hero_play_btn_focused.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/drawable/bg_hero_play_btn_focused.xml)
  - 播放按钮正常状态: [`bg_hero_play_btn_normal.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/drawable/bg_hero_play_btn_normal.xml)
