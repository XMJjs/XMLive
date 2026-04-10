// src/main/java/com/XMLIVE/xmlive/listeners/RecorderRestrictListener.java
package com.XMLIVE.xmllive.listeners;

import com.XMLIVE.xmllive.XMLive;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class RecorderRestrictListener implements Listener {

    private final XMLive plugin;

    public RecorderRestrictListener(XMLive plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // 如果玩家是录制者，阻止打开背包/容器
            if (plugin.getLiveCore().getBinding(player) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        // 如果玩家是录制者，阻止丢弃物品
        if (plugin.getLiveCore().getBinding(player) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        // 如果玩家是录制者，阻止与方块/物品交互（可根据需要细化）
        if (plugin.getLiveCore().getBinding(player) != null) {
            event.setCancelled(true);
        }
    }
}
