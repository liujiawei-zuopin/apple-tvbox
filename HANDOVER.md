# Apple TV 风格电视盒子项目交接文档 (Project Handover)

> **文档版本**: 1.0 (对应代码 Release `v1.0.65` / Commit `a241335`)  
> **更新时间**: 2026-09-28  
> **适用场景**: 新对话无缝接续开发、团队协作交接、技术架构回顾

---

## 1. 项目基本信息

- **项目名称**: Apple TV 风格沉浸式电视盒子 (FongMi TVBox Leanback 重构版)
- **代码仓库**: `https://github.com/liujiawei-zuopin/apple-tvbox`
- **主要分支**: `main`
- **最新发布**: [Release v1.0.65](https://github.com/liujiawei-zuopin/apple-tvbox/releases)
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
│    - 界面向下滚动时，背景自动切换为海报多重高斯模糊层               │
├─────────────────────────────────────────────────────────────────┤
│ 2. 分类页 (电影 / 剧集 / 综艺):                                 │
│    - 100vh 全屏沉浸海报轮播，文字位于中下部黄金视觉区               │
│    - “▶ 立即播放” 白底黑字按钮 + 轮播进度点                     │
│    - 底部露出半截（40%~50%）“推荐”栏卡片（动态计算高度）           │
│    - 按 ⬇️ 平滑展开“推荐”栏，按 ⬆️ 平滑回滚至全屏海报              │
│    - “动作”、“爱情”等子分类卡片与“推荐”严格 6 列右端对齐           │
│    - 页面底部承接“全部影片”瀑布流                                │
├─────────────────────────────────────────────────────────────────┤
│ 3. 专属暗色高斯毛玻璃背景 (FrostedGlassUtil):                     │
│    - 电影：深祖母绿真毛玻璃 (#08160F + #00E599 光核)              │
│    - 剧集：深蓝宝石真毛玻璃 (#070E1A + #0A84FF 光核)              │
│    - 综艺：深紫水晶真毛玻璃 (#120717 + #BF5AF2 光核)              │
│    - 明确原则：严禁用海报直接当分类背景，滑动时深邃稳定不闪烁       │
├─────────────────────────────────────────────────────────────────┤
│ 4. 搜索页 (Search):                                             │
│    - 极简磨砂药丸容器，选中元素为白底黑字                          │
│    - 焦点可顺畅返回顶部药丸，彻底解决焦点陷阱锁定问题               │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 关键架构设计与源码对照

### 3.1 顶部药丸导航系统 (`TopNavController`)
- **源码文件**:
  - 控制器: [`TopNavController.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/home/TopNavController.java)
  - 药丸背景: [`bg_top_capsule_bar.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/drawable/bg_top_capsule_bar.xml)
  - 激活项背景: [`bg_top_nav_pill_active.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/drawable/bg_top_nav_pill_active.xml)
- **技术要点**:
  - **严格无描边 (No Stroke)**: 药丸底色采用 `#66FFFFFF` $\to$ `#44FFFFFF` 的纯高漫透光白渐变，彻底移除任何白色细描边。
  - **离开药丸保持高亮**: 光标切入下方货架或播放按钮时，当前选中的 Tab 仍维持纯白圆角胶囊底色与黑字。
  - **防闪烁与焦点流转**: 向上回滚时先平滑重置滚动条坐标再转移焦点，避免快速回跳导致的白边或跳帧。

### 3.2 分类频道控制器与全屏海报 (`CategoryViewController`)
- **源码文件**:
  - 控制器: [`CategoryViewController.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/home/CategoryViewController.java)
  - 布局文件: [`layout_category_channel.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/layout/layout_category_channel.xml)
  - 海报渐变过渡: [`gradient_category_hero_mask.xml`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/res/drawable/gradient_category_hero_mask.xml)
- **技术要点**:
  - **全屏沉浸重构**: 海报容器从原先被裁剪的 320dp 修复为全屏铺满（`100vh`）。
  - **半截露卡计算公式**:
    ```java
    // 动态根据屏幕总高减去 64dp (标题20dp + 间距4dp + 卡片半截40dp)
    int targetHeroH = containerH - ResUtil.dp2px(64);
    heroContainer.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, targetHeroH));
    ```
  - **双向平滑滚动控制**:
    - 在“▶ 立即播放”按 `KEYCODE_DPAD_DOWN`: 调用 `smoothScrollTo(0, heroHeight - 120)`，并聚焦推荐首张卡片；
    - 在“推荐”按 `KEYCODE_DPAD_UP`: 调用 `smoothScrollTo(0, 0)`，聚焦回“▶ 立即播放”。
  - **货架网格对齐**: 卡片宽度与左右间距通过屏幕可用宽度均分计算，确保“动作”、“爱情”等子分类卡片与“推荐”栏在屏幕右边缘严格对齐。

### 3.3 真实高斯磨砂毛玻璃引擎 (`FrostedGlassUtil`)
- **源码文件**:
  - 工具类: [`FrostedGlassUtil.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/main/java/com/fongmi/android/tv/utils/FrostedGlassUtil.java)
  - 模糊底层算法: [`BlurUtil.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/main/java/com/fongmi/android/tv/utils/BlurUtil.java)
  - 宿主绑定: [`HomeActivity.java`](file:///c:/Users/liuji/Documents/antigravity/sharp-brahmagupta/apple-tvbox/app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java)
- **技术要点**:
  - **根治“假透明底”**: Android 原生 View 的 Alpha 值只是颜色混合，没有模糊卷积。`FrostedGlassUtil` 通过 `RadialGradient` 在 Canvas 上绘制多个有机发光球体，结合 `BlurUtil.fastBlur(bitmap, 26, true)` 生成内存真实毛玻璃位图：
    - **电影 (Emerald)**: `#08160F` 底色 + `#00E599` 光斑
    - **剧集 (Sapphire)**: `#070E1A` 底色 + `#0A84FF` 光斑
    - **综艺 (Amethyst)**: `#120717` 底色 + `#BF5AF2` 光斑
  - **异步与缓存机制**: 生成的毛玻璃 Drawable 缓存在 `ConcurrentHashMap`，切换 Tab 时零延迟直接复用，不产生 GC 卡顿。

---

## 4. 关键设计约束与防踩坑守则

> [!IMPORTANT]
> **后续开发务必严格遵守以下约束：**
> 1. **严禁在磨砂玻璃上添加描边 (Stroke)**：无论是顶部胶囊药丸还是磨砂卡片，均不得设置白色或浅色边框线（`no stroke`）。
> 2. **分类页不得使用海报作为背景**：电影/剧集/综艺的背景必须为 `FrostedGlassUtil` 生成的深绿/深蓝/深紫高斯毛玻璃，滑动时背景不可随海报剧烈变色。
> 3. **分类页首屏海报必须 100vh 全屏沉浸**：严禁将其包在固定高度（如 300dp/320dp）的局部容器中。
> 4. **首屏必须露半截推荐卡片**：高度必须经由 `adjustHeroLayout()` 动态算得，保持 40%~50% 露卡视觉引导。
> 5. **遥控器按键导航闭查**：按键上下移动焦点时，必须严格处理边界聚焦，不得出现焦点掉入不可见区域或死锁。

---

## 5. 自动化构建与实机验证流水线

项目包含高度自动化的 CI/CD 与实机回归测试脚本：

1. **自动构建流水线**:
   - 推送代码到 GitHub `main` 分支触发 GitHub Actions 编译构建；
   - 编译产物自动发布为 GitHub Release（最新为 `v1.0.65`）。
2. **自动化测试脚本 (`run_v64_pipeline.py`)**:
   - 自动轮询 GitHub Actions 直至构建成功；
   - 自动下载最新 APK 并推送到 MuMu 模拟器无损覆盖安装；
   - 模拟遥控器（DPAD 键位）完整走测：主页 $\to$ 电影 $\to$ 立即播放 $\to$ 推荐 $\to$ 动作/爱情 $\to$ 剧集 $\to$ 综艺 $\to$ 搜索 $\to$ 返回主页；
   - 截取实机高清无损图片保存到 Artifact 目录进行视觉审查。

---

## 6. 后续开发建议与待办列表 (Backlog)

1. **真实数据源对接**:
   - 目前分类页中的海报轮播、推荐栏、子分类（动作、爱情等）已具备完整的动态布局架构，接下来可进一步对接视频源站的 API / JSON 配置，实现子分类与全部影片的真实分页加载；
2. **卡片聚焦动效打磨**:
   - 可进一步对选中的卡片增加 Apple TV 风格的轻微浮起缩放（`scaleX/Y = 1.06`）与微投影；
3. **真实遥控器物理按键适配**:
   - 在真实电视机顶盒上验证遥控器菜单键（Menu）直接唤出顶栏胶囊的高级快捷交互。

---

## 7. 新对话开篇提示词 (Prompt Template)

在新对话中，你可以直接复制并发送以下内容快速建立上下文：

```markdown
我们正在进行 Apple TV 风格电视盒子（com.fongmi.android.tv）的前端重构工作。
项目最新版本已演进至 Release v1.0.65 (Commit: a241335)。
关键已实现架构：
1. 顶部无描边磨砂胶囊导航栏（TopNavController，离焦维持高亮，搜索无锁死）；
2. 分类页（电影/剧集/综艺）100vh 全屏海报轮播 + 底部半截露卡推荐栏 + 白底黑字“立即播放”；
3. 电影（深祖母绿）、剧集（深蓝宝石）、综艺（深紫水晶）专属高斯毛玻璃环境光引擎（FrostedGlassUtil）；
4. 推荐与动作/爱情等子分类卡片全右端对齐。

请阅读交接文档后，继续协助我进行下一阶段的开发：[填写你的下一步需求]
```
