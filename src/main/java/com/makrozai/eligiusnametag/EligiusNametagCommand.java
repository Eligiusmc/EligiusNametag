package com.makrozai.eligiusnametag;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import com.makrozai.eligiusnametag.domain.service.NametagService;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.List;

public class EligiusNametagCommand {
    private static EligiusNametag plugin;
    private static NametagService service;

    public static void createCommand(EligiusNametag eligiusPlugin, NametagService nametagService, Commands commands) {
        plugin = eligiusPlugin;
        service = nametagService;

        LiteralArgumentBuilder<CommandSourceStack> top = Commands.literal("eligiusnametag");

        top.then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("eligiusnametag.admin"))
                .executes(EligiusNametagCommand::reloadNode));

        top.then(Commands.literal("me")
                .requires(source -> source.getSender().hasPermission("eligiusnametag.viewself"))
                .executes(EligiusNametagCommand::meNode));

        top.then(Commands.literal("help")
                .executes(EligiusNametagCommand::helpNode));
                
        top.then(Commands.literal("lang")
                .requires(source -> source.getSender().hasPermission("eligiusnametag.admin"))
                .executes(c -> {
                    c.getSource().getSender().sendMessage(Component.text("Usage: /eltag lang <language>", NamedTextColor.RED));
                    return Command.SINGLE_SUCCESS;
                })
                .then(Commands.argument("language", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(EligiusNametagCommand::langNode)));
                        
        top.then(Commands.literal("pets")
                .requires(source -> source.getSender().hasPermission("eligiusnametag.admin"))
                .executes(EligiusNametagCommand::petsToggleNode));

        top.executes(EligiusNametagCommand::rootNode);

        List<String> aliases = plugin.getConfigAdapter().getCommandAliases();
        
        commands.register(
                top.build(),
                "EligiusNametag command",
                aliases
        );
    }

    private static int rootNode(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        Component msg = Component.text("EligiusNametag v" + plugin.getPluginMeta().getVersion(), NamedTextColor.GREEN);
        sender.sendMessage(msg);
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadNode(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        plugin.reloadPlugin();
        
        String msgStr = plugin.getConfigAdapter().getMessage("reloaded");
        if (msgStr == null || msgStr.isEmpty()) msgStr = "<green>Config reloaded successfully.";
        sender.sendMessage(MiniMessage.miniMessage().deserialize(msgStr));
        
        return Command.SINGLE_SUCCESS;
    }

    private static int meNode(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
            return Command.SINGLE_SUCCESS;
        }
        
        org.bukkit.entity.Player player = (org.bukkit.entity.Player) sender;
        boolean nowEnabled = service.toggleSelfView(player.getUniqueId());
        
        String msgStr;
        if (nowEnabled) {
            msgStr = plugin.getConfigAdapter().getMessage("toggled_self_on");
            if (msgStr == null || msgStr.isEmpty()) msgStr = "<green>You can now see your own nametag.";
        } else {
            msgStr = plugin.getConfigAdapter().getMessage("toggled_self_off");
            if (msgStr == null || msgStr.isEmpty()) msgStr = "<red>You can no longer see your own nametag.";
        }
        
        player.sendMessage(MiniMessage.miniMessage().deserialize(msgStr));
        return Command.SINGLE_SUCCESS;
    }

    private static int helpNode(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        
        String[] helpKeys = {"help_header", "help_me", "help_lang", "help_pets", "help_reload", "help_footer"};
        for (String key : helpKeys) {
            String msg = plugin.getConfigAdapter().getMessage(key);
            if (msg != null && !msg.isEmpty()) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize(msg));
            }
        }
        
        return Command.SINGLE_SUCCESS;
    }

    private static int langNode(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        String lang = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "language");
        
        if (!plugin.getConfigAdapter().hasLanguage(lang)) {
            String errorMsg = plugin.getConfigAdapter().getMessage("lang_not_found");
            if (errorMsg == null || errorMsg.isEmpty()) errorMsg = "<red>Language file not found.";
            errorMsg = errorMsg.replace("{lang}", lang);
            sender.sendMessage(MiniMessage.miniMessage().deserialize(errorMsg));
            return Command.SINGLE_SUCCESS;
        }
        
        plugin.getConfigAdapter().setLanguage(lang);
        plugin.reloadPlugin();
        
        String msg = plugin.getConfigAdapter().getMessage("lang_changed");
        if (msg == null || msg.isEmpty()) msg = "<green>Language changed.";
        msg = msg.replace("{lang}", lang);
        sender.sendMessage(MiniMessage.miniMessage().deserialize(msg));
        
        return Command.SINGLE_SUCCESS;
    }
    
    private static int petsToggleNode(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        
        boolean currentState = plugin.getConfigAdapter().isTamedMobsEnabled();
        boolean newState = !currentState;
        
        plugin.getConfigAdapter().setPetsEnabled(newState);
        plugin.reloadPlugin();
        
        String msgStr;
        if (newState) {
            msgStr = plugin.getConfigAdapter().getMessage("pets_enabled");
            if (msgStr == null || msgStr.isEmpty()) msgStr = "<green>Pets enabled.";
        } else {
            msgStr = plugin.getConfigAdapter().getMessage("pets_disabled");
            if (msgStr == null || msgStr.isEmpty()) msgStr = "<red>Pets disabled.";
        }
        
        sender.sendMessage(MiniMessage.miniMessage().deserialize(msgStr));
        
        return Command.SINGLE_SUCCESS;
    }
}
