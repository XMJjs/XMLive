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
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

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
        handleTargetMovement(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onVehicleMove(VehicleMoveEvent event) {
        for (RecorderBinding binding : plugin.getLiveCore().getAllBindings()) {
            Player target = Bukkit.getPlayer(binding.getTargetUuid());
            if (target != null && target.isOnline() && target.isInsideVehicle()) {
                if (target.getVehicle() != null && target.getVehicle().equals(event.getVehicle())) {
                    handleTargetMovement(target);
                    break;
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

    private void handleTargetMovement(Player target) {
        for (RecorderBinding binding : plugin.getLiveCore().getAllBindings()) {
            if (binding.getTargetUuid().equals(target.getUniqueId())) {
                Player recorder = Bukkit.getPlayer(binding.getRecorderUuid());
                if (recorder == null || !recorder.isOnline()) {
                    continue;
                }

                int currentTick = Bukkit.getCurrentTick();
                int freq = plugin.getConfigManager().getUpdateFrequency();
                int lastTick = lastUpdateTick.getOrDefault(recorder.getUniqueId(), 0);
                if (currentTick - lastTick < freq) {
                    continue;
                }
                lastUpdateTick.put(recorder.getUniqueId(), currentTick);

                applySmoothCameraMovement(recorder, target);
                sendActionBarStatus(recorder, target);
            }
        }
    }

    private void applySmoothCameraMovement(Player recorder, Player target) {
        Location camLoc = calculateCameraLocation(target, recorder);
        Location recorderLoc = recorder.getLocation();

        Vector direction = camLoc.toVector().subtract(recorderLoc.toVector());

        if (direction.lengthSquared() < 0.01) {
            return;
        }

        double strength = plugin.getConfigManager().getVelocityStrength();
        recorder.setVelocity(direction.multiply(strength));

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