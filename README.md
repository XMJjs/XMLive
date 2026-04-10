# XMLIVE - Paper 直播运镜插件

## 注意！本项目完全由AI编写！

[![MC Version](https://img.shields.io/badge/Minecraft-1.21%2B-brightgreen)](https://papermc.io)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

XMLIVE 是一款为 **Paper 1.21+** 服务器设计的专业第三人称运镜插件。它允许指定玩家（录制者）以平滑、无延迟的方式跟随其他玩家（目标），并提供多种镜头模式与丰富的自定义参数，是制作游戏直播、视频录制、服务器宣传片的理想工具。

## ✨ 主要特性

- **双镜头模式**：
  - **速度模式** (`velocity`)：利用 `setVelocity` 驱动录制者平滑移动，无需额外依赖。
  - **数据包模式** (`packet`)：基于 PacketEvents 直接控制客户端镜头，实现**零延迟、无惯性**的专业级运镜（需安装 PacketEvents 前置）。
- **实时无延迟跟随**：目标移动、传送、乘坐载具（矿车、船等）时镜头均能实时响应。
- **个人化镜头参数**：每位录制者可独立设置镜头距离 (`/xl camera distance`) 和俯角 (`/xl camera pitch`)。
- **自动模式**：可设定时间间隔，自动随机切换跟随目标，实现无人值守的直播运镜。
- **录制者状态保护**：自动将录制者设为旁观模式、隐身、无敌，并禁止打开背包、丢弃物品等干扰操作。
- **视觉反馈**：目标玩家发光高亮，ActionBar 实时显示当前状态，可选粒子效果指示镜头位置。
- **权限与认证**：支持基于令牌的录制者登录验证，管理员命令独立权限控制。

## 📥 安装要求

- **服务端**：Paper 1.21 或更高版本
- **Java**：21 或更高版本
- **前置插件**（可选）：
  - [PacketEvents](https://github.com/retrooper/packetevents/releases) （仅当需要使用“数据包模式”时必须安装）

## 🚀 快速部署

1. 从 [Releases](https://github.com/XMJjs/XMLive/releases) 页面下载最新版 `XMLIVE-x.x.x.jar`。
2. 将 JAR 文件放入服务器的 `plugins/` 文件夹。
3. 如需使用**数据包模式**，请一并下载 `packetevents-spigot-2.11.2.jar` 并放入 `plugins/` 文件夹。
4. 启动服务器，插件将自动生成配置文件 `plugins/XMLIVE/config.yml`。
5. 关闭服务器（或执行 `/xl reload`），编辑 `config.yml` 添加授权玩家及其令牌。
6. 重新启动服务器（或重载插件），完成部署。

## ⚙️ 配置说明 (`config.yml`)

```yaml
# 授权设置
auth:
  enabled: true
  players:
    - "cameraman1:your-secret-token-123"   # 格式：玩家名:专属令牌
    - "cameraman2:another-secret-token-456"

# 镜头默认参数（可被个人设置覆盖）
camera:
  distance: 4.0          # 镜头与目标的距离（格）
  pitch: 220.0           # 镜头俯角（度），220 度可实现背后上方视角
  smooth-factor: 0.3     # 数据包模式下的位置平滑系数（0~1，越小越平滑）
  rotation-smooth-factor: 0.5  # 数据包模式下的视角旋转平滑系数

# 自动模式
auto:
  default-interval: 30   # 默认切换间隔（秒）

# 更新设置
update:
  frequency: 1           # 每游戏刻更新次数（1=每秒20次，最流畅）

# 速度跟随模式参数
velocity:
  strength: 0.9          # 速度力度系数（0.6~1.2，越高跟随越紧致）

# 视觉反馈
visual:
  glowing: true          # 目标玩家发光
  particles: false       # 镜头位置粒子效果
```

## 📋 命令与权限

### 玩家命令（需权限 `xmlive.use`）

| 命令 | 说明 |
|------|------|
| `/xl login <令牌>` | 使用配置文件中预设的令牌登录系统 |
| `/xl camera distance <数值>` | 设置个人镜头距离 |
| `/xl camera pitch <数值>` | 设置个人镜头俯角 |
| `/xl toggle` | 切换录制状态（旁观/隐身/无敌） |
| `/xl mode <velocity\|packet>` | 切换镜头跟随模式 |

### 管理员命令（需权限 `xmlive.admin`）

| 命令 | 说明 |
|------|------|
| `/xl bind <录制者> <目标>` | 手动将录制者绑定到指定目标 |
| `/xl unbind <录制者>` | 解除录制者的绑定 |
| `/xl auto <录制者> [间隔秒]` | 为录制者开启自动切换目标模式 |
| `/xl reset` | 重置所有自动模式计时器，立即切换目标 |
| `/xl list` | 查看当前所有录制者状态 |
| `/xl reload` | 重载配置文件 |

### 权限节点

```yaml
permissions:
  xmlive.use:
    description: 允许登录并使用个人镜头调整和状态切换
    default: true       # 所有玩家默认拥有
  xmlive.admin:
    description: 管理员命令权限
    default: op         # 仅 OP 默认拥有
```

## 🎮 使用流程示例

1. **管理员配置**：在 `config.yml` 中添加录制者 `cameraman1:abc123`。
2. **录制者登录**：录制者 `cameraman1` 进入游戏，输入 `/xl login abc123`。
3. **管理员绑定**：管理员输入 `/xl bind cameraman1 Steve` 让录制者跟随 `Steve`。
4. **录制者调整**：`cameraman1` 输入 `/xl toggle` 进入录制状态，输入 `/xl mode packet` 切换到极致平滑的数据包模式，并用 `/xl camera distance 5` 拉远镜头。
5. **自动切换**：管理员输入 `/xl auto cameraman1 60`，录制者将每隔 60 秒自动换一个在线玩家跟随。

## ❓ 常见问题

**Q：为什么数据包模式提示不可用？**  
A：请确保服务器已安装 `packetevents-spigot-2.11.2.jar` 且名称完全匹配（全小写 `packetevents`）。插件会自动回退到速度模式以保证可用性。

**Q：镜头移动时感觉有“弹簧感”或延迟？**  
A：尝试将 `config.yml` 中的 `update.frequency` 设为 `1`，并将 `velocity.strength` 调高至 `1.0` 或 `1.2`。对于数据包模式，可将 `smooth-factor` 调高（如 `0.8`）以降低延迟感。

**Q：录制者无法打开背包怎么办？**  
A：这是插件对录制者的主动保护，防止录制画面被 GUI 遮挡。如需临时打开，请先使用 `/xl toggle` 退出录制状态。

**Q：支持跨世界跟随吗？**  
A：完全支持。当目标玩家切换世界时，录制者会瞬间传送至目标所在世界的对应镜头位置。

## 🛠️ 开发者信息

- **构建工具**：Gradle 9.x (Kotlin DSL)
- **API**：Paper API 1.21.x
- **可选依赖**：PacketEvents 2.11.2 (Spigot Module)
- **自动构建**：GitHub Actions 自动编译并上传 JAR 产物

如需自行构建，请执行：
```bash
git clone https://github.com/XMJjs/XMLive.git
cd XMLive
./gradlew build
```

构建产物位于 `build/libs/XMLIVE-1.0.0.jar`。

## 📜 开源协议

本项目基于 [MIT License](LICENSE) 开源，欢迎贡献代码或提出建议。

---

**Enjoy your professional live streaming with XMLIVE!**

