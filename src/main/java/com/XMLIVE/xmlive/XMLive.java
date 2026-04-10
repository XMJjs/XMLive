// src/main/java/com/yourpackage/xmlive/XMLive.java
package com.yourpackage.xmllive;

import com.yourpackage.xmllive.auth.AuthManager;
import com.yourpackage.xmllive.commands.XLCommand;
import com.yourpackage.xmllive.config.ConfigManager;
import com.yourpackage.xmllive.core.LiveCore;
import com.yourpackage.xmllive.listeners.CameraFollowListener;
import com.yourpackage.xmllive.listeners.RecorderRestrictListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class XMLive extends JavaPlugin {

    private static XMLive instance;
    private ConfigManager configManager;
    private AuthManager authManager;
    private LiveCore liveCore;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 保存默认配置文件
        saveDefaultConfig();

        // 2. 初始化管理器
        configManager = new ConfigManager(this);
        authManager = new AuthManager(this);
        liveCore = new LiveCore(this);

        // 3. 注册事件监听器
        getServer().getPluginManager().registerEvents(new CameraFollowListener(this), this);
        getServer().getPluginManager().registerEvents(new RecorderRestrictListener(this), this);

        // 4. 注册命令
        getCommand("xl").setExecutor(new XLCommand(this));
        // 在 XMLive.java 的 onEnable() 方法中添加
		XLCommand xlCommand = new XLCommand(this);
		getCommand("xl").setExecutor(xlCommand);
		getCommand("xl").setTabCompleter(xlCommand);

        getLogger().info("XMLIVE 插件已成功启动！");
    }

    @Override
    public void onDisable() {
        // 关闭所有自动切换任务，释放资源
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
