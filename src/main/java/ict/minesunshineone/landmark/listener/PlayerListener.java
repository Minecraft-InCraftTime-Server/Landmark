package ict.minesunshineone.landmark.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;

import ict.minesunshineone.landmark.LandmarkPlugin;
import ict.minesunshineone.landmark.gui.LandmarkMenu;
import ict.minesunshineone.landmark.model.Landmark;
import net.kyori.adventure.title.Title;

public class PlayerListener implements Listener {

    private final LandmarkPlugin plugin;
    private final Map<UUID, Long> lastCheckTimes = new HashMap<>();
    private static final long CHECK_INTERVAL = 500L; // 500ms检查间隔
    private final Map<UUID, Location> lastPlayerLocations = new HashMap<>();

    public PlayerListener(LandmarkPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClickMenu(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) {
            return;
        }

        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof LandmarkMenu menu)) {
            return;
        }

        menu.onClick(event.getSlot(), event);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        long currentTime = System.currentTimeMillis();

        // 限制检查频率
        if (currentTime - lastCheckTimes.getOrDefault(player.getUniqueId(), 0L) < CHECK_INTERVAL) {
            return;
        }

        lastCheckTimes.put(player.getUniqueId(), currentTime);
        checkLandmarkUnlock(player, event.getTo());
    }

    private void checkLandmarkUnlock(Player player, Location playerLoc) {
        // 如果玩家是观察模式，直接返回
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        for (Map.Entry<String, Landmark> entry : plugin.getLandmarkManager().getLandmarks().entrySet()) {
            String landmarkName = entry.getKey();
            Landmark landmark = entry.getValue();
            Location landmarkLoc = landmark.getLocation();

            // 使用 LandmarkManager 中的辅助方法检查距离
            if (!plugin.getLandmarkManager().isLandmarkUnlocked(player, landmarkName)
                    && plugin.getLandmarkManager().isPlayerNearLandmark(player, landmarkLoc)) {

                unlockLandmark(player, landmark);
            }

            // 优化粒子效果显示范围检查
            if (playerLoc.getWorld().equals(landmarkLoc.getWorld())
                    && playerLoc.distance(landmarkLoc) <= 20) {
                spawnLandmarkParticles(player, landmarkLoc);
            }
        }
    }

    private void unlockLandmark(Player player, Landmark landmark) {
        // 解锁锚点
        plugin.getLandmarkManager().unlockLandmark(player, landmark.getName());

        // 播放音效和显示消息
        playUnlockSound(player);
        showUnlockEffects(player, landmark);
    }

    private void showUnlockEffects(Player player, Landmark landmark) {
        // 只给解锁的玩家发送标题
        Title title = Title.title(
                plugin.getConfigManager().getMessage("unlock-title", ""),
                plugin.getConfigManager().getMessage("unlock-subtitle", "")
                        .replaceText(builder -> builder.match("<landmark_name>")
                        .replacement(landmark.getName()))
        );
        player.showTitle(title);

        // 只给解锁的玩家发送消息
        plugin.getConfigManager().sendMessage(player, "unlock-message",
                "<green>你已解锁锚点: <gold><landmark_name></gold></green>",
                "<landmark_name>", landmark.getName());
    }

    // 优化粒子效果显示
    private void spawnLandmarkParticles(Player player, Location landmarkLoc) {
        FileConfiguration config = plugin.getConfigManager().getConfig();
        double time = System.currentTimeMillis() / 1000.0;

        // 六芒星魔法阵
        if (config.getBoolean("particles.center.enabled", true)) {
            spawnHexagramParticles(player, landmarkLoc, config, time);
        }

        // 边界光柱
        if (config.getBoolean("particles.border.enabled", true)) {
            spawnBorderPillars(player, landmarkLoc, config);
        }
    }

    private void spawnHexagramParticles(Player player, Location center, FileConfiguration config, double time) {
        try {
            Particle particle = Particle.valueOf(config.getString("particles.center.type", "END_ROD"));
            double radius = config.getDouble("particles.center.star_radius", 1.5);
            double height = config.getDouble("particles.center.height", 0.1);
            double particleDensity = config.getDouble("particles.center.particle_density", 0.2);

            // 计算动画阶段
            double duration = config.getDouble("particles.center.animation.duration", 4.0);
            double fadeIn = config.getDouble("particles.center.animation.fade_in", 1.0);
            double stay = config.getDouble("particles.center.animation.stay", 2.0);
            double fadeOut = config.getDouble("particles.center.animation.fade_out", 1.0);

            double cycleTime = time % duration;
            double alpha;

            if (cycleTime < fadeIn) {
                // 渐入阶段
                alpha = cycleTime / fadeIn;
            } else if (cycleTime < fadeIn + stay) {
                // 停留阶段
                alpha = 1.0;
            } else if (cycleTime < duration) {
                // 渐出阶段
                alpha = (duration - cycleTime) / fadeOut;
            } else {
                alpha = 0.0;
            }

            // 制静态五角星
            double startAngle = -Math.PI / 2; // 保持五角星顶点朝上
            double[] angles = new double[5];
            for (int i = 0; i < 5; i++) {
                angles[i] = startAngle + (2 * Math.PI * i / 5);
            }

            int[] order = {0, 2, 4, 1, 3, 0};

            // 绘制五角星的线段
            for (int i = 0; i < order.length - 1; i++) {
                double x1 = center.getX() + Math.cos(angles[order[i]]) * radius;
                double z1 = center.getZ() + Math.sin(angles[order[i]]) * radius;
                double x2 = center.getX() + Math.cos(angles[order[i + 1]]) * radius;
                double z2 = center.getZ() + Math.sin(angles[order[i + 1]]) * radius;

                // 根据alpha值调整粒子密度
                double density = particleDensity * (alpha + 0.1); // 添加基础密度
                for (double j = 0; j <= 1; j += density) {
                    double x = x1 + (x2 - x1) * j;
                    double z = z1 + (z2 - z1) * j;
                    player.spawnParticle(particle, x, center.getY() + height, z, 1, 0, 0, 0, 0);
                }
            }

            // 绘制静态外圆
            double circleRadius = radius * 1.3;
            int points = (int) (20 * (alpha + 0.1)); // 添加基础点数
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI * i / points);
                double x = center.getX() + Math.cos(angle) * circleRadius;
                double z = center.getZ() + Math.sin(angle) * circleRadius;
                player.spawnParticle(particle, x, center.getY() + height * 0.5, z, 1, 0, 0, 0, 0);
            }

        } catch (IllegalArgumentException e) {
            plugin.getSLF4JLogger().warn("无效的粒子类型: {}", config.getString("particles.center.type"));
        }
    }

    private void spawnBorderPillars(Player player, Location center, FileConfiguration config) {
        try {
            Particle particle = Particle.valueOf(config.getString("particles.border.type", "SPELL_WITCH"));
            double height = config.getDouble("particles.border.height", 1.5);
            double density = config.getDouble("particles.border.density", 0.03);
            double spiralSpeed = config.getDouble("particles.border.spiral_speed", 1.0);
            double trailLength = config.getDouble("particles.border.trail_length", 1.5);
            int spiralCount = config.getInt("particles.border.spiral_count", 8);
            int headDensity = config.getInt("particles.border.head_density", 6);
            double spiralRadius = config.getDouble("particles.border.spiral_radius", 0.3);

            double time = System.currentTimeMillis() / 1000.0;
            double radius = plugin.getConfigManager().getUnlockRadius();

            for (int i = 0; i < spiralCount; i++) {
                double angleOffset = (2 * Math.PI * i / spiralCount);
                double heightOffset = (height / spiralCount) * i;

                double currentHeight = ((time * spiralSpeed) + heightOffset) % height;
                double baseAngle = time * spiralSpeed + angleOffset;

                // 使用正弦函数使螺旋更平滑
                double spiralAngle = baseAngle + Math.sin(currentHeight * Math.PI) * spiralRadius;
                double x = center.getX() + Math.cos(spiralAngle) * radius;
                double z = center.getZ() + Math.sin(spiralAngle) * radius;

                // 生成尾巴
                for (double t = 0; t < trailLength; t += density) {
                    double trailHeight = currentHeight - t;
                    if (trailHeight >= 0 && trailHeight < height) {
                        // 头部区域增加密度
                        if (t < density * 4) {
                            for (int h = 0; h < headDensity; h++) {
                                double spread = 0.03;
                                double offsetX = (Math.random() - 0.5) * spread;
                                double offsetZ = (Math.random() - 0.5) * spread;
                                player.spawnParticle(particle,
                                        x + offsetX,
                                        center.getY() + trailHeight,
                                        z + offsetZ,
                                        1, 0, 0, 0, 0);
                            }
                        } else {
                            // 尾部渐变消失
                            double fadeAlpha = Math.pow(1.0 - (t / trailLength), 1.5);
                            if (Math.random() < fadeAlpha) {
                                player.spawnParticle(particle,
                                        x,
                                        center.getY() + trailHeight,
                                        z,
                                        1, 0, 0, 0, 0);
                            }
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            plugin.getSLF4JLogger().warn("无效的粒子类型: {}", config.getString("particles.border.type"));
        }
    }

    private void playUnlockSound(Player player) {
        String soundName = plugin.getConfigManager().getUnlockSound().toString();
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(),
                    sound,
                    plugin.getConfigManager().getUnlockSoundVolume(),
                    plugin.getConfigManager().getUnlockSoundPitch());
        } catch (IllegalArgumentException e) {
            plugin.getSLF4JLogger().error("无效的声音设置: {}", soundName);
        }
    }

    public void cleanup() {
        lastPlayerLocations.clear();
        lastCheckTimes.clear();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 只监听右键空气
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player = event.getPlayer();

        // 检查玩家是否在任意锚点范围内
        for (Landmark landmark : plugin.getLandmarkManager().getLandmarks().values()) {
            if (plugin.getLandmarkManager().isPlayerNearLandmark(player, landmark.getLocation())) {
                // 在锚点范围内,打开菜单
                new LandmarkMenu(plugin, player).open();
                return;
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        lastPlayerLocations.remove(playerId);
        lastCheckTimes.remove(playerId);
    }
}
