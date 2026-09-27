# Apple TV 风格电视盒子项目交接文档 (Project Handover)

> **文档版本**: 1.2 (对应代码 Release `v1.0.68` / Commit `9e6e221`)  
> **更新时间**: 2026-09-28  
> **适用场景**: 新对话无缝接续开发、团队协作交接、技术架构回顾

---

## 1. 项目基本信息

- **项目名称**: Apple TV 风格沉浸式电视盒子 (FongMi TVBox Leanback 重构版)
- **代码仓库**: `https://github.com/liujiawei-zuopin/apple-tvbox`
- **主要分支**: `main`
- **最新发布**: [Release v1.0.68](https://github.com/liujiawei-zuopin/apple-tvbox/releases)
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
│    - “▶ 立即播放” 纯白高亮药丸（聚焦时 1.08x 放大 + 发光光环）     │
│    - 聚焦“立即播放”时，按遥控器左右键可实时切换轮播海报及文字     │
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

## 3. 最新问题修复与架构优化 (v1.0.68)

| 序号 | 用户反馈问题 | 根因剖析 | 解决方案与实现代码 |
| :--- | :--- | :--- | :--- |
| **1** | 分类页切换时轮播海报闪烁/大小跳变 | `CategoryViewController` 在网络数据返回后在 `post` 中重复调用 `setLayoutParams` 触发重测算，叠加切页位移动画产生尺寸抖动 | 将 `categoryHeroSection` 固定为标准沉浸高度 476dp，仅初始化测量一次，彻底移除重复 `setLayoutParams`；切页采用纯淡入淡出无垂直位移跳动 |
| **2** | 轮播海报与“推荐”过渡生硬 | `gradient_category_hero_mask.xml` 底部仅 94% 不透明且颜色与分类主题底色存在色差，海报底边产生硬切边缘 | 引入专用无缝底部渐变遮罩 `gradient_category_hero_bottom_fade.xml`（180dp 高度，`#00101012` $\to$ `#FF101012`），多重深度羽化与主题底色 100% 融合 |
| **4** | 顶部药丸向下滚动时应向上移出 | 之前版本将 `updateTopBarOnScroll` 固定为了 `translationY = 0` | 恢复视差折叠算法：`mBinding.topBar.setTranslationY(-progress * maxCollapse); mBinding.topBar.setAlpha(1.0f - progress * 0.85f);`，向下滚动时平滑移出屏幕 |
| **5** | 向下滚动后回到药丸页面会闪一下 | 焦点重回顶部药丸同名 Tab 时触发了 `onTabSelected`，导致重新执行 `switchTab`（重置 alpha=0 并发起网络数据重拉取） | 增加当前 Tab 拦截守护：`if (position == mCurrentTab) { mBinding.topBar.animate().translationY(0)...; return; }`，回到药丸仅恢复顶部栏位置，零网络请求、零闪烁 |
| **6** | “立即播放”聚焦高亮态与左右键切换轮播海报 | 按钮聚焦前后均为纯白底色无视觉差异；按键监听仅处理上下键 | ① 改造为 Apple TV 经典状态：未聚焦为磨砂半透胶囊（`#33FFFFFF`），聚焦时为纯白发光胶囊（`#FFFFFFFF` + 1.08x 放大 + 黑色粗体图标文字）；<br>② 在 `categoryBtnPlay` 拦截 `DPAD_LEFT`/`DPAD_RIGHT`，联动 `prevHeroCarousel()` / `nextHeroCarousel()` 实时切换电影、简介与指示点 |

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

---

## 5. 关键设计约束与防踩坑守则

1. **分类海报高度严禁动态随意修改**:
   标准 1080P 下固定为 476dp，避免 Glide CenterCrop 矩阵在切页动画过程中重新计算导致的海报尺寸突跳。
2. **Tab 聚焦保护与防重载**:
   从下方列表按向上键回到当前 Tab 时，严禁触发数据重新请求或视图 alpha 重置。
3. **沉浸渐变遮罩多层合成**:
   全屏海报底部必须由暗色渐变层与底部 180dp 专用羽化层叠加，确保海报与主题背景 100% 融合。
4. **遥控器按键拦截完整性**:
   “立即播放”按钮必须响应 DPAD_LEFT / DPAD_RIGHT 并重置 7 秒自动轮播计时器。

---

## 6. 自动化回归测试验证脚本

本地已封装完整的自动化验证脚本 [`run_v68_pipeline.py`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/run_v68_pipeline.py)：
- 自动轮询 GitHub Actions CI 构建结果；
- 自动下载最新 Release APK 并安装至 MuMu 模拟器；
- 自动模拟遥控器键值并执行全部 15 步交互截图验证。
