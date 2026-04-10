package com.xmjjs.xmlive.listeners;

import com.xmjjs.xmlive.XMLive;
import com.xmjjs.xmlive.core.RecorderBinding;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CameraFollowListener implements Listener {

    private final XMLive plugin;
    private final Map<UUID, Integer> lastUpdateTick = new ConcurrentHashMap<>();

    public CameraFollowListener(XMLive plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player target = event.getPlayer();
        for (RecorderBinding binding : plugin.getLiveCore().getAllBindings()) {
            if (binding.getTargetUuid().equals(target.getUniqueId())) {
                Player recorder = Bukkit.getPlayer(binding.getRecorderUuid());
                if (recorder != null && recorder.isOnline()) {
                    int currentTick = Bukkit.getCurrentTick();
                    int freq = plugin.getConfigManager().getUpdateFrequency();
                    int lastTick = lastUpdateTick.getOrDefault(recorder.getUniqueId(), 0);
                    if (currentTick - lastTick < freq) {
                        continue;
                    }
                    lastUpdateTick.put(recorder.getUniqueId(), currentTick);
                    updateCameraPosition(recorder, target);
                    sendActionBarStatus(recorder, target);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player target = event.getPlayer();
        for (RecorderBinding binding : plugin.getLiveCore().getAllBindings()) {
            if (binding.getTargetUuid().equals(target.getUniqueId())) {
                Player recorder = Bukkit.getPlayer(binding.getRecorderUuid());
                if (recorder != null && recorder.isOnline()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            Location camLoc = calculateCameraLocation(target, recorder);
                            recorder.teleportAsync(camLoc).thenAccept(result -> {
                                if (!result) {
                                    plugin.getLogger().warning("录制者 " + recorder.getName() + " 传送失败");
                                }
                            });
                        }
                    }.runTask(plugin);
                }
            }
        }
    }

    private void updateCameraPosition(Player recorder, Player target) {
        Location camLoc = calculateCameraLocation(target, recorder);
        recorder.teleportAsync(camLoc).thenAccept(result -> {
            if (!result) {
                plugin.getLogger().warning("录制者 " + recorder.getName() + " 传送失败");
            }
        });
        if (plugin.getConfigManager().isParticlesEnabled()) {
            target.getWorld().spawnParticle(Particle.END_ROD, camLoc, 1, 0, 0, 0, 0);
        }
    }

    private Location calculateCameraLocation(Player target, Player recorder) {
        RecorderBinding binding = plugin.getLiveCore().getBinding(recorder);
        double distance = binding != null ?
                binding.getEffectiveDistance(plugin.getConfigManager().getCameraDistance()) :
                plugin.getConfigManager().getCameraDistance();
        double pitch = binding != null ?
                binding.getEffectivePitch(plugin.getConfigManager().getCameraPitch()) :
                plugin.getConfigManager().getCameraPitch();

        Location targetLoc = target.getLocation();
        BigDecimal yaw = BigDecimal.valueOf(targetLoc.getYaw());
        BigDecimal pitchBd = BigDecimal.valueOf(pitch);
        BigDecimal distBd = BigDecimal.valueOf(distance);

        double yawRad = Math.toRadians(yaw.doubleValue());
        double pitchRad = Math.toRadians(pitchBd.doubleValue());

        BigDecimal xOffset = BigDecimal.valueOf(-Math.sin(yawRad) * Math.cos(pitchRad))
                .multiply(distBd).setScale(5, RoundingMode.HALF_UP);
        BigDecimal yOffset = BigDecimal.valueOf(-Math.sin(pitchRad))
                .multiply(distBd).setScale(5, RoundingMode.HALF_UP);
        BigDecimal zOffset = BigDecimal.valueOf(Math.cos(yawRad) * Math.cos(pitchRad))
                .multiply(distBd).setScale(5, RoundingMode.HALF_UP);

        Location camLoc = targetLoc.clone();
        camLoc.setX(targetLoc.getX() + xOffset.doubleValue());
        camLoc.setY(targetLoc.getY() + yOffset.doubleValue());
        camLoc.setZ(targetLoc.getZ() + zOffset.doubleValue());
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
        recorder.sendActionBar(message);
    }
}
