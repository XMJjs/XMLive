// src/main/java/com/yourpackage/xmlive/commands/XLCommand.java
package com.xmjjs.xmlive.commands;

import com.xmjjs.xmlive.XMLive;
import com.xmjjs.xmlive.core.RecorderBinding;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class XLCommand implements CommandExecutor, TabCompleter {

    private final XMLive plugin;

    public XLCommand(XMLive plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        // 所有命令都需要权限检查
        if (!sender.hasPermission("xmlive.use")) {
            sender.sendMessage(Component.text("你没有权限使用此命令。", NamedTextColor.RED));
            return true;
        }

        switch (subCommand) {
            case "login":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("此命令只能由玩家执行。", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /xl login <令牌>", NamedTextColor.RED));
                    return true;
                }
                handleLogin(player, args[1]);
                break;

            case "bind":
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /xl bind <录制者> <目标>", NamedTextColor.RED));
                    return true;
                }
                handleBind(sender, args[1], args[2]);
                break;

            case "unbind":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /xl unbind <录制者>", NamedTextColor.RED));
                    return true;
                }
                handleUnbind(sender, args[1]);
                break;

            case "auto":
                if (args.length < 2) {
                    sender.sendMessage(Component.text("用法: /xl auto <录制者> [间隔]", NamedTextColor.RED));
                    return true;
                }
                int interval = args.length >= 3 ? Integer.parseInt(args[2]) : plugin.getConfigManager().getDefaultInterval();
                handleAuto(sender, args[1], interval);
                break;

            case "time":
                sender.sendMessage(Component.text("此功能为自动模式下的默认切换时长，已由 auto 命令的间隔参数替代。", NamedTextColor.GRAY));
                break;

            case "reset":
                handleReset(sender);
                break;

            case "list":
                handleList(sender);
                break;

            case "reload":
                handleReload(sender);
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleLogin(Player player, String token) {
        if (plugin.getAuthManager().login(player, token)) {
            player.sendMessage(Component.text("登录成功！现在可以使用 XMLIVE 功能。", NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("登录失败：令牌无效或玩家未授权。", NamedTextColor.RED));
        }
    }

    private void handleBind(CommandSender sender, String recorderName, String targetName) {
        Player recorder = Bukkit.getPlayer(recorderName);
        Player target = Bukkit.getPlayer(targetName);

        if (recorder == null || !recorder.isOnline()) {
            sender.sendMessage(Component.text("录制者 " + recorderName + " 不在线。", NamedTextColor.RED));
            return;
        }
        if (target == null || !target.isOnline()) {
            sender.sendMessage(Component.text("目标玩家 " + targetName + " 不在线。", NamedTextColor.RED));
            return;
        }
        if (!plugin.getAuthManager().isLoggedIn(recorder)) {
            sender.sendMessage(Component.text("录制者 " + recorderName + " 未登录，请先使用 /xl login 登录。", NamedTextColor.RED));
            return;
        }

        plugin.getLiveCore().bind(recorder, target);

        // 为目标玩家添加发光效果
        if (plugin.getConfigManager().isGlowingEnabled()) {
            target.setGlowing(true);
        }

        sender.sendMessage(Component.text("已将录制者 " + recorderName + " 绑定到目标 " + targetName, NamedTextColor.GREEN));
    }

    private void handleUnbind(CommandSender sender, String recorderName) {
        Player recorder = Bukkit.getPlayer(recorderName);

        if (recorder == null || !recorder.isOnline()) {
            sender.sendMessage(Component.text("录制者 " + recorderName + " 不在线。", NamedTextColor.RED));
            return;
        }

        Player target = plugin.getLiveCore().getTarget(recorder);
        if (target != null && plugin.getConfigManager().isGlowingEnabled()) {
            target.setGlowing(false);
        }

        plugin.getLiveCore().unbind(recorder);
        sender.sendMessage(Component.text("已解除录制者 " + recorderName + " 的绑定。", NamedTextColor.GREEN));
    }

    private void handleAuto(CommandSender sender, String recorderName, int interval) {
        Player recorder = Bukkit.getPlayer(recorderName);

        if (recorder == null || !recorder.isOnline()) {
            sender.sendMessage(Component.text("录制者 " + recorderName + " 不在线。", NamedTextColor.RED));
            return;
        }
        if (!plugin.getAuthManager().isLoggedIn(recorder)) {
            sender.sendMessage(Component.text("录制者 " + recorderName + " 未登录。", NamedTextColor.RED));
            return;
        }
        if (plugin.getLiveCore().getBinding(recorder) == null) {
            sender.sendMessage(Component.text("录制者 " + recorderName + " 尚未绑定目标，请先使用 /xl bind。", NamedTextColor.RED));
            return;
        }

        plugin.getLiveCore().setAutoMode(recorder, interval);
        sender.sendMessage(Component.text("已为录制者 " + recorderName + " 开启自动模式，切换间隔: " + interval + " 秒。", NamedTextColor.GREEN));
    }

    private void handleReset(CommandSender sender) {
        // 重置所有自动模式的计时器，强制立即切换目标
        for (RecorderBinding binding : plugin.getLiveCore().getAllBindings()) {
            if (binding.isAutoMode()) {
                Player recorder = Bukkit.getPlayer(binding.getRecorderUuid());
                if (recorder != null) {
                    plugin.getLiveCore().disableAutoMode(recorder);
                    plugin.getLiveCore().setAutoMode(recorder, binding.getInterval());
                }
            }
        }
        sender.sendMessage(Component.text("已重置所有自动模式计时器。", NamedTextColor.GREEN));
    }

    private void handleList(CommandSender sender) {
        Collection<RecorderBinding> bindings = plugin.getLiveCore().getAllBindings();
        if (bindings.isEmpty()) {
            sender.sendMessage(Component.text("当前没有录制者。", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("=== XMLIVE 录制者列表 ===", NamedTextColor.GOLD));
        for (RecorderBinding binding : bindings) {
            Player recorder = Bukkit.getPlayer(binding.getRecorderUuid());
            Player target = Bukkit.getPlayer(binding.getTargetUuid());
            String recorderName = recorder != null ? recorder.getName() : "离线";
            String targetName = target != null ? target.getName() : "离线";
            String mode = binding.isAutoMode() ? "自动 (" + binding.getInterval() + "s)" : "手动";

            sender.sendMessage(Component.text()
                    .append(Component.text("  " + recorderName, NamedTextColor.YELLOW))
                    .append(Component.text(" -> ", NamedTextColor.GRAY))
                    .append(Component.text(targetName, NamedTextColor.GREEN))
                    .append(Component.text(" [" + mode + "]", NamedTextColor.AQUA))
                    .build());
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        sender.sendMessage(Component.text("配置文件已重载。", NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== XMLIVE 命令帮助 ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/xl login <令牌> - 登录系统", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/xl bind <录制者> <目标> - 手动绑定", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/xl unbind <录制者> - 解除绑定", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/xl auto <录制者> [间隔] - 自动模式", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/xl reset - 重置计时并切换目标", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/xl list - 查看录制者状态", NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("/xl reload - 重载配置", NamedTextColor.YELLOW));
    }

    // Tab 补全实现
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("login", "bind", "unbind", "auto", "reset", "list", "reload");
            return subCommands.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("bind") || sub.equals("unbind") || sub.equals("auto")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("bind")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
