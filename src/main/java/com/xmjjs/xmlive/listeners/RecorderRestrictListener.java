package com.xmjjs.xmlive.listeners;

import com.xmjjs.xmlive.XMLive;
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
            if (plugin.getLiveCore().getBinding(player) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getLiveCore().getBinding(player) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (plugin.getLiveCore().getBinding(player) != null) {
            event.setCancelled(true);
        }
    }
}
