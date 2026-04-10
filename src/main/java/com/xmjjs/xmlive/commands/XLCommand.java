package com.xmjjs.xmlive.commands;

import com.xmjjs.xmlive.XMLive;
import com.xmjjs.xmlive.core.RecorderBinding;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
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

        // 权限检查
        if (subCommand.equals("login") || subCommand.equals("camera") || subCommand.equals("toggle") || subCommand.equals("mode")) {
            if (!sender.hasPermission("xmlive.use")) {
                sender.sendMessage(Component.text("你没有权限使用此命令。", NamedTextColor.RED));
                return true;
            }
        } else {
            if (!sender.hasPermission("xmlive.admin")) {
                sender.sendMessage(Component.text("你需要管理员权限才能使用此命令。", NamedTextColor.RED));
                return true;
            }
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

            case "camera":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该命令只能由玩家执行。", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(Component.text("用法: /xl camera <distance|pitch> <数值>", NamedTextColor.RED));
                    return true;
                }
                handleCamera(player, args[1], args[2]);
                break;

            case "toggle":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该命令只能由玩家执行。", NamedTextColor.RED));
                    return true;
                }
                handleToggle(player);
                break;

            case "mode":
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("该命令只能由玩家执行。", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    player.sendMessage(Component.text("用法: /xl mode <velocity|packet>", NamedTextColor.RED));
                    return true;
                }
                handleMode(player, args[1]);
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

    private void handleCamera(Player player, String type, String valueStr) {
        RecorderBinding binding = plugin.getLiveCore().getBinding(player);
        if (binding == null) {
            player.sendMessage(Component.text("你当前没有绑定目标，无法调整镜头参数。", NamedTextColor.RED));
            return;
        }
        try {
            double val = Double.parseDouble(valueStr);
            if (type.equalsIgnoreCase("distance")) {
                binding.setCustomDistance(val);
                player.sendMessage(Component.text("镜头距离已设置为 " + val, NamedTextColor.GREEN));
            } else if (type.equalsIgnoreCase("pitch")) {
                binding.setCustomPitch(val);
                player.sendMessage(Component.text("镜头俯角已设置为 " + val, NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("无效类型，请使用 distance 或 pitch", NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("请输入有效的数字", NamedTextColor.RED));
        }
    }

    private void handleToggle(Player player) {
        RecorderBinding binding = plugin.getLiveCore().getBinding(player);
        if (binding == null) {
            player.sendMessage(Component.text("你尚未绑定目标，无法切换录制状态。", NamedTextColor.RED));
            return;
        }
        boolean newState = !binding.isSpectatorMode();
        binding.setSpectatorMode(newState);

        if (newState) {
            binding.setPreviousGameMode(player.getGameMode());
            player.setGameMode(GameMode.SPECTATOR);
            player.setInvisible(true);
            player.setInvulnerable(true);
            player.sendMessage(Component.text("已进入录制状态：旁观模式、隐身、无敌", NamedTextColor.GREEN));
        } else {
            GameMode prev = binding.getPreviousGameMode() != null ? binding.getPreviousGameMode() : GameMode.SURVIVAL;
            player.setGameMode(prev);
            player.setInvisible(false);
            player.setInvulnerable(false);
            player.setVelocity(new Vector(0.0, 0.0, 0.0));
            player.sendMessage(Component.text("已退出录制状态", NamedTextColor.GREEN));
        }
    }

    private void handleMode(Player player, String modeArg) {
        RecorderBinding binding = plugin.getLiveCore().getBinding(player);
        if (binding == null) {
            player.sendMessage(Component.text("你尚未绑定目标，无法切换镜头模式。", NamedTextColor.RED));
            return;
        }

        int newMode;
        String modeName;
        if (modeArg.equalsIgnoreCase("velocity")) {
            newMode = RecorderBinding.MODE_VELOCITY;
            modeName = "速度跟随";
        } else if (modeArg.equalsIgnoreCase("packet")) {
            newMode = RecorderBinding.MODE_PACKET;
            modeName = "数据包控制";
        } else {
            player.sendMessage(Component.text("无效的模式，请使用 velocity 或 packet。", NamedTextColor.RED));
            return;
        }

        binding.setCameraMode(newMode);
        player.sendMessage(Component.text("镜头模式已切换为：" + modeName, NamedTextColor.GREEN));
    }

    private void handleReset(CommandSender sender) {
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
            String camMode = binding.getCameraMode() == RecorderBinding.MODE_VELOCITY ? "速度" : "数据包";
            sender.sendMessage(Component.text()
                    .append(Component.text("  " + recorderName, NamedTextColor.YELLOW))
                    .append(Component.text(" -> ", NamedTextColor.GRAY))
                    .append(Component.text(targetName, NamedTextColor.GREEN))
                    .append(Component.text(" [" + mode + "] ", NamedTextColor.AQUA))
                    .append(Component.text("镜头: " + camMode, NamedTextColor.LIGHT_PURPLE))
                    .build());
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.getConfigManager().reload();
        sender.sendMessage(Component.text("配置文件已重载。", NamedTextColor.GREEN));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("=== XMLIVE 命令帮助 ===", NamedTextColor.GOLD));
        if (sender.hasPermission("xmlive.use")) {
            sender.sendMessage(Component.text("/xl login <令牌> - 登录系统", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/xl camera <distance|pitch> <值> - 调整个人镜头参数", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/xl toggle - 切换录制状态（旁观/隐身/无敌）", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/xl mode <velocity|packet> - 切换镜头模式", NamedTextColor.YELLOW));
        }
        if (sender.hasPermission("xmlive.admin")) {
            sender.sendMessage(Component.text("/xl bind <录制者> <目标> - 手动绑定", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/xl unbind <录制者> - 解除绑定", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/xl auto <录制者> [间隔] - 自动模式", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/xl reset - 重置计时并切换目标", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/xl list - 查看录制者状态", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("/xl reload - 重载配置", NamedTextColor.YELLOW));
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("xmlive.use")) {
                subs.addAll(Arrays.asList("login", "camera", "toggle", "mode"));
            }
            if (sender.hasPermission("xmlive.admin")) {
                subs.addAll(Arrays.asList("bind", "unbind", "auto", "reset", "list", "reload"));
            }
            return subs.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if ((sub.equals("bind") || sub.equals("unbind") || sub.equals("auto")) && sender.hasPermission("xmlive.admin")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("camera") && sender.hasPermission("xmlive.use")) {
                return Arrays.asList("distance", "pitch").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            if (sub.equals("mode") && sender.hasPermission("xmlive.use")) {
                return Arrays.asList("velocity", "packet").stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("bind") && sender.hasPermission("xmlive.admin")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(s -> s.toLowerCase().startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
