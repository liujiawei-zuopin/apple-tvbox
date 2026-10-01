# TV+ (Apple TV 风格电视盒子项目交接文档)

> **文档版本**: 2.3 (对应全新命名 `TV+` / 包名 `com.tvplus`)  
> **更新时间**: 2026-10-01  
> **适用场景**: 新对话无缝接续开发、团队协作交接、技术架构全景回顾

---

## 1. 项目基本信息

- **软件名称**: TV+ (Apple TV 风格沉浸式电视盒子)
- **代码仓库**: `https://github.com/liujiawei-zuopin/apple-tvbox`
- **主要分支**: `main`
- **最新发布**: [Releases](https://github.com/liujiawei-zuopin/apple-tvbox/releases)
- **包名与启动 Activity**: `com.tvplus` / `com.fongmi.android.tv.ui.activity.HomeActivity`
- **模拟器/测试设备**: MuMu 模拟器 Android 12/15 (1080P TV 模式, `127.0.0.1:16384`)
- **本地 ADB 路径**: `D:\Program Files\Netease\MuMuPlayer\nx_main\adb.exe`

---

## 2. 核心架构与 3 大精度交互优化 (v2.2)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       顶部磨砂药丸导航栏 (Top Nav)                              │
│               [ 主页 ]  [ 电影 ]  [ 剧集 ]  [ 综艺 ]  [ 🔍 ]                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ 1. 顶栏固定标准 4+1 频道体系:                                                │
│    - 固定 4 大主分类 + 搜索: [主页] [电影] [剧集] [综艺] [🔍]                  │
│    - 其他非标准频道（动漫、纪录片、少儿、短剧等）收拢于侧边抽屉或可选定列表      │
│    - 保持顶栏极简纯粹，杜绝站点海量分类撑爆顶栏                                │
├─────────────────────────────────────────────────────────────────────────────┤
│ 2. 全频道 Apple TV 级平滑切换动效 (Fluid Channel Switch Motion):            │
│    - 主页 <-> 电影 <-> 剧集 <-> 综艺 <-> 搜索 横向频道切换具备物理方向滑动感      │
│    - 采用 32dp 方向位移 + 240ms DecelerateInterpolator 减速曲线 + CrossFade      │
│    - 频道与侧边抽屉切换时具备多光谱高斯毛玻璃背景无缝渐变                        │
├─────────────────────────────────────────────────────────────────────────────┤
│ 3. 轮播高度绝对锁死与 Apple TV 双阶流体文字动效 (Rock-Solid Hero Carousel): │
│    - categoryHeroTextGroup 锁死为 124dp 恒定高度容器，支持 1~3 行丰富剧情描述  │
│    - “▶ 立即播放”按钮坐标 100% 绝对锚定，不同电影 2段/3段简介切换零像素跳动    │
│    - 轮播切片采用 150ms 进场 + 260ms 减速出场贝塞尔曲线，海报保留前图 CrossFade │
│    - 彻底告别生硬闪烁与突兀跳动                                                │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 关键架构设计与源码索引

### 3.1 核心控制器
- 顶栏药丸控制器: [`TopNavController.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/home/TopNavController.java)
- 分类频道控制器: [`CategoryViewController.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/home/CategoryViewController.java)
- 首页主控制器: [`HomeActivity.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java)
- 视差滚动协调器: [`ScrollCoordinator.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/main/java/com/fongmi/android/tv/utils/ScrollCoordinator.java)

### 3.2 布局与渲染
- 首页主布局: [`activity_home.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/layout/activity_home.xml)
- 分类频道布局: [`layout_category_channel.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/layout/layout_category_channel.xml)
- 硬件加速羽化容器: [`AlphaFadeFrameLayout.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/custom/AlphaFadeFrameLayout.java)
- 多光谱高斯毛玻璃引擎: [`FrostedGlassUtil.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/main/java/com/fongmi/android/tv/utils/FrostedGlassUtil.java)
