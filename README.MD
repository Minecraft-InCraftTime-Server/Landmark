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

## 📦 安装

1. 从 [Releases](https://github.com/MineSunshineOne/LandmarkSystem/releases) 下载最新版本
2. 将插件放入服务器的 `plugins` 文件夹
3. 重启服务器或使用 `/reload confirm` 重载插件
4. 插件将自动生成配置文件

## 🎮 使用方法

### 基础命令
- `/landmark menu` - 打开锚点菜单
- `/landmark create <名称> [描述]` - 创建新锚点
- `/landmark delete <名称>` - 删除锚点
- `/landmark edit <名称> <描述>` - 编辑锚点描述
- `/landmark rename <旧名称> <新名称>` - 重命名锚点
- `/landmark reload` - 重载配置

### 锚点解锁
1. 玩家靠近未解锁的锚点
2. 系统自动解锁并显示特效
3. 解锁后可以在菜单中查看并传送

### 传送方式
1. 通过 GUI 菜单选择目标锚点
2. 使用 `/landmark teleport <名称>` 命令
3. 右键点击锚点实体

## ⚙️ 配置

### 基础设置
```yaml
settings:
  cooldown-time: 120      # 传送冷却时间（秒）
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

## 🔒 权限

### 管理员权限
- `landmark.create` - 创建锚点
- `landmark.delete` - 删除锚点
- `landmark.edit` - 编辑锚点
- `landmark.rename` - 重命名锚点
- `landmark.reload` - 重载配置

### 玩家权限
- `landmark.teleport` - 传送权限 (默认: true)
- `landmark.menu` - 菜单权限 (默认: true)
- `landmark.interact` - 交互权限 (默认: true)

## 🌟 特色功能

### 魔法阵特效
- 五角星魔法阵
- 动态光柱
- 渐变色粒子
- 自定义粒子密度

### 安全机制
- 数据自动备份
- 异步数据处理
- 防崩溃保护
- 冷却时间控制

### 优化设计
- 异步传送
- 区域调度器支持
- 智能实体管理
- 高性能粒子系统

## 📝 开发计划

- [ ] 添加更多粒子特效
- [ ] 支持自定义传送动画
- [ ] 添加锚点组功能
- [ ] 支持更多交互方式
- [ ] 添加经济系统支持

## 🐛 问题反馈

如果您在使用过程中遇到任何问题，欢迎通过以下方式反馈：

1. 在 [GitHub Issues](https://github.com/MineSunshineOne/LandmarkSystem/issues) 提交问题
2. 加入我们的交流群：[QQ群号]

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源。

## 🙏 鸣谢

- [Paper](https://papermc.io/) - 高性能 Minecraft 服务端
- [Folia](https://github.com/PaperMC/Folia) - 多线程优化服务端
- [MiniMessage](https://docs.adventure.kyori.net/minimessage.html) - 文本格式化库
