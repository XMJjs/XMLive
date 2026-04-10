package com.xmjjs.xmlive.core;

import com.xmjjs.xmlive.XMLive;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LiveCore {

    private final XMLive plugin;
    private final Map<UUID, RecorderBinding> bindings = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> autoTasks = new ConcurrentHashMap<>();

    public LiveCore(XMLive plugin) {
        this.plugin = plugin;
    }

    public void bind(Player recorder, Player target) {
        bindings.put(recorder.getUniqueId(), new RecorderBinding(recorder.getUniqueId(), target.getUniqueId(), false, 0));
    }

    public void unbind(Player recorder) {
        recorder.setVelocity(new Vector(0, 0, 0));
        bindings.remove(recorder.getUniqueId());
        cancelAutoTask(recorder.getUniqueId());
    }

    public RecorderBinding getBinding(Player recorder) {
        return bindings.get(recorder.getUniqueId());
    }

    public Player getTarget(Player recorder) {
        RecorderBinding binding = bindings.get(recorder.getUniqueId());
        return binding != null ? Bukkit.getPlayer(binding.getTargetUuid()) : null;
    }

    public Collection<RecorderBinding> getAllBindings() {
        return bindings.values();
    }

    public void setAutoMode(Player recorder, int intervalSeconds) {
        RecorderBinding binding = bindings.get(recorder.getUniqueId());
        if (binding != null) {
            binding.setAutoMode(true);
            binding.setInterval(intervalSeconds);
            startAutoTask(recorder.getUniqueId(), intervalSeconds);
        }
    }

    public void disableAutoMode(Player recorder) {
        RecorderBinding binding = bindings.get(recorder.getUniqueId());
        if (binding != null) {
            binding.setAutoMode(false);
            cancelAutoTask(recorder.getUniqueId());
        }
    }

    private void startAutoTask(UUID recorderUuid, int intervalSeconds) {
        cancelAutoTask(recorderUuid);
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                Player recorder = Bukkit.getPlayer(recorderUuid);
                if (recorder == null || !recorder.isOnline()) {
                    cancel();
                    return;
                }
                RecorderBinding binding = bindings.get(recorderUuid);
                if (binding == null || !binding.isAutoMode()) {
                    cancel();
                    return;
                }
                List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
                onlinePlayers.removeIf(p -> p.getUniqueId().equals(recorderUuid));
                if (!onlinePlayers.isEmpty()) {
                    Player newTarget = onlinePlayers.get(new Random().nextInt(onlinePlayers.size()));
                    binding.setTargetUuid(newTarget.getUniqueId());
                    if (plugin.getConfigManager().isGlowingEnabled()) {
                        onlinePlayers.forEach(p -> p.setGlowing(false));
                        newTarget.setGlowing(true);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L * intervalSeconds, 20L * intervalSeconds);
        autoTasks.put(recorderUuid, task);
    }

    private void cancelAutoTask(UUID recorderUuid) {
        BukkitTask task = autoTasks.remove(recorderUuid);
        if (task != null) {
            task.cancel();
        }
    }

    public void shutdown() {
        autoTasks.values().forEach(BukkitTask::cancel);
        autoTasks.clear();
        bindings.clear();
    }
}
