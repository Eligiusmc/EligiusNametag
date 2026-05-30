package com.makrozai.eligiusnametag;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

public class StartupLogger {

    public static void printLogo(String version, String platform, String storage) {
        String[] lines = {
            "<gray>------------------------------------------------------------",
            "<gradient:#9b59b6:#8e44ad>                  <bold>ELIGIUS MC</bold></gradient>",
            "<gradient:#9b59b6:#8e44ad>      /\\_/\\       </gradient>",
            "<gradient:#9b59b6:#8e44ad>     ( o.o )      </gradient><aqua>Nametag Plugin v" + version + "</aqua>",
            "<gradient:#9b59b6:#8e44ad>      > ^ <       </gradient><gray>Platform:</gray> <white>" + platform + "</white> <gray>|</gray> <gray>Storage:</gray> <white>" + storage + "</white>",
            "<gradient:#9b59b6:#8e44ad>                  </gradient><dark_gray>GitHub:</dark_gray> <aqua>https://github.com/Eligiusmc</aqua>",
            "<gray>------------------------------------------------------------"
        };

        for (String line : lines) {
            Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize(line));
        }
    }

    public static void printStep(String step) {
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<gray>[<gradient:#9b59b6:#8e44ad>EligiusNametag</gradient>]</gray> <white>" + step + "</white>"));
    }

    public static void printError(String error) {
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<gray>[<gradient:#9b59b6:#8e44ad>EligiusNametag</gradient>]</gray> <red>[ERROR] " + error + "</red>"));
    }

    public static void printSuccess(long ms) {
        Bukkit.getConsoleSender().sendMessage(MiniMessage.miniMessage().deserialize("<gray>[<gradient:#9b59b6:#8e44ad>EligiusNametag</gradient>]</gray> <green>Successfully enabled. (took " + ms + "ms)</green>"));
    }
}
