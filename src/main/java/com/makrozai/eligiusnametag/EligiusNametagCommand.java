package com.makrozai.eligiusnametag;

import com.makrozai.eligiusnametag.domain.service.NametagService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.Bukkit;

public class EligiusNametagCommand implements CommandExecutor, TabCompleter {
    private final EligiusNametag plugin;
    private final NametagService service;

    public EligiusNametagCommand(EligiusNametag plugin, NametagService service) {
        this.plugin = plugin;
        this.service = service;
    }

    private void sendMessage(CommandSender sender, Component component) {
        try {
            sender.sendMessage(component);
        } catch (NoSuchMethodError e) {
            String legacyString = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build().serialize(component);
            sender.sendMessage(legacyString);
        }
    }

    private void sendMessage(CommandSender sender, String messageStr) {
        if (messageStr == null || messageStr.isEmpty()) return;
        sendMessage(sender, MiniMessage.miniMessage().deserialize(messageStr));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendMessage(sender, Component.text("EligiusNametag v" + plugin.getPluginMeta().getVersion(), NamedTextColor.GREEN));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                if (!sender.hasPermission("eligiusnametag.admin")) {
                    sendMessage(sender, Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                plugin.reloadPlugin();
                String reloadMsg = plugin.getConfigAdapter().getMessage("reloaded");
                if (reloadMsg == null || reloadMsg.isEmpty()) reloadMsg = "<green>Config reloaded successfully.";
                sendMessage(sender, reloadMsg);
                break;

            case "me":
                if (!sender.hasPermission("eligiusnametag.viewself")) {
                    sendMessage(sender, Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                if (!(sender instanceof Player)) {
                    sendMessage(sender, Component.text("Only players can use this command.", NamedTextColor.RED));
                    return true;
                }
                Player player = (Player) sender;
                boolean nowEnabled = service.toggleSelfView(player.getUniqueId());

                String msgStr;
                if (nowEnabled) {
                    msgStr = plugin.getConfigAdapter().getMessage("toggled_self_on");
                    if (msgStr == null || msgStr.isEmpty()) msgStr = "<green>You can now see your own nametag.";
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } else {
                    msgStr = plugin.getConfigAdapter().getMessage("toggled_self_off");
                    if (msgStr == null || msgStr.isEmpty()) msgStr = "<red>You can no longer see your own nametag.";
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
                }
                sendMessage(player, msgStr);
                break;

            case "help":
                if (!sender.hasPermission("eligiusnametag.admin")) {
                    sendMessage(sender, Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                String[] helpKeys = {"help_header", "help_me", "help_lang", "help_pets", "help_reload", "help_footer"};
                for (String key : helpKeys) {
                    sendMessage(sender, plugin.getConfigAdapter().getMessage(key));
                }
                break;

            case "lang":
                if (!sender.hasPermission("eligiusnametag.admin")) {
                    sendMessage(sender, Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                if (args.length < 2) {
                    sendMessage(sender, Component.text("Usage: /enametag lang <language>", NamedTextColor.RED));
                    return true;
                }
                String lang = args[1];
                if (!plugin.getConfigAdapter().hasLanguage(lang)) {
                    String errorMsg = plugin.getConfigAdapter().getMessage("lang_not_found");
                    if (errorMsg == null || errorMsg.isEmpty()) errorMsg = "<red>Language file not found.";
                    errorMsg = errorMsg.replace("{lang}", lang);
                    sendMessage(sender, errorMsg);
                    return true;
                }

                plugin.getConfigAdapter().setLanguage(lang);
                plugin.reloadPlugin();

                String langMsg = plugin.getConfigAdapter().getMessage("lang_changed");
                if (langMsg == null || langMsg.isEmpty()) langMsg = "<green>Language changed.";
                langMsg = langMsg.replace("{lang}", lang);
                sendMessage(sender, langMsg);
                break;

            case "pets":
                if (!sender.hasPermission("eligiusnametag.admin")) {
                    sendMessage(sender, Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                boolean currentState = plugin.getConfigAdapter().isTamedMobsEnabled();
                boolean newState = !currentState;

                plugin.getConfigAdapter().setPetsEnabled(newState);
                plugin.reloadPlugin();

                String petsMsg;
                if (newState) {
                    petsMsg = plugin.getConfigAdapter().getMessage("pets_enabled");
                    if (petsMsg == null || petsMsg.isEmpty()) petsMsg = "<green>Pets enabled.";
                } else {
                    petsMsg = plugin.getConfigAdapter().getMessage("pets_disabled");
                    if (petsMsg == null || petsMsg.isEmpty()) petsMsg = "<red>Pets disabled.";
                }
                sendMessage(sender, petsMsg);
                break;
                
            case "debug":
                if (!sender.hasPermission("eligiusnametag.admin")) {
                    sendMessage(sender, Component.text("No permission.", NamedTextColor.RED));
                    return true;
                }
                sendMessage(sender, Component.text("Generating debug report...", NamedTextColor.YELLOW));
                
                File debugDir = new File(plugin.getDataFolder(), "debug");
                if (!debugDir.exists()) debugDir.mkdirs();
                
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                File debugFile = new File(debugDir, "report_" + timestamp + ".txt");
                
                try (PrintWriter out = new PrintWriter(new FileWriter(debugFile))) {
                    out.println("=== EligiusNametag Debug Report ===");
                    out.println("Generated at: " + timestamp);
                    out.println();
                    
                    out.println("--- Plugin Metrics ---");
                    out.println("Version: " + plugin.getPluginMeta().getVersion());
                    out.println("Active TextDisplays: " + service.getActiveEntityCount());
                    out.println("Tracking Loop Latency: " + service.getLastTickDuration() + "ms");
                    out.println("Players with ViewSelf: " + service.getSelfViewersCount());
                    out.println();
                    
                    out.println("--- Server Metrics ---");
                    out.println("Server Version: " + Bukkit.getVersion());
                    out.println("Bukkit Version: " + Bukkit.getBukkitVersion());
                    out.println("Java Version: " + System.getProperty("java.version"));
                    out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
                    
                    Runtime runtime = Runtime.getRuntime();
                    long maxMemory = runtime.maxMemory() / 1024 / 1024;
                    long allocatedMemory = runtime.totalMemory() / 1024 / 1024;
                    long freeMemory = runtime.freeMemory() / 1024 / 1024;
                    out.println("Memory: " + (allocatedMemory - freeMemory) + "MB / " + maxMemory + "MB");
                    
                    out.println();
                    out.println("--- Installed Plugins ---");
                    Arrays.stream(Bukkit.getPluginManager().getPlugins())
                        .forEach(p -> out.println("- " + p.getName() + " v" + p.getPluginMeta().getVersion()));
                } catch (Exception e) {
                    plugin.getLogger().severe("Failed to generate debug report: " + e.getMessage());
                    sendMessage(sender, Component.text("Failed to generate debug report. Check console.", NamedTextColor.RED));
                    return true;
                }
                
                sendMessage(sender, Component.text("Debug report saved to plugins/EligiusNametag/debug/" + debugFile.getName(), NamedTextColor.GREEN));
                break;

            default:
                sendMessage(sender, Component.text("Unknown subcommand. Type /enametag help", NamedTextColor.RED));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.hasPermission("eligiusnametag.admin")) {
                completions.addAll(Arrays.asList("reload", "help", "lang", "pets", "debug"));
            }
            if (sender.hasPermission("eligiusnametag.viewself")) {
                completions.add("me");
            }
            // Filter by prefix
            String prefix = args[0].toLowerCase();
            completions.removeIf(s -> !s.startsWith(prefix));
        }
        return completions;
    }
}
