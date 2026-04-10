package com.xmjjs.xmlive.config;

import com.xmjjs.xmlive.XMLive;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final XMLive plugin;
    private FileConfiguration config;

    public ConfigManager(XMLive plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public boolean isAuthEnabled() {
        return config.getBoolean("auth.enabled", true);
    }

    public List<String> getAuthPlayers() {
        return config.getStringList("auth.players");
    }

    public Map<String, String> getAuthMap() {
        Map<String, String> map = new HashMap<>();
        for (String entry : getAuthPlayers()) {
            String[] parts = entry.split(":");
            if (parts.length == 2) {
                map.put(parts[0].trim(), parts[1].trim());
            }
        }
        return map;
    }

    public double getCameraDistance() {
        return config.getDouble("camera.distance", 3.0);
    }

    public double getCameraPitch() {
        return config.getDouble("camera.pitch", 45.0);
    }

    public int getDefaultInterval() {
        return config.getInt("auto.default-interval", 30);
    }

    public int getUpdateFrequency() {
        return config.getInt("update.frequency", 2);
    }

    // ⭐ 确保这一行存在
    public double getVelocityStrength() {
        return config.getDouble("velocity.strength", 0.9);
    }

    public boolean isGlowingEnabled() {
        return config.getBoolean("visual.glowing", true);
    }

    public boolean isParticlesEnabled() {
        return config.getBoolean("visual.particles", false);
    }
}
