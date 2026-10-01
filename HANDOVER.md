# Apple TV 风格电视盒子项目交接文档 (Project Handover)

> **文档版本**: 2.1 (对应代码 Release `v1.0.82`)  
> **更新时间**: 2026-10-01  
> **适用场景**: 新对话无缝接续开发、团队协作交接、技术架构全景回顾

---

## 1. 项目基本信息

- **项目名称**: Apple TV 风格沉浸式电视盒子 (FongMi TVBox Leanback 重构版)
- **代码仓库**: `https://github.com/liujiawei-zuopin/apple-tvbox`
- **主要分支**: `main`
- **最新发布**: [Release v1.0.82](https://github.com/liujiawei-zuopin/apple-tvbox/releases)
- **包名与启动 Activity**: `com.fongmi.android.tv` / `com.fongmi.android.tv.ui.activity.HomeActivity`
- **模拟器/测试设备**: MuMu 模拟器 Android 12/15 (1080P TV 模式, `127.0.0.1:16384`)
- **本地 ADB 路径**: `D:\Program Files\Netease\MuMuPlayer\nx_device\15.0\shell\adb.exe`

---

## 2. 核心架构与 3 大精度交互优化 (v2.1)

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
│ 2. 彻底解决海报全屏闪烁 (True 100vw Zero-Flash Layout):                     │
│    - categoryContainer 提升为 progressLayout 直属全宽子容器 (100vw)          │
│    - 彻底废除负边距 (-44dp margin hack)，分类海报在首帧测量即 100% 满屏渲染    │
│    - 彻底杜绝“先露白边/未填满、下一帧突然撑满全屏”的缩放闪现问题               │
├─────────────────────────────────────────────────────────────────────────────┤
│ 3. 轮播高度防抖与 Apple TV 流体文字过渡 (Anti-Jumping Hero Motion):          │
│    - categoryHeroDesc 约束固定 2 行标准高度 (lines=2, minLines=2, maxLines=2)│
│    - 无论不同影片简介长短/有无，简介文字区域高度恒定不变                      │
│    - 彻底消除轮播切片时“▶ 立即播放”胶囊按钮上下剧烈抽搐跳动的问题              │
│    - 配合 14dp 方向性位移 + 190ms Apple 级 CrossFade 淡入淡出，动效丝滑自然    │
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
