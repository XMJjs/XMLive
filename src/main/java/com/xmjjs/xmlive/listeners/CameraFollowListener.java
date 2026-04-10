package com.xmjjs.xmlive.listeners;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerPositionAndLook;
import com.xmjjs.xmlive.XMLive;
import com.xmjjs.xmlive.core.RecorderBinding;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
                            if (binding.getCameraMode() == RecorderBinding.MODE_PACKET) {
                                // 传送时直接重置平滑状态，避免插值错乱
                                binding.setCurrentSmoothLocation(camLoc.clone());
                                binding.setCurrentSmoothYaw(camLoc.getYaw());
                                binding.setCurrentSmoothPitch(camLoc.getPitch());
                                sendPacketPosition(recorder, camLoc, camLoc.getYaw(), camLoc.getPitch());
                            } else {
                                recorder.teleportAsync(camLoc).thenAccept(result -> {
                                    if (!result) {
                                        plugin.getLogger().warning("录制者 " + recorder.getName() + " 传送失败");
                                    }
                                });
                            }
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

                if (binding.getCameraMode() == RecorderBinding.MODE_PACKET) {
                    applyPacketCameraMovement(recorder, target, binding);
                } else {
                    applySmoothCameraMovement(recorder, target);
                }
                sendActionBarStatus(recorder, target);
            }
        }
    }

    private void applySmoothCameraMovement(Player recorder, Player target) {
        Location camLoc = calculateCameraLocation(target, recorder);
        Location recorderLoc = recorder.getLocation();

        float targetYaw = camLoc.getYaw();
        float targetPitch = camLoc.getPitch();

        float yawDiff = Math.abs(targetYaw - recorderLoc.getYaw());
        float pitchDiff = Math.abs(targetPitch - recorderLoc.getPitch());
        if (yawDiff > 1.0f || pitchDiff > 1.0f) {
            recorder.setRotation(targetYaw, targetPitch);
        }

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

    private void applyPacketCameraMovement(Player recorder, Player target, RecorderBinding binding) {
        // 检查 PacketEvents 是否已成功加载
        try {
            if (!PacketEvents.getAPI().isLoaded()) {
                recorder.sendMessage(Component.text("数据包模式不可用：PacketEvents 未加载，已自动回退到速度模式。", NamedTextColor.RED));
                binding.setCameraMode(RecorderBinding.MODE_VELOCITY);
                applySmoothCameraMovement(recorder, target);
                return;
            }
        } catch (Exception e) {
            recorder.sendMessage(Component.text("数据包模式不可用：PacketEvents 状态异常，已自动回退到速度模式。", NamedTextColor.RED));
            binding.setCameraMode(RecorderBinding.MODE_VELOCITY);
            applySmoothCameraMovement(recorder, target);
            return;
        }

        if (recorder.getGameMode() != GameMode.SPECTATOR) {
            recorder.setGameMode(GameMode.SPECTATOR);
        }

        // 获取配置的平滑因子
        double smoothFactor = plugin.getConfigManager().getCameraSmoothFactor();
        double rotationSmoothFactor = plugin.getConfigManager().getCameraRotationSmoothFactor();

        // 计算理想镜头位置
        Location idealCamLoc = calculateCameraLocation(target, recorder);

        // --- 位置平滑处理 ---
        Location currentSmoothLoc = binding.getCurrentSmoothLocation();
        if (currentSmoothLoc == null) {
            currentSmoothLoc = idealCamLoc.clone();
            binding.setCurrentSmoothLocation(currentSmoothLoc);
        } else {
            // 确保世界一致，避免跨世界时插值出错
            if (!currentSmoothLoc.getWorld().equals(idealCamLoc.getWorld())) {
                currentSmoothLoc = idealCamLoc.clone();
            } else {
                // 线性插值：当前位置 + (目标位置 - 当前位置) * 平滑因子
                currentSmoothLoc.setX(currentSmoothLoc.getX() + (idealCamLoc.getX() - currentSmoothLoc.getX()) * smoothFactor);
                currentSmoothLoc.setY(currentSmoothLoc.getY() + (idealCamLoc.getY() - currentSmoothLoc.getY()) * smoothFactor);
                currentSmoothLoc.setZ(currentSmoothLoc.getZ() + (idealCamLoc.getZ() - currentSmoothLoc.getZ()) * smoothFactor);
            }
            binding.setCurrentSmoothLocation(currentSmoothLoc);
        }

        // --- 视角平滑处理 ---
        float currentSmoothYaw = binding.getCurrentSmoothYaw();
        float currentSmoothPitch = binding.getCurrentSmoothPitch();
        float idealYaw = idealCamLoc.getYaw();
        float idealPitch = idealCamLoc.getPitch();

        // 如果尚未初始化，则直接使用理想值
        if (binding.getCurrentSmoothLocation() == null) {
            currentSmoothYaw = idealYaw;
            currentSmoothPitch = idealPitch;
        } else {
            // 处理 Yaw 的跨越 -180/180 边界问题
            float yawDiff = idealYaw - currentSmoothYaw;
            if (yawDiff > 180) yawDiff -= 360;
            if (yawDiff < -180) yawDiff += 360;
            currentSmoothYaw += yawDiff * rotationSmoothFactor;
            // 归一化到 [-180, 180)
            if (currentSmoothYaw >= 180) currentSmoothYaw -= 360;
            if (currentSmoothYaw < -180) currentSmoothYaw += 360;

            // Pitch 不需要特殊处理，范围在 -90 到 90
            currentSmoothPitch += (idealPitch - currentSmoothPitch) * rotationSmoothFactor;
        }

        binding.setCurrentSmoothYaw(currentSmoothYaw);
        binding.setCurrentSmoothPitch(currentSmoothPitch);

        // 发送平滑后的位置和视角
        sendPacketPosition(recorder, currentSmoothLoc, currentSmoothYaw, currentSmoothPitch);

        if (plugin.getConfigManager().isParticlesEnabled()) {
            target.getWorld().spawnParticle(Particle.END_ROD, currentSmoothLoc, 1, 0, 0, 0, 0);
        }
    }

    private void sendPacketPosition(Player player, Location location, float yaw, float pitch) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        if (user == null) return;

        WrapperPlayServerPlayerPositionAndLook packet = new WrapperPlayServerPlayerPositionAndLook(
                new Vector3d(location.getX(), location.getY(), location.getZ()),
                yaw,
                pitch,
                (byte) 0x00,
                0,
                false
        );
        user.sendPacket(packet);
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
        String camMode = binding.getCameraMode() == RecorderBinding.MODE_VELOCITY ? "速度" : "数据包";
        Component message = Component.text()
                .append(Component.text("目标: ", NamedTextColor.GRAY))
                .append(Component.text(target.getName(), NamedTextColor.GREEN))
                .append(Component.text(" | 模式: ", NamedTextColor.GRAY))
                .append(Component.text(mode, NamedTextColor.YELLOW))
                .append(Component.text(" | 镜头: ", NamedTextColor.GRAY))
                .append(Component.text(camMode, NamedTextColor.LIGHT_PURPLE))
                .build();
        recorder.sendActionBar(message);
    }
}
