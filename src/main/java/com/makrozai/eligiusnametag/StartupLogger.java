package com.makrozai.eligiusnametag;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

public class StartupLogger {

    private static void log(String miniMessageString) {
        String legacy = LegacyComponentSerializer.builder().hexColors().useUnusualXRepeatedCharacterHexFormat().build()
                .serialize(MiniMessage.miniMessage().deserialize(miniMessageString));
        Bukkit.getConsoleSender().sendMessage(legacy);
    }

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
            log(line);
        }
    }

    public static void printStep(String step) {
        log("<gray>[<gradient:#9b59b6:#8e44ad>EligiusNametag</gradient>]</gray> <white>" + step + "</white>");
    }

    public static void printError(String error) {
        log("<gray>[<gradient:#9b59b6:#8e44ad>EligiusNametag</gradient>]</gray> <red>[ERROR] " + error + "</red>");
    }

    public static void printSuccess(long ms) {
        log("<gray>[<gradient:#9b59b6:#8e44ad>EligiusNametag</gradient>]</gray> <green>Successfully enabled. (took " + ms + "ms)</green>");
    }

    public static void printUpToDate(String version) {
        log("<gray>[<gradient:#9b59b6:#8e44ad>EligiusNametag</gradient>]</gray> <green>✓ You are running the latest version (v" + version + ").</green>");
    }

    public static void printUpdateNotice(String newVersion, String url) {
        log("<gray>[<gradient:#9b59b6:#8e44ad>EligiusNametag</gradient>]</gray> <yellow>⚠️ A new update (v" + newVersion + ") is available!</yellow>");
        log("<gray>[<gradient:#9b59b6:#8e44ad>EligiusNametag</gradient>]</gray> <dark_gray>🔗 Download it here:</dark_gray> <aqua><underlined>" + url + "</underlined></aqua>");
    }
}
