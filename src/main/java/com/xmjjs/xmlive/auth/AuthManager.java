// src/main/java/com/yourpackage/xmlive/auth/AuthManager.java
package com.xmjjs.xmlive.auth;

import com.xmjjs.xmlive.XMLive;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthManager {

    private final XMLive plugin;
    // 存储已登录玩家的 UUID
    private final Map<UUID, Boolean> loggedInPlayers = new HashMap<>();

    public AuthManager(XMLive plugin) {
        this.plugin = plugin;
    }

    public boolean isLoggedIn(Player player) {
        return loggedInPlayers.getOrDefault(player.getUniqueId(), false);
    }

    public boolean login(Player player, String token) {
        Map<String, String> authMap = plugin.getConfigManager().getAuthMap();
        String expectedToken = authMap.get(player.getName());

        if (expectedToken != null && expectedToken.equals(token)) {
            loggedInPlayers.put(player.getUniqueId(), true);
            return true;
        }
        return false;
    }

    public void logout(Player player) {
        loggedInPlayers.remove(player.getUniqueId());
    }
}
