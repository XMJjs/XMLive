// src/main/java/com/yourpackage/xmlive/listeners/CameraFollowListener.java
package com.yourpackage.xmllive.listeners;

import com.yourpackage.xmllive.XMLive;
import com.yourpackage.xmllive.core.RecorderBinding;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraFollowListener implements Listener {

    private final XMLive plugin;
    // 记录上一次处理移动的时间，用于实现自定义更新频率
    private final Map<UUID, Integer> lastUpdateTick = new ConcurrentHashMap<>();

    public CameraFollowListener(XMLive plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player target = event.getPlayer();
        // 检查是否有录制者以该玩家为目标
        for (RecorderBinding binding : plugin.getLiveCore().getAllBindings()) {
            if (binding.getTargetUuid().equals(target.getUniqueId())) {
                Player recorder = Bukkit.getPlayer(binding.getRecorderUuid());
                if (recorder != null && recorder.isOnline()) {
                    // 控制更新频率
                    int currentTick = Bukkit.getCurrentTick();
                    int freq = plugin.getConfigManager().getUpdateFrequency();
                    int lastTick = lastUpdateTick.getOrDefault(recorder.getUniqueId(), 0);
                    if (currentTick - lastTick < freq) {
                        continue;
                    }
                    lastUpdateTick.put(recorder.getUniqueId(), currentTick);
                    // 应用相机偏移并更新录制者位置
                    updateCameraPosition(recorder, target);
                    // 发送 ActionBar 状态
                    sendActionBarStatus(recorder, target);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player target = event.getPlayer();
        // 检查是否有录制者以该玩家为目标
        for (RecorderBinding binding : plugin.getLiveCore().getAllBindings()) {
            if (binding.getTargetUuid().equals(target.getUniqueId())) {
                Player recorder = Bukkit.getPlayer(binding.getRecorderUuid());
                if (recorder != null && recorder.isOnline()) {
                    // 使用 runTask 确保传送在事件完成后立即执行，避免视觉上的瞬移
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            // 立即重新计算镜头位置并传送录制者
                            Location camLoc = calculateCameraLocation(target);
                            recorder.teleport(camLoc);
                        }
                    }.runTask(plugin);
                }
            }
        }
    }

    private void updateCameraPosition(Player recorder, Player target) {
        Location camLoc = calculateCameraLocation(target);
        // 直接传送录制者到镜头位置，实现“无瞬移感”的实时跟随
        recorder.teleport(camLoc);

        // 可选：显示粒子效果
        if (plugin.getConfigManager().isParticlesEnabled()) {
            target.getWorld().spawnParticle(Particle.END_ROD, camLoc, 1, 0, 0, 0, 0);
        }
    }

    private Location calculateCameraLocation(Player target) {
        Location targetLoc = target.getLocation();
        // 使用 BigDecimal 进行高精度计算，避免浮点数精度问题
        BigDecimal yaw = BigDecimal.valueOf(targetLoc.getYaw());
        BigDecimal pitch = BigDecimal.valueOf(plugin.getConfigManager().getCameraPitch());
        BigDecimal distance = BigDecimal.valueOf(plugin.getConfigManager().getCameraDistance());

        // 角度转弧度
        double yawRad = Math.toRadians(yaw.doubleValue());
        double pitchRad = Math.toRadians(pitch.doubleValue());

        // 计算偏移向量
        // x = -sin(yaw) * cos(pitch) * distance
        // y = -sin(pitch) * distance
        // z = cos(yaw) * cos(pitch) * distance
        BigDecimal xOffset = BigDecimal.valueOf(-Math.sin(yawRad) * Math.cos(pitchRad))
                .multiply(distance).setScale(5, RoundingMode.HALF_UP);
        BigDecimal yOffset = BigDecimal.valueOf(-Math.sin(pitchRad))
                .multiply(distance).setScale(5, RoundingMode.HALF_UP);
        BigDecimal zOffset = BigDecimal.valueOf(Math.cos(yawRad) * Math.cos(pitchRad))
                .multiply(distance).setScale(5, RoundingMode.HALF_UP);

        // 计算最终位置
        Location camLoc = targetLoc.clone();
        camLoc.setX(targetLoc.getX() + xOffset.doubleValue());
        camLoc.setY(targetLoc.getY() + yOffset.doubleValue());
        camLoc.setZ(targetLoc.getZ() + zOffset.doubleValue());
        // 让录制者面向目标玩家
        camLoc.setDirection(targetLoc.toVector().subtract(camLoc.toVector()));

        return camLoc;
    }

    private void sendActionBarStatus(Player recorder, Player target) {
        RecorderBinding binding = plugin.getLiveCore().getBinding(recorder);
        if (binding == null) return;

        String mode = binding.isAutoMode() ? "自动模式 (" + binding.getInterval() + "s)" : "手动模式";
        Component message = Component.text()
                .append(Component.text("目标: ", NamedTextColor.GRAY))
                .append(Component.text(target.getName(), NamedTextColor.GREEN))
                .append(Component.text(" | 模式: ", NamedTextColor.GRAY))
                .append(Component.text(mode, NamedTextColor.YELLOW))
                .build();

        // 使用 Paper 的 Adventure API 发送 ActionBar
        recorder.sendActionBar(message);
    }
}
