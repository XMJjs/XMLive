package com.xmjjs.xmlive;

import com.xmjjs.xmlive.auth.AuthManager;
import com.xmjjs.xmlive.commands.XLCommand;
import com.xmjjs.xmlive.config.ConfigManager;
import com.xmjjs.xmlive.core.LiveCore;
import com.xmjjs.xmlive.listeners.CameraFollowListener;
import com.xmjjs.xmlive.listeners.RecorderRestrictListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class XMLive extends JavaPlugin {

    private static XMLive instance;
    private ConfigManager configManager;
    private AuthManager authManager;
    private LiveCore liveCore;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configManager = new ConfigManager(this);
        authManager = new AuthManager(this);
        liveCore = new LiveCore(this);

        getServer().getPluginManager().registerEvents(new CameraFollowListener(this), this);
        getServer().getPluginManager().registerEvents(new RecorderRestrictListener(this), this);

        XLCommand xlCommand = new XLCommand(this);
        getCommand("xl").setExecutor(xlCommand);
        getCommand("xl").setTabCompleter(xlCommand);

        getLogger().info("XMLIVE 插件已成功启动！");
    }

    @Override
    public void onDisable() {
        if (liveCore != null) {
            liveCore.shutdown();
        }
        getLogger().info("XMLIVE 插件已关闭。");
    }

    public static XMLive getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public LiveCore getLiveCore() {
        return liveCore;
    }
}