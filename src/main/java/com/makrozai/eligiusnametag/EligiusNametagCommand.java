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

        top.executes(EligiusNametagCommand::rootNode);

        List<String> aliases = plugin.getConfigAdapter().getCommandAliases();
        for (String alias : aliases) {
            LiteralArgumentBuilder<CommandSourceStack> aliasTop = Commands.literal(alias);
            aliasTop.then(top.getArguments().stream().findFirst().get()); // It's better to just redirect or reconstruct
            
            // To properly duplicate the command tree for aliases, we can just redirect to the main node
            // But Paper allows registering aliases directly
        }
        
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
        sender.sendMessage(Component.text("EligiusNametag config reloaded successfully.", NamedTextColor.GREEN));
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
            if (msgStr == null || msgStr.isEmpty()) msgStr = "<green>Ahora puedes ver tu propio nametag.";
        } else {
            msgStr = plugin.getConfigAdapter().getMessage("toggled_self_off");
            if (msgStr == null || msgStr.isEmpty()) msgStr = "<red>Ya no puedes ver tu propio nametag.";
        }
        
        player.sendMessage(MiniMessage.miniMessage().deserialize(msgStr));
        return Command.SINGLE_SUCCESS;
    }
}
