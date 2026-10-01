# Apple TV 风格电视盒子项目交接文档 (Project Handover)

> **文档版本**: 2.0 (对应代码 Release `v1.0.80`)  
> **更新时间**: 2026-10-01  
> **适用场景**: 新对话无缝接续开发、团队协作交接、技术架构全景回顾

---

## 1. 项目基本信息

- **项目名称**: Apple TV 风格沉浸式电视盒子 (FongMi TVBox Leanback 重构版)
- **代码仓库**: `https://github.com/liujiawei-zuopin/apple-tvbox`
- **主要分支**: `main`
- **最新发布**: [Release v1.0.80](https://github.com/liujiawei-zuopin/apple-tvbox/releases)
- **包名与启动 Activity**: `com.fongmi.android.tv` / `com.fongmi.android.tv.ui.activity.HomeActivity`
- **模拟器/测试设备**: MuMu 模拟器 Android 12 (1080P TV 模式, `127.0.0.1:16384`)
- **本地 ADB 路径**: `D:\Program Files\Netease\MuMuPlayer\nx_device\15.0\shell\adb.exe`

---

## 2. 核心架构与 5 大系统级 UI 升级体系 (v2.0)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       顶部磨砂药丸导航栏 (Top Nav)                              │
│         [ 主页 ]  [ 电影 ]  [ 剧集 ]  [ 综艺 ]  [ 动漫/纪录片... ]  [ 🔍 ]        │
├─────────────────────────────────────────────────────────────────────────────┤
│ 1. 主页 (Home View):                                                        │
│    - 100vh 全屏海报剧照 + 调深暗夜渐变遮罩 (Darker Scrim)                       │
│    - “现在观看” 横版大卡片轮播                                                │
│    - “继续观看”、“正在热播” 6 列对齐货架                                       │
│    - ScrollCoordinator 联动：向下滚动时顶栏移出，背景深暗磨砂化                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ 2. 多光谱分类频道 (Category Channels - 电影 / 剧集 / 综艺 / 自定义频道):      │
│    - 12 款 Apple TV 风格多光谱高斯毛玻璃引擎 (FrostedGlassUtil)                │
│      · 语义优先匹配 (如"漫"->Sakura粉, "纪"->AuroraCyan青, "短剧"->CrimsonRuby) │
│      · 哈希确定性回退 (hashCode % 11)，保证任意未知新频道拥有专属沉浸背景       │
│    - 海报滚动区彻底移除暗色渐变遮罩挡光，纯净还原海报原生画质                   │
│    - 标题、简介与 Meta 纯白无阴影排版，处于黄金视觉中下平衡区 (Top 170dp)        │
│    - “▶ 立即播放” 纯白胶囊与指示点居中放置 (Margin 24dp)                       │
│    - 底部露出 40%~50% “推荐”栏横卡（沉浸露头排版）                              │
│    - 硬件加速 AlphaFadeFrameLayout (DST_IN) 底部 28% 羽化，与底层光斑完美交融   │
│    - 动态货架编排：根据接口动态生成自适应子分类货架与“全部XX”过滤网格          │
│    - 内存级预热缓存 (mCategoryCache)，Tab 切换零闪烁/零布局跳动                 │
├─────────────────────────────────────────────────────────────────────────────┤
│ 3. 统一视差滚动协调器 (ScrollCoordinator):                                   │
│    - 统一协调顶部药丸移出/复位与 Hero 标题渐隐，消除冗余计算                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ 4. 全局 Apple Design Tokens (dimens.xml & integers.xml):                    │
│    - 空间布局、圆角规格、动画时长全面 Token 化，支持未来一处修改全局生效       │
├─────────────────────────────────────────────────────────────────────────────┤
│ 5. 搜索页 (Embedded Search):                                                │
│    - 极简磨砂药丸容器，选中元素为白底黑字，标准中性深暗毛玻璃底图               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 5 大系统级 UI 架构升级实施明细

| 架构升级方向 | 核心设计理念 | 实施模块与关键代码 |
| :--- | :--- | :--- |
| **① 多光谱高斯毛玻璃主题引擎** | 为全网任意分类提供定制级 Apple TV 氛围背景，杜绝千篇一律 | [`FrostedGlassUtil.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/main/java/com/fongmi/android/tv/utils/FrostedGlassUtil.java)<br>内置 12 款深暗多光谱氛围光斑：Emerald, Sapphire, Amethyst, Sakura, AuroraCyan, FlameOrange, SunGold, VintageAmber, CrimsonRuby, CosmicViolet, CyberTeal, DeepSlate。<br>提供 `getFrostedForCategory(typeId, typeName)` 语义+哈希双重解析引擎。 |
| **② 全局 Apple Design Tokens 体系** | 将 UI 尺寸、间距、圆角、动效参数集中管理，便于后续精细化调优 | [`dimens.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/values/dimens.xml) & [`integers.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/values/integers.xml)<br>标准化 `hero_info_margin_top (170dp)`、`hero_section_height (476dp)`、`shelf_peeking_height (64dp)`、`corner_radius_pill (18dp)` 及 `anim_duration_focus (150ms)` 等。 |
| **③ 数据驱动型自适应货架编排** | 动态解析任意站点返回的分类、过滤器与内容，自动构建 Apple 风格货架 | [`TopNavController.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/home/TopNavController.java) & [`CategoryViewController.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/home/CategoryViewController.java)<br>顶栏动态接入动漫/纪录片/少儿等新频道；分类页动态解析子分类标签自动生成子流派货架与“全部XX”过滤矩阵。 |
| **④ 统一视差滚动与动效协调器** | 统一所有页面与频道的滚动联动逻辑，保持物理运动手感一致 | [`ScrollCoordinator.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/main/java/com/fongmi/android/tv/utils/ScrollCoordinator.java)<br>统一管理 `topBar` 的位移/渐隐、`heroInfo` 的淡出淡入以及平滑复位动画。 |
| **⑤ 跨频道秒开与零闪烁预热** | 内存级数据缓存与异步渲染，实现频道切换瞬时呈现 | [`HomeActivity.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java)<br>`mCategoryCache` 结构记录分类快照，Tab 切换优先内存直出，再后台网络静默增量刷新。 |

---

## 4. 关键架构设计与源码索引

### 4.1 核心控制器
- 顶栏药丸控制器: [`TopNavController.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/home/TopNavController.java)
- 分类频道控制器: [`CategoryViewController.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/home/CategoryViewController.java)
- 首页主控制器: [`HomeActivity.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java)
- 视差滚动协调器: [`ScrollCoordinator.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/main/java/com/fongmi/android/tv/utils/ScrollCoordinator.java)

### 4.2 渲染与动效组件
- 多光谱高斯毛玻璃引擎: [`FrostedGlassUtil.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/main/java/com/fongmi/android/tv/utils/FrostedGlassUtil.java)
- 硬件加速羽化容器: [`AlphaFadeFrameLayout.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/custom/AlphaFadeFrameLayout.java)
- 分类频道布局: [`layout_category_channel.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/layout/layout_category_channel.xml)
- Design Tokens 资源: [`dimens.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/values/dimens.xml) / [`integers.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/values/integers.xml)

---

## 5. 快速构建与发布流程

```bash
# 1. 提交与推送
git add .
git commit -m "feat(ui): implement multi-spectral theme engine, design tokens, and dynamic channel shelf orchestration (v1.0.80)"
git push origin main

# 2. 创建 GitHub Release Tag 触发 CI/CD
git tag -a v1.0.80 -m "Release v1.0.80: 5 Major UI Architecture Upgrades"
git push origin v1.0.80

# 3. 安装到 MuMu 模拟器验证
adb connect 127.0.0.1:16384
adb install -r <path-to-apk>
adb shell am start -n com.fongmi.android.tv/com.fongmi.android.tv.ui.activity.HomeActivity
```
