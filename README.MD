# LandmarkSystem (锚点传送系统)

一个功能强大的 Minecraft 锚点传送系统插件，支持 Folia。

## 功能特点

- 🎯 创建和管理锚点
- 🔓 自动解锁系统
- ⚡ 快速传送
- 📱 直观的 GUI 界面
- 🎨 粒子特效
- 🔊 音效反馈
- 💾 数据持久化
- ⚙️ 高度可配置
- 🚀 支持 Folia

## 命令

- `/landmark create <名称> [描述]` - 创建锚点
- `/landmark delete <名称>` - 删除锚点
- `/landmark edit <名称> <新描述>` - 编辑锚点描述
- `/landmark rename <旧名称> <新名称>` - 重命名锚点
- `/landmark reload` - 重载配置
- `/landmark menu` - 打开锚点菜单

## 权限

### 管理员权限
- `landmark.create` - 允许创建锚点
- `landmark.delete` - 允许删除锚点
- `landmark.edit` - 允许编辑锚点描述
- `landmark.rename` - 允许重命名锚点
- `landmark.reload` - 允许重载配置

### 玩家权限
- `landmark.teleport` - 允许传送到已解锁的锚点 (默认: true)
- `landmark.menu` - 允许打开锚点菜单 (默认: true)
- `landmark.interact` - 允许与锚点展示实体交互 (默认: true)

### 权限使用说明
1. 管理员权限默认只对 OP 开放
2. 玩家权限默认对所有玩家开放
3. 可以通过权限插件修改权限设置
4. 建议在生产环境中仔细配置权限以确保安全

## 配置文件说明

### 基础设置 (settings)
####插件的主要配置项包括：

- 冷却时间
- 解锁半径
- 解锁音效
- GUI 设置
- 消息自定义

### 配置说明

1. **基础设置**
   - `cooldown-time`: 两次传送之间的冷却时间
   - `unlock-radius`: 玩家需要靠近锚点多近才能解锁
   - `unlock-sound`: 解锁时播放的音效

2. **GUI设置**
   - `size`: 菜单大小，必须是9的倍数（9、18、27、36、45、54）
   - `items`: 已解锁和未解锁锚点的显示样式
   - `lore`: 物品说明文本的格式

3. **粒子效果**
   - `type`: 粒子类型，支持所有 Bukkit 粒子类型
   - `height`: 粒子效果离地高度
   - `particle_density`: 粒子密度，数值越小越密集

4. **消息设置**
   - 支持 MiniMessage 格式
   - 支持渐变色文本
   - 支持变量替换
   - 支持时间格式化

### 配置示例

```yaml
settings:
# 传送冷却时间（秒）
cooldown-time: 120
# 锚点解锁范围（方块）
unlock-radius: 3
# 解锁声音设置
unlock-sound: BLOCK_ENCHANTMENT_TABLE_USE
unlock-sound-volume: 10.0
unlock-sound-pitch: 1.0
```


### 数据存储设置 (storage)

```yaml
storage:
# 数据文件名
filename: 'landmarks.yml'
```

### 消息设置 (messages)

```yaml
messages:
prefix: '<gradient:gold:yellow>[魔法锚点]</gradient> '
landmark-not-exist: '<gradient:red:dark_red>✧ 魔法师,这个锚点似乎并不存在于这个世界...</gradient>'
landmark-not-unlocked: '<gradient:red:dark_red>✧ 魔法师,你需要先解开这个锚点的封印才能使用它!</gradient>'
teleport-cooldown: '<gradient:red:dark_red>✧ 魔法师,传送魔法还在冷却中,需要等待 <time> 秒才能再次施法...</gradient>'
teleport-success: '<gradient:green:dark_green>✧ 魔法师成功施展传送术,抵达了锚点 <landmark>!</gradient>'
```


### GUI 设置 (gui)

```yaml
gui:
title: '<gradient:gold:yellow>✧ 魔法锚点传送之书 ✧</gradient>'
size: 27 # 菜单大小（必须是9的倍数）
# 物品说明文本
lore:
coordinates: '<gradient:aqua:blue>魔法坐标: X:%x% Y:%y% Z:%z%</gradient>'
description: '<gradient:green:aqua>魔法描述: %description%</gradient>'
click-teleport: '<gradient:gold:yellow>✧ 点击施展传送魔法 ✧</gradient>'
locked: '<gradient:red:dark_red>✧ 等待解封的魔法锚点 ✧</gradient>'
# 物品显示设置
items:
unlocked:
material: ENDER_EYE
name: '<gradient:gold:yellow>✧ %landmark_name% ✧</gradient>'
locked:
material: ENDER_PEARL
name: '<gradient:gray:dark_gray>✧ %landmark_name% ✧</gradient>'
```


### 粒子效果设置 (particles)

```yaml
particles:
  # 五角星魔法阵
  center:
    enabled: true
    type: END_ROD           # 粒子类型
    height: 0.1             # 粒子高度
    star_radius: 1.5        # 五角星半径
    circle_radius: 2.0      # 外圈半径
    particle_density: 0.3   # 粒子密度

```

## 安装

1. 下载最新版本的插件
2. 将 jar 文件放入服务器的 plugins 文件夹
3. 重启服务器或重载插件

## 使用方法

1. 管理员使用 `/landmark create` 创建锚点
2. 玩家靠近锚点时自动解锁
3. 玩家可以通过以下方式打开传送菜单：
   - 使用 `/landmark menu` 命令
   - 右键点击锚点实体
4. 在菜单中选择已解锁的锚点进行传送

## 技术特性

- 异步数据处理
- 高性能粒子系统
- 智能的区域调度器使用
- 完整的错误处理
- 自动数据备份

## 依赖

- Minecraft 1.21.4+
- Paper/Folia 服务端

## 开发者

- MineSunshineone

## 许可证

本项目采用 MIT 许可证