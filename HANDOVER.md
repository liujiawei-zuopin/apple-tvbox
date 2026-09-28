# Apple TV 风格电视盒子项目交接文档 (Project Handover)

> **文档版本**: 1.5 (对应代码 Release `v1.0.73`)  
> **更新时间**: 2026-09-28  
> **适用场景**: 新对话无缝接续开发、团队协作交接、技术架构回顾

---

## 1. 项目基本信息

- **项目名称**: Apple TV 风格沉浸式电视盒子 (FongMi TVBox Leanback 重构版)
- **代码仓库**: `https://github.com/liujiawei-zuopin/apple-tvbox`
- **主要分支**: `main`
- **最新发布**: [Release v1.0.73](https://github.com/liujiawei-zuopin/apple-tvbox/releases)
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
│    - 标题、简介位于垂直中高居中舒适区 (Top Margin 140dp)          │
│    - “▶ 立即播放” 纯白胶囊，与上方简介保留 22dp 高级呼吸感间距     │
│    - 轮播指示点置于底部水平居中区域 (Bottom Margin 24dp)           │
│    - 左右键切换与自动轮播：标题/简介方向性滑入滑出+指示点平滑缩放 │
│    - 底部 220dp 专属动态色系渐变过渡，与背景彻底融为一体无接缝      │
│    - 内存数据级秒开缓存 (mCategoryCache)，Tab 切换零闪烁/零跳动    │
│    - 底部露出半截（40%~50%）“推荐”栏卡片（高度预设稳定无二次重绘） │
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

## 3. 最新问题修复与架构优化 (v1.0.73)

| 序号 | 用户反馈问题 | 根因剖析 | 解决方案与实现代码 |
| :--- | :--- | :--- | :--- |
| **1** | 标题简介需要再往下靠一点 | 之前 `marginTop="96dp"` 偏靠上，未完全处于中轴视觉平衡区 | 将 `categoryHeroInfo` 的 `marginTop` 调整为 `140dp`，完美居于视口中轴线。 |
| **2** | 立即播放按钮与上面简介要有间距 | 之前 `categoryHeroActionRow` 的 `marginTop` 为 `12dp`，偏紧凑 | 将按钮行的 `marginTop` 增加至 `22dp`，形成富有呼吸感的排版层次。 |
| **3** | 轮播指示点放在靠下居中区域 | 之前指示点紧跟在播放按钮右侧 | 将 `categoryHeroDots` 移为 `categoryHeroSection` 的直接子 View，设置 `gravity="bottom|center_horizontal"` 与 `marginBottom="24dp"`，水平居中优雅悬浮在海报底部过渡区。 |
| **4** | 分类切换海报轮播闪烁 / 尺寸跳动 | ① 每次切 Tab 强制将容器 `alpha` 设为 0 重走动画；<br>② 异步网络回调完成后重复调用 `adjustHeroLayout` 修改 Section 高度引发二次重绘；<br>③ 缺乏内存缓存，切 Tab 先展示空白再渲染海报 | ① `HomeActivity` 新增 `mCategoryCache` 内存缓存，切 Tab 命中即刻同步上屏；<br>② 若分类容器已处于显示状态，直接做数据过渡，不重复打落 `alpha(0f)`；<br>③ `adjustHeroLayout` 改为在容器测量时预设完成，网络回调不再修改高度。 |
| **5** | 海报轮播与“推荐”货架背景过渡生硬 | `gradient_category_hero_bottom_fade.xml` 曾硬编码为 `#101012`（深灰色），与电影/剧集/综艺的暗色主题光核（祖母绿/蓝宝石/紫水晶）存在肉眼可见的色差接缝 | 在 `CategoryViewController` 实现 `updateCategoryTheme(tabType)`，动态构建从 `0x00...` $\to$ `0x66...` $\to$ `0xCC...` $\to$ `0xFF...` 的动态 `GradientDrawable`，使海报底部 220dp 100% 顺滑过渡至当前分类的真实背景底色。 |

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
  - 动态主题渐变引擎: `updateCategoryTheme(tabType)` (电影: `#08160F`, 剧集: `#070E1A`, 综艺: `#120717`)
  - 播放按钮选择器: [`selector_hero_play_btn.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/drawable/selector_hero_play_btn.xml)
