# 🎯 LandmarkSystem (魔法锚点系统)

一个优雅而强大的 Minecraft 锚点传送系统插件，支持 Paper/Folia 服务端。

[![](https://img.shields.io/badge/Minecraft-1.21.4-green.svg)](https://www.minecraft.net/)
[![](https://img.shields.io/badge/Paper/Folia-Latest-blue.svg)](https://papermc.io/)
[![](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## ✨ 特性

- 🌟 优雅的魔法主题界面
- 🎨 精美的粒子特效和音效
- 🔮 自动解锁系统
- ⚡ 快速传送功能
- 📱 直观的 GUI 菜单
- 💾 安全的数据存储
- ⚙️ 高度可配置
- 🚀 支持 Folia 多线程优化
- 🛡️ 完善的权限系统
- 🌐 多世界支持
- 🎯 自定义交互实体
- 🔄 内存优化管理

## 📦 安装要求

- Java 21 或更高版本
- Paper/Folia 1.21 或更高版本

## 📖 使用说明

详细的使用说明请查看 [使用指南](USAGE.md)。

## 🎮 基础命令

- `/landmark menu` - 打开锚点菜单
- `/landmark create <名称> [描述]` - 创建新锚点
  - 支持多行描述，每个参数将作为单独的一行
  - 例如：`/landmark create 主城 欢迎来到 主城中心 这里是传送大厅`
- `/landmark delete <名称>` - 删除锚点
- `/landmark edit <名称> <描述>` - 编辑锚点描述
  - 支持多行描述，每个参数将作为单独的一行
  - 例如：`/landmark edit 主城 欢迎来到 主城中心 这里是传送大厅`
- `/landmark rename <旧名称> <新名称>` - 重命名锚点
- `/landmark teleport <名称>` - 传送到指定锚点
- `/landmark reload` - 重载配置

## 🔒 权限系统

### 管理员权限
- `landmark.create` - 创建锚点
- `landmark.delete` - 删除锚点
- `landmark.edit` - 编辑锚点
- `landmark.rename` - 重命名锚点
- `landmark.reload` - 重载配置
- `landmark.admin` - 管理员权限组

### 玩家权限
- `landmark.teleport` - 传送权限 (默认: true)
- `landmark.menu` - 菜单权限 (默认: true)
- `landmark.interact` - 交互权限 (默认: true)

## ⚙️ 配置示例

### 基础设置
```yaml
settings:
  cooldown-time: 90      # 传送冷却时间（秒）
  unlock-radius: 3        # 解锁范围（方块）
  unlock-sound: BLOCK_ENCHANTMENT_TABLE_USE  # 解锁音效
```

### GUI 设置
```yaml
gui:
  title: '<gradient:gold:yellow>✧ 魔法锚点系统 ✦</gradient>'
  size: 45  # 菜单大小
  items:
    unlocked:
      material: AMETHYST_SHARD
      name: '<#c7a3ed><bold>%landmark_name%</bold>'
    locked:
      material: BARRIER
      name: '<gray><bold>%landmark_name%</bold>'
```

### 粒子效果
```yaml
particles:
  center:
    enabled: true
    type: END_ROD
    height: 0.1
    star_radius: 1.5
    circle_radius: 2.0
```

## 📦 安装

1. 从 [Releases](https://github.com/MineSunshineOne/Landmark/releases) 下载最新版本
2. 将插件放入服务器的 `plugins` 文件夹
3. 重启服务器或使用 `/reload confirm` 重载插件
4. 插件将自动生成配置文件

## 🔧 技术特性

- 基于 Folia API 开发，支持多线程优化
- 使用 Adventure API 实现富文本显示
- MiniMessage 格式支持
- 异步数据处理和存储
- 智能内存管理
- 自动清理系统

## 📝 开发计划

- [ ] 添加更多粒子特效
- [ ] 支持自定义传送动画
- [ ] 添加锚点组功能
- [ ] 支持更多交互方式
- [ ] 添加经济系统支持

## 🐛 问题反馈

如果您在使用过程中遇到任何问题，欢迎通过以下方式反馈：

1. 在 [GitHub Issues](https://github.com/MineSunshineOne/Landmark/issues) 提交问题

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源。

## 🙏 鸣谢

- [Paper](https://papermc.io/) - 高性能 Minecraft 服务端
- [Folia](https://github.com/PaperMC/Folia) - 多线程优化服务端
- [MiniMessage](https://docs.adventure.kyori.net/minimessage.html) - 文本格式化库
