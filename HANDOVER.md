# Apple TV 风格电视盒子项目交接文档 (Project Handover)

> **文档版本**: 1.9 (对应代码 Release `v1.0.79`)  
> **更新时间**: 2026-10-01  
> **适用场景**: 新对话无缝接续开发、团队协作交接、技术架构回顾

---

## 1. 项目基本信息

- **项目名称**: Apple TV 风格沉浸式电视盒子 (FongMi TVBox Leanback 重构版)
- **代码仓库**: `https://github.com/liujiawei-zuopin/apple-tvbox`
- **主要分支**: `main`
- **最新发布**: [Release v1.0.79](https://github.com/liujiawei-zuopin/apple-tvbox/releases)
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
│    - 100vh 全屏海报剧照 + 调深暗夜渐变遮罩 (Darker Scrim)          │
│    - “现在观看” 横版大卡片轮播                                   │
│    - “继续观看”、“正在热播” 6 列对齐货架                          │
│    - 向下滚动时：顶部药丸平滑向上移出屏幕，背景深暗磨砂化           │
│    - 向上回滚时：顶部药丸平滑复位，无闪烁刷新                     │
├─────────────────────────────────────────────────────────────────┤
│ 2. 分类页 (电影 / 剧集 / 综艺):                                 │
│    - 海报滚动区彻底移除暗色渐变遮罩挡光，纯净还原海报原生画质      │
│    - 标题、简介与 Meta 纯净纯白排版 (无文字阴影，极简 Apple TV 风格)│
│    - 标题、简介、播放按钮整体位于黄金视觉中下平衡区 (Top 170dp)    │
│    - “▶ 立即播放” 纯白胶囊，与上方简介保留 22dp 高级呼吸感间距     │
│    - 轮播指示点置于海报下部水平居中 (Bottom Margin 24dp)           │
│    - 底部露出半截（40%~50%）“推荐”栏卡片（保持原始沉浸露头排版）    │
│    - 左右键切换与自动轮播：标题/简介方向性滑入滑出+指示点平滑缩放 │
│    - 硬件加速 Alpha 渐变容器 (AlphaFadeFrameLayout, DST_IN 着色器) │
│      顶部 72% 实色完整呈现，底部 28% 平滑羽化消融至 Alpha 0，       │
│      与底层深暗高斯磨砂光斑底图及“推荐”栏背景无缝交融             │
│    - 内存数据级秒开缓存 (mCategoryCache)，Tab 切换零闪烁/零跳动    │
│    - 向下滚动时顶部药丸随画面向上滑出，向上返回顶部药丸平滑无重载 │
├─────────────────────────────────────────────────────────────────┤
│ 3. 专属暗色高斯毛玻璃背景 (FrostedGlassUtil + BlurView):          │
│    - 电影：更深邃深祖母绿真毛玻璃 (#050E09 + #00E599 减光晕光核)   │
│    - 剧集：更深邃深蓝宝石真毛玻璃 (#040811 + #0A84FF 减光晕光核)   │
│    - 综艺：更深邃深紫水晶真毛玻璃 (#09040D + #BF5AF2 减光晕光核)   │
│    - 顶部药丸：真硬件加速 BlurView 实时背景漫反射采样             │
├─────────────────────────────────────────────────────────────────┤
│ 4. 搜索页 (Search):                                             │
│    - 极简磨砂药丸容器，选中元素为白底黑字                          │
│    - 焦点可顺畅返回顶部药丸，向下滚动时药丸平滑移出               │
│    - 保持原标准中性暗色毛玻璃底图不变                             │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 最新问题修复与架构优化 (v1.0.78)

| 序号 | 用户反馈问题 | 根因剖析 | 解决方案与实现代码 |
| :--- | :--- | :--- | :--- |
| **1** | 分类海报标题/描述/立即播放按钮整体再往下移一点点 | 原 `marginTop="140dp"` 上方空间充裕，下移后在 476dp 容器内更加舒展稳重 | 将 `categoryHeroInfo` 的 `layout_marginTop` 由 `140dp` 调整至 `170dp`，处于 476dp 高度容器黄金中下平衡区。 |
| **2** | 首页的背景效果也暗深一点（海报当背景不变） | 首页原渐变暗光遮罩（`gradient_scrim_hero.xml`）透明度较高，底色偏灰 | 将首页 `gradient_scrim_hero.xml` 渐变加深为 `#59000000` $\to$ `#4D060608` $\to$ `#F8060608`，并将滚动磨砂滤镜 `heroFrostedOverlay` 加深至 `#8008080A`，在完整保留全屏海报剧照的同时呈现深邃暗夜影院质感。 |
| **3** | 海报轮播与“推荐”货架背景过渡生硬 | 之前使用静态渐变色块盖住海报底部（结束于硬编码纯色 `#08160F`），而底层是动态多光谱光斑（RadialGradient 光核），纯色色块在 476dp 底部边缘戛然而止，与底层不同亮度的磨砂光斑碰撞形成可见横线接缝 | **创建 `AlphaFadeFrameLayout` 硬件加速着色器容器**：<br>① 在 `dispatchDraw` 中通过 `PorterDuff.Mode.DST_IN` + 4 段式非线性 `LinearGradient` 将海报底部平滑羽化为透明（Alpha=0）；<br>② 彻底移除 `categoryHeroBottomFade` 纯色盖板，海报消融后自然裸露出底层贯穿全屏的 `categoryAmbientBackdrop` 真实高斯磨砂光斑，海报区与“推荐”栏共享同一张底图画布，实现 100% 绝对无缝过渡！ |
| **4** | 除首页和搜索外，分类页背景需要再深暗一点 | 之前分类页磨砂底图光核透明度较高、基色偏亮 | 在 `FrostedGlassUtil.java` 全面调深分类基底色并调低光晕光强（电影基色由 `#08160F` 调深至 `#050E09`，剧集基色由 `#070E1A` 调深至 `#040811`，综艺基色由 `#120717` 调深至 `#09040D`），同时保持首页海报毛玻璃与搜索页标准底图不变。 |

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
  - 硬件加速 Alpha 渐变容器: [`AlphaFadeFrameLayout.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/custom/AlphaFadeFrameLayout.java)
  - 布局文件: [`layout_category_channel.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/layout/layout_category_channel.xml)
  - 真实高斯磨砂引擎: [`FrostedGlassUtil.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/main/java/com/fongmi/android/tv/utils/FrostedGlassUtil.java)
  - 播放按钮选择器: [`selector_hero_play_btn.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/drawable/selector_hero_play_btn.xml)

